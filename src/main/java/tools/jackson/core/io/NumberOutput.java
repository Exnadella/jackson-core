package tools.jackson.core.io;

import tools.jackson.core.io.schubfach.DoubleToDecimal;
import tools.jackson.core.io.schubfach.FloatToDecimal;

public final class NumberOutput
{
    private static int MILLION = 1000000;
    private static int BILLION = 1000000000;
    private static long BILLION_L = 1000000000L;

    private static long MIN_INT_AS_LONG = Integer.MIN_VALUE;
    private static long MAX_INT_AS_LONG = Integer.MAX_VALUE;

    final static String SMALLEST_INT = String.valueOf(Integer.MIN_VALUE);
    final static String SMALLEST_LONG = String.valueOf(Long.MIN_VALUE);

    /**
     * Encoded representations of 3-decimal-digit indexed values, where
     * 3 LSB are ascii characters
     */
    private final static int[] TRIPLET_TO_CHARS = new int[1000];

    static {
        // Let's fill it with NULLs for ignorable leading digits,
        // and digit chars for others
        int fullIx = 0;
        for (int i1 = 0; i1 < 10; ++i1) {
            for (int i2 = 0; i2 < 10; ++i2) {
                for (int i3 = 0; i3 < 10; ++i3) {
                    int enc = ((i1 + '0') << 16)
                            | ((i2 + '0') << 8)
                            | (i3 + '0');
                    TRIPLET_TO_CHARS[fullIx++] = enc;
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Efficient serialization methods using raw buffers
    /**********************************************************************
     */

    /**
     * Method for appending value of given {@code int} value into
     * specified {@code char[]}.
     *<p>
     * NOTE: caller must guarantee that the output buffer has enough room
     * for String representation of the value.
     *
     * @param v Value to append to buffer
     * @param b Buffer to append value to: caller must guarantee there is enough room
     * @param off Offset within output buffer ({@code b}) to append number at
     *
     * @return Offset within buffer after outputting {@code int}
     */
    public static int outputInt(int v, char[] b, int off)
    {
        if (v < 0) {
            if (v == Integer.MIN_VALUE) {
                // Special case: no matching positive value within range;
                // let's then "upgrade" to long and output as such.
                return _outputSmallestI(b, off);
            }
            b[off++] = '-';
            v = -v;
        }

        if (v < MILLION) { // at most 2 triplets...
            if (v < 10) {
                b[off] = (char) ('0' + v);
                return off+1;
            }
            if (v < 1000) {
                return _leading3(v, b, off);
            }
            int thousands = divBy1000(v);
            v -= (thousands * 1000); // == value % 1000
            off = _leading3(thousands, b, off);
            off = _full3(v, b, off);
            return off;
        }

        // ok, all 3 triplets included
        /* Let's first hand possible billions separately before
         * handling 3 triplets. This is possible since we know we
         * can have at most '2' as billion count.
         */
        if (v >= BILLION) {
            v -= BILLION;
            if (v >= BILLION) {
                v -= BILLION;
                b[off++] = '2';
            } else {
                b[off++] = '1';
            }
            return _outputFullBillion(v, b, off);
        }
        int newValue = divBy1000(v);
        int ones = (v - (newValue * 1000)); // == value % 1000
        v = newValue;
        newValue = divBy1000(newValue);
        int thousands = (v - (newValue * 1000));

        off = _leading3(newValue, b, off);
        off = _full3(thousands, b, off);
        return _full3(ones, b, off);
    }

    public static int outputInt(int v, byte[] b, int off)
    {
        if (v < 0) {
            if (v == Integer.MIN_VALUE) {
                return _outputSmallestI(b, off);
            }
            b[off++] = '-';
            v = -v;
        }

        if (v < MILLION) { // at most 2 triplets...
            if (v < 1000) {
                if (v < 10) {
                    b[off++] = (byte) ('0' + v);
                } else {
                    off = _leading3(v, b, off);
                }
            } else {
                int thousands = divBy1000(v);
                v -= (thousands * 1000); // == value % 1000
                off = _leading3(thousands, b, off);
                off = _full3(v, b, off);
            }
            return off;
        }
        if (v >= BILLION) {
            v -= BILLION;
            if (v >= BILLION) {
                v -= BILLION;
                b[off++] = '2';
            } else {
                b[off++] = '1';
            }
            return _outputFullBillion(v, b, off);
        }
        int newValue = divBy1000(v);
        int ones = (v - (newValue * 1000)); // == value % 1000
        v = newValue;
        newValue = divBy1000(newValue);
        int thousands = (v - (newValue * 1000));
        off = _leading3(newValue, b, off);
        off = _full3(thousands, b, off);
        return _full3(ones, b, off);
    }

    /**
     * Method for appending value of given {@code long} value into
     * specified {@code char[]}.
     *<p>
     * NOTE: caller must guarantee that the output buffer has enough room
     * for String representation of the value.
     *
     * @param v Value to append to buffer
     * @param b Buffer to append value to: caller must guarantee there is enough room
     * @param off Offset within output buffer ({@code b}) to append number at
     *
     * @return Offset within buffer after outputting {@code long}
     */
    public static int outputLong(long v, char[] b, int off)
    {
        // First: does it actually fit in an int?
        if (v < 0L) {
            if (v > MIN_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
            if (v == Long.MIN_VALUE) {
                return _outputSmallestL(b, off);
            }
            b[off++] = '-';
            v = -v;
        } else {
            if (v <= MAX_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
        }

        // Ok, let's separate last 9 digits (3 x full sets of 3)
        long upper = v / BILLION_L;
        v -= (upper * BILLION_L);

        // two integers?
        if (upper < BILLION_L) {
            off = _outputUptoBillion((int) upper, b, off);
        } else {
            // no, two ints and bits; hi may be about 16 or so
            long hi = upper / BILLION_L;
            upper -= (hi * BILLION_L);
            off = _leading3((int) hi, b, off);
            off = _outputFullBillion((int) upper, b, off);
        }
        return _outputFullBillion((int) v, b, off);
    }

    public static int outputLong(long v, byte[] b, int off)
    {
        if (v < 0L) {
            if (v > MIN_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
            if (v == Long.MIN_VALUE) {
                return _outputSmallestL(b, off);
            }
            b[off++] = '-';
            v = -v;
        } else {
            if (v <= MAX_INT_AS_LONG) {
                return outputInt((int) v, b, off);
            }
        }

        // Ok, let's separate last 9 digits (3 x full sets of 3)
        long upper = v / BILLION_L;
        v -= (upper * BILLION_L);

        // two integers?
        if (upper < BILLION_L) {
            off = _outputUptoBillion((int) upper, b, off);
        } else {
            // no, two ints and bits; hi may be about 16 or so
            long hi = upper / BILLION_L;
            upper -= (hi * BILLION_L);
            off = _leading3((int) hi, b, off);
            off = _outputFullBillion((int) upper, b, off);
        }
        return _outputFullBillion((int) v, b, off);
    }

    /**
     * Optimized code for integer division by 1000; typically 50% higher
     * throughput for calculation
     *
     * @since 2.17
     */
    static int divBy1000(int number) {
        return (int) (number * 274_877_907L >>> 38);
    }

    /*
    /**********************************************************************
    /* Convenience serialization methods
    /**********************************************************************
     */

    /**
     * @param v double
     * @return double as a string
     */
    public static String toString(final double v) {
        return toString(v, false);
    }

    /**
     * @param v double
     * @param useFastWriter whether to use Schubfach algorithm to write output (default false)
     * @return double as a string
     */
    public static String toString(final double v, final boolean useFastWriter) {
        return useFastWriter ? DoubleToDecimal.toString(v) : Double.toString(v);
    }

    /**
     * @param v float
     * @return float as a string
     */
    public static String toString(final float v) {
        return toString(v, false);
    }

    /**
     * @param v float
     * @param useFastWriter whether to use Schubfach algorithm to write output (default false)
     * @return float as a string
     */
    public static String toString(final float v, final boolean useFastWriter) {
        return useFastWriter ? FloatToDecimal.toString(v) : Float.toString(v);
    }

    /*
    /**********************************************************************
    /* Other convenience methods
    /**********************************************************************
     */

    /**
     * Helper method to verify whether given {@code double} value is finite
     * (regular rational number} or not (NaN or Infinity).
     *
     * @param value {@code double} value to check
     *
     * @return True if number is NOT finite (is Infinity or NaN); false otherwise
     */
    public static boolean notFinite(double value) {
        return !Double.isFinite(value);
    }

    /**
     * Helper method to verify whether given {@code float} value is finite
     * (regular rational number} or not (NaN or Infinity).
     *
     * @param value {@code float} value to check
     *
     * @return True if number is NOT finite (is Infinity or NaN); false otherwise
     */
    public static boolean notFinite(float value) {
        return !Float.isFinite(value);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private static int _outputUptoBillion(int v, char[] b, int off)
    {
        if (v < MILLION) { // at most 2 triplets...
            if (v < 1000) {
                return _leading3(v, b, off);
            }
            int thousands = divBy1000(v);
            int ones = v - (thousands * 1000); // == value % 1000
            return _outputUptoMillion(b, off, thousands, ones);
        }
        int thousands = divBy1000(v);
        int ones = (v - (thousands * 1000)); // == value % 1000
        int millions = divBy1000(thousands);
        thousands -= (millions * 1000);

        off = _leading3(millions, b, off);

        int enc = TRIPLET_TO_CHARS[thousands];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);

        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);

        return off;
    }

    private static int _outputFullBillion(int v, char[] b, int off)
    {
        int thousands = divBy1000(v);
        int ones = (v - (thousands * 1000)); // == value % 1000
        int millions = divBy1000(thousands);

        int enc = TRIPLET_TO_CHARS[millions];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);

        thousands -= (millions * 1000);
        enc = TRIPLET_TO_CHARS[thousands];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);

        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);

        return off;
    }

    private static int _outputUptoBillion(int v, byte[] b, int off)
    {
        if (v < MILLION) { // at most 2 triplets...
            if (v < 1000) {
                return _leading3(v, b, off);
            }
            int thousands = divBy1000(v);
            int ones = v - (thousands * 1000); // == value % 1000
            return _outputUptoMillion(b, off, thousands, ones);
        }
        int thousands = divBy1000(v);
        int ones = (v - (thousands * 1000)); // == value % 1000
        int millions = divBy1000(thousands);
        thousands -= (millions * 1000);

        off = _leading3(millions, b, off);

        int enc = TRIPLET_TO_CHARS[thousands];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;

        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;

        return off;
    }

    private static int _outputFullBillion(int v, byte[] b, int off)
    {
        int thousands = divBy1000(v);
        int ones = (v - (thousands * 1000)); // == value % 1000
        int millions = divBy1000(thousands);
        thousands -= (millions * 1000);

        int enc = TRIPLET_TO_CHARS[millions];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;

        enc = TRIPLET_TO_CHARS[thousands];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;

        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;

        return off;
    }

    private static int _outputUptoMillion(char[] b, int off, int thousands, int ones)
    {
        int enc = TRIPLET_TO_CHARS[thousands];
        if (thousands > 9) {
            if (thousands > 99) {
                b[off++] = (char) (enc >> 16);
            }
            b[off++] = (char) ((enc >> 8) & 0x7F);
        }
        b[off++] = (char) (enc & 0x7F);
        // and then full
        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);
        return off;
    }

    private static int _outputUptoMillion(byte[] b, int off, int thousands, int ones)
    {
        int enc = TRIPLET_TO_CHARS[thousands];
        if (thousands > 9) {
            if (thousands > 99) {
                b[off++] = (byte) (enc >> 16);
            }
            b[off++] = (byte) (enc >> 8);
        }
        b[off++] = (byte) enc;
        // and then full
        enc = TRIPLET_TO_CHARS[ones];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;
        return off;
    }

    private static int _leading3(int t, char[] b, int off)
    {
        int enc = TRIPLET_TO_CHARS[t];
        if (t > 9) {
            if (t > 99) {
                b[off++] = (char) (enc >> 16);
            }
            b[off++] = (char) ((enc >> 8) & 0x7F);
        }
        b[off++] = (char) (enc & 0x7F);
        return off;
    }

    private static int _leading3(int t, byte[] b, int off)
    {
        int enc = TRIPLET_TO_CHARS[t];
        if (t > 9) {
            if (t > 99) {
                b[off++] = (byte) (enc >> 16);
            }
            b[off++] = (byte) (enc >> 8);
        }
        b[off++] = (byte) enc;
        return off;
    }

    private static int _full3(int t, char[] b, int off)
    {
        int enc = TRIPLET_TO_CHARS[t];
        b[off++] = (char) (enc >> 16);
        b[off++] = (char) ((enc >> 8) & 0x7F);
        b[off++] = (char) (enc & 0x7F);
        return off;
    }

    private static int _full3(int t, byte[] b, int off)
    {
        int enc = TRIPLET_TO_CHARS[t];
        b[off++] = (byte) (enc >> 16);
        b[off++] = (byte) (enc >> 8);
        b[off++] = (byte) enc;
        return off;
    }

    // // // Special cases for where we can not flip the sign bit

    private static int _outputSmallestL(char[] b, int off)
    {
        int len = SMALLEST_LONG.length();
        SMALLEST_LONG.getChars(0, len, b, off);
        return (off + len);
    }

    private static int _outputSmallestL(byte[] b, int off)
    {
        int len = SMALLEST_LONG.length();
        for (int i = 0; i < len; ++i) {
            b[off++] = (byte) SMALLEST_LONG.charAt(i);
        }
        return off;
    }

    private static int _outputSmallestI(char[] b, int off)
    {
        int len = SMALLEST_INT.length();
        SMALLEST_INT.getChars(0, len, b, off);
        return (off + len);
    }

    private static int _outputSmallestI(byte[] b, int off)
    {
        int len = SMALLEST_INT.length();
        for (int i = 0; i < len; ++i) {
            b[off++] = (byte) SMALLEST_INT.charAt(i);
        }
        return off;
    }
}
