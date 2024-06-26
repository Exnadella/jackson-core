package tools.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncPointerFromContext563Test extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    // [core#563]
    @Test
    void pointerWithAsyncParser() throws Exception
    {
        final String SIMPLE = a2q("{'a':123,'array':[1,2,[3],5,{'obInArray':4}],"
                +"'ob':{'first':[false,true],'second':{'sub':37}},'b':true}");
        byte[] SIMPLE_BYTES = SIMPLE.getBytes("UTF-8");

        _testPointerWithAsyncParser(SIMPLE_BYTES, 0, 1000);
        _testPointerWithAsyncParser(SIMPLE_BYTES, 0, 7);
        _testPointerWithAsyncParser(SIMPLE_BYTES, 0, 3);
        _testPointerWithAsyncParser(SIMPLE_BYTES, 0, 2);
        _testPointerWithAsyncParser(SIMPLE_BYTES, 0, 1);

        _testPointerWithAsyncParser(SIMPLE_BYTES, 20, 5);
        _testPointerWithAsyncParser(SIMPLE_BYTES, 14, 1);
    }

    public void _testPointerWithAsyncParser(byte[] doc, int offset, int readSize) throws Exception
    {
        AsyncReaderWrapper p = asyncForBytes(JSON_F, readSize, doc, offset);

        // by default should just get "empty"
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());

        // let's just traverse, then:
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());

        assertEquals("", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // a
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 1
        assertEquals("/array/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 2
        assertEquals("/array/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 3
        assertEquals("/array/2/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 5
        assertEquals("/array/3", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // obInArray
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 4
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken()); // /array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // first
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("/ob/first/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/ob/first/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // second
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // sub
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 37
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken()); // /ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // b
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());

        // note: wrapper maps to `null`, plain async-parser would give NOT_AVAILABLE
        assertNull(p.nextToken());
        p.close();
    }
}
