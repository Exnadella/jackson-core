package tools.jackson.core.json.async;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AsyncPropertyNamesTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final JsonFactory JSON_APOS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    // Mainly to test "fast" parse for shortish names
    @Test
    void simpleFieldNames() throws IOException
    {
        for (String name : new String[] { "", "a", "ab", "abc", "abcd",
                "abcd1", "abcd12", "abcd123", "abcd1234",
                "abcd1234a",  "abcd1234ab",  "abcd1234abc",  "abcd1234abcd",
                "abcd1234abcd1"
            }) {
            _testSimpleName(name);
        }
    }

    private void _testSimpleName(String fieldName) throws IOException
    {
        // use long buffer to ensure fast decoding may be used
        AsyncReaderWrapper r = asyncForBytes(JSON_F, 99,
                _jsonDoc(String.format("{\"%s\":true}                     \r", fieldName)),
                0);
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals(fieldName, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        assertNull(r.nextToken());
        JsonLocation loc = r.parser().currentLocation();
        assertEquals(2, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());
    }

    @Test
    void escapedFieldNames() throws IOException
    {
        _testEscapedNames("\\'foo\\'", "'foo'");
        _testEscapedNames("\\'foobar\\'", "'foobar'");
        _testEscapedNames("\\'foo \\u0026 bar\\'", "'foo & bar'");
        _testEscapedNames("Something \\'longer\\'?", "Something 'longer'?");
        _testEscapedNames("\\u00A7", "\u00A7");
        _testEscapedNames("\\u4567", "\u4567");
        _testEscapedNames("Unicode: \\u00A7 and \\u4567?", "Unicode: \u00A7 and \u4567?");
    }

    private void _testEscapedNames(String nameEncoded, String nameExp) throws IOException
    {
        byte[] doc;
        StringWriter w;

        nameEncoded = a2q(nameEncoded);
        nameExp = a2q(nameExp);

        w = new StringWriter();
        w.append("{\"");
        w.append(nameEncoded);
        w.append("\":true}");
        doc = w.toString().getBytes("UTF-8");

        _testEscapedFieldNames(doc, nameExp, 0, 99);
        _testEscapedFieldNames(doc, nameExp, 0, 5);
        _testEscapedFieldNames(doc, nameExp, 0, 3);
        _testEscapedFieldNames(doc, nameExp, 0, 2);
        _testEscapedFieldNames(doc, nameExp, 0, 1);

        _testEscapedFieldNames(doc, nameExp, 1, 99);
        _testEscapedFieldNames(doc, nameExp, 1, 3);
        _testEscapedFieldNames(doc, nameExp, 1, 1);

        w = new StringWriter();
        w.append("{'");
        w.append(nameEncoded);
        w.append("':true}");
        doc = w.toString().getBytes("UTF-8");

        _testEscapedAposFieldNames(doc, nameExp, 0, 99);
        _testEscapedAposFieldNames(doc, nameExp, 0, 5);
        _testEscapedAposFieldNames(doc, nameExp, 0, 3);
        _testEscapedAposFieldNames(doc, nameExp, 0, 2);
        _testEscapedAposFieldNames(doc, nameExp, 0, 1);

        _testEscapedAposFieldNames(doc, nameExp, 1, 99);
        _testEscapedAposFieldNames(doc, nameExp, 1, 3);
        _testEscapedAposFieldNames(doc, nameExp, 1, 1);
    }

    private void _testEscapedFieldNames(byte[] doc, String expName,
            int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(JSON_F, readSize, doc, offset);
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals(expName, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        r.close();
        assertNull(r.nextToken());
    }

    private void _testEscapedAposFieldNames(byte[] doc, String expName,
            int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(JSON_APOS_F, readSize, doc, offset);
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals(expName, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        r.close();
        assertNull(r.nextToken());
    }
}
