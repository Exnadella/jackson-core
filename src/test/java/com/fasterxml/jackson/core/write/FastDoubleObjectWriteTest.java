package com.fasterxml.jackson.core.write;

import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.json.JsonFactory;

public class FastDoubleObjectWriteTest extends ObjectWriteTest {
    private final JsonFactory FACTORY = JsonFactory.builder().enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER).build();

    protected JsonFactory jsonFactory() {
        return FACTORY;
    }
}