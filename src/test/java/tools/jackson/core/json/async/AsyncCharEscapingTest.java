package tools.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
class AsyncCharEscapingTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void missingLinefeedEscaping() throws Exception
    {
        byte[] doc = _jsonDoc(a2q("['Linefeed: \n.']"));
        _testMissingLinefeedEscaping(doc, 0, 99);
        _testMissingLinefeedEscaping(doc, 0, 5);
        _testMissingLinefeedEscaping(doc, 0, 3);
        _testMissingLinefeedEscaping(doc, 0, 2);
        _testMissingLinefeedEscaping(doc, 0, 1);

        _testMissingLinefeedEscaping(doc, 1, 99);
        _testMissingLinefeedEscaping(doc, 1, 3);
        _testMissingLinefeedEscaping(doc, 1, 1);
    }

    private void _testMissingLinefeedEscaping(byte[] doc, int offset, int readSize)
            throws Exception
    {
        AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        try {
            // This may or may not trigger exception
            JsonToken t = r.nextToken();
            assertToken(JsonToken.VALUE_STRING, t);
            fail("Expected an exception for un-escaped linefeed in string value");
        } catch (StreamReadException jex) {
            verifyException(jex, "has to be escaped");
        }
        r.close();
    }

    @Test
    void simpleEscaping() throws Exception
    {
        _testSimpleEscaping(0, 99);
        _testSimpleEscaping(0, 5);
        _testSimpleEscaping(0, 3);
        _testSimpleEscaping(0, 2);
        _testSimpleEscaping(0, 1);

        _testSimpleEscaping(1, 99);
        _testSimpleEscaping(1, 3);
        _testSimpleEscaping(1, 1);
    }

    private void _testSimpleEscaping(int offset, int readSize) throws Exception
    {
        byte[] doc = _jsonDoc(a2q("['LF=\\n']"));

        AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals("LF=\n", r.currentText());
        r.close();

        // Note: must split Strings, so that javac won't try to handle
        // escape and inline null char
        doc = _jsonDoc(a2q("['NULL:\\u0000!']"));
        r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals("NULL:\0!", r.currentText());
        r.close();

        // Then just a single char escaping
        doc = _jsonDoc(a2q("['\\u0123']"));
        r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals("\u0123", r.currentText());
        r.close();

        // And then double sequence
        doc = _jsonDoc(a2q("['\\u0041\\u0043']"));
        r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals("AC", r.currentText());
        r.close();
    }

    @Test
    void test8DigitSequence() throws Exception
    {
        String DOC = "[\"\\u00411234\"]";
        AsyncReaderWrapper r = asyncForBytes(JSON_F, 1, _jsonDoc(DOC), 1);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_STRING, r.nextToken());
        assertEquals("A1234", r.currentText());
        r.close();
    }
}
