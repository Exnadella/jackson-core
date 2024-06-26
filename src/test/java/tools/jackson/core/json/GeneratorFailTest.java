package tools.jackson.core.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.exc.StreamWriteException;

import static org.junit.jupiter.api.Assertions.fail;

class GeneratorFailTest
    extends tools.jackson.core.JUnit5TestBase
{
    private final JsonFactory F = newStreamFactory();

    // [core#167]: no error for writing field name twice
    @Test
    void dupFieldNameWrites() throws Exception
    {
        _testDupFieldNameWrites(F, false);
        _testDupFieldNameWrites(F, true);
    }

    // [core#177]
    // Also: should not try writing JSON String if field name expected
    // (in future maybe take one as alias... but not yet)
    @Test
    void failOnWritingStringNotFieldNameBytes() throws Exception {
        _testFailOnWritingStringNotFieldName(F, false);
    }

    // [core#177]
    @Test
    void failOnWritingStringNotFieldNameChars() throws Exception {
        _testFailOnWritingStringNotFieldName(F, true);
    }

    // for [core#282]
    @Test
    void failOnWritingFieldNameInRoot() throws Exception {
        _testFailOnWritingFieldNameInRoot(F, false);
        _testFailOnWritingFieldNameInRoot(F, true);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private void _testDupFieldNameWrites(JsonFactory f, boolean useReader) throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();
        gen.writeName("a");

        try {
            gen.writeName("b");
            gen.flush();
            String json = utf8String(bout);
            fail("Should not have let two consecutive property name writes succeed: output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "Cannot write a property name, expecting a value");
        }
        gen.close();
    }

    private void _testFailOnWritingStringNotFieldName(JsonFactory f, boolean useReader) throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            gen.writeString("a");
            gen.flush();
            String json = utf8String(bout);
            fail("Should not have let "+gen.getClass().getName()+".writeString() be used in place of 'writeName()': output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "Cannot write a String");
        }
        gen.close();
    }

    // for [core#282]
    private void _testFailOnWritingFieldNameInRoot(JsonFactory f, boolean useReader) throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        try {
            gen.writeName("a");
            gen.flush();
            String json = utf8String(bout);
            fail("Should not have let "+gen.getClass().getName()+".writeName() be used in root context: output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "Cannot write a property name");
        }
        gen.close();
    }
}
