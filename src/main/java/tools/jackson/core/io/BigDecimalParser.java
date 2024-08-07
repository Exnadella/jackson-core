package tools.jackson.core.io;

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;

import java.math.BigDecimal;

/**
 * Internal Jackson Helper class used to implement more optimized parsing of
 * {@link BigDecimal} for REALLY big values (over 500 characters).
 *<p>
 * This class is not meant to be used directly. It is designed to be used by Jackson JSON parsers (and parsers
 * for other Jackson supported data formats). The parsers check for invalid characters and the length of the number.
 * Without these checks, this parser is susceptible to performing badly with invalid inputs. If you need to parse
 * numbers directly, please use JavaBigDecimalParser in <a href="https://github.com/wrandelshofer/FastDoubleParser">fastdoubleparser</a>
 * instead.
 *</p>
 *<p>
 * Based on ideas from this
 * <a href="https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f">this
 * git commit</a>.
 */
public final class BigDecimalParser
{
    final static int MAX_CHARS_TO_REPORT = 1000;

    private BigDecimalParser() {}

    /**
     * Internal Jackson method. Please do not use.
     *<p>
     * Note: Caller MUST pre-validate that given String represents a valid representation
     * of {@link BigDecimal} value: parsers in {@code jackson-core} do that; other
     * code must do the same.
     *
     * @param valueStr Value to parse
     *
     * @return BigDecimal value
     *
     * @throws NumberFormatException for decoding failures
     */
    public static BigDecimal parse(String valueStr) {
        return parse(valueStr.toCharArray());
    }

    /**
     * Internal Jackson method. Please do not use.
     *<p>
     * Note: Caller MUST pre-validate that given String represents a valid representation
     * of {@link BigDecimal} value: parsers in {@code jackson-core} do that; other
     * code must do the same.
     *
     * @param chars Buffer that contains value to parse
     * @param off Offset of the first character to decode
     * @param len Length of value to parse in buffer
     *
     * @return BigDecimal value
     *
     * @throws NumberFormatException for decoding failures
     */
    public static BigDecimal parse(final char[] chars, final int off, final int len) {
        try {
            if (len < 500) {
                return new BigDecimal(chars, off, len);
            }
            return JavaBigDecimalParser.parseBigDecimal(chars, off, len);

        // 20-Aug-2022, tatu: Although "new BigDecimal(...)" only throws NumberFormatException
        //    operations by "parseBigDecimal()" can throw "ArithmeticException", so handle both:
        } catch (ArithmeticException | NumberFormatException e) {
            throw _parseFailure(e, new String(chars, off, len));
        }
    }
    
    /**
     * Internal Jackson method. Please do not use.
     *<p>
     * Note: Caller MUST pre-validate that given String represents a valid representation
     * of {@link BigDecimal} value: parsers in {@code jackson-core} do that; other
     * code must do the same.
     *
     * @param chars Value to parse
     * @return BigDecimal value
     * @throws NumberFormatException for decoding failures
     */
    public static BigDecimal parse(char[] chars) {
        return parse(chars, 0, chars.length);
    }

    /**
     * Internal Jackson method. Please do not use.
     *<p>
     * Note: Caller MUST pre-validate that given String represents a valid representation
     * of {@link BigDecimal} value: parsers in {@code jackson-core} do that; other
     * code must do the same.
     *
     * @param valueStr Value to parse
     *
     * @return BigDecimal value
     *
     * @throws NumberFormatException for decoding failures
     */
    public static BigDecimal parseWithFastParser(final String valueStr) {
        try {
            return JavaBigDecimalParser.parseBigDecimal(valueStr);
        } catch (ArithmeticException | NumberFormatException e) {
            throw _parseFailure(e, valueStr);
        }
    }

    /**
     * Internal Jackson method. Please do not use.
     *<p>
     * Note: Caller MUST pre-validate that given String represents a valid representation
     * of {@link BigDecimal} value: parsers in {@code jackson-core} do that; other
     * code must do the same.
     *
     * @return BigDecimal value
     *
     * @throws NumberFormatException for decoding failures
     */
    public static BigDecimal parseWithFastParser(final char[] ch, final int off, final int len) {
        try {
            return JavaBigDecimalParser.parseBigDecimal(ch, off, len);
        } catch (ArithmeticException | NumberFormatException e) {
            throw _parseFailure(e, new String(ch, off, len));
        }
    }

    private static NumberFormatException _parseFailure(Exception e, String fullValue) {
        String desc = e.getMessage();
        // 05-Feb-2021, tatu: Alas, JDK mostly has null message so:
        if (desc == null) {
            desc = "Not a valid number representation";
        }
        String valueToReport = _getValueDesc(fullValue);
        return new NumberFormatException("Value " + valueToReport
                + " can not be deserialized as `java.math.BigDecimal`, reason: " + desc);
    }

    private static String _getValueDesc(String fullValue) {
        final int len = fullValue.length();
        if (len <= MAX_CHARS_TO_REPORT) {
            return String.format("\"%s\"", fullValue);
        }
        return String.format("\"%s\" (truncated to %d chars (from %d))",
                fullValue.substring(0, MAX_CHARS_TO_REPORT),
                MAX_CHARS_TO_REPORT, len);
    }

}
