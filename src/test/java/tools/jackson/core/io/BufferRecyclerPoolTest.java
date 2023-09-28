package tools.jackson.core.io;

import java.io.IOException;
import java.io.OutputStream;

import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonGeneratorBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.BufferRecyclerPool;
import tools.jackson.core.util.JsonBufferRecyclers;

public class BufferRecyclerPoolTest extends BaseTest
{
    public void testNoOp() throws Exception {
        // no-op pool doesn't actually pool anything, so avoid checking it
        checkBufferRecyclerPoolImpl(JsonBufferRecyclers.nonRecyclingPool(), false);
    }

    public void testThreadLocal() throws Exception {
        checkBufferRecyclerPoolImpl(JsonBufferRecyclers.threadLocalPool(), true);
    }

    public void testLockFree() throws Exception {
        checkBufferRecyclerPoolImpl(JsonBufferRecyclers.newLockFreePool(), true);
    }

    public void testConcurrentDequeue() throws Exception {
        checkBufferRecyclerPoolImpl(JsonBufferRecyclers.newConcurrentDequePool(), true);
    }

    public void testBounded() throws Exception {
        checkBufferRecyclerPoolImpl(JsonBufferRecyclers.newBoundedPool(1), true);
    }

    public void testPluggingPool() throws Exception {
        checkBufferRecyclerPoolImpl(new TestPool(), true);
    }

    private void checkBufferRecyclerPoolImpl(BufferRecyclerPool<BufferRecycler> pool,
            boolean checkPooledResource) throws Exception {
        JsonFactory jsonFactory = JsonFactory.builder()
                .bufferRecyclerPool(pool)
                .build();
        BufferRecycler usedBufferRecycler = write("test", jsonFactory, 6);

        if (checkPooledResource) {
            // acquire the pooled BufferRecycler again and check if it is the same instance used before
            BufferRecycler pooledBufferRecycler = pool.acquireAndLinkPooled();
            try {
                assertSame(usedBufferRecycler, pooledBufferRecycler);
            } finally {
                pooledBufferRecycler.release();
            }
        }
    }

    protected final BufferRecycler write(String value, JsonFactory jsonFactory, int expectedSize) {
        BufferRecycler bufferRecycler;
        NopOutputStream out = new NopOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(ObjectWriteContext.empty(), out)) {
            bufferRecycler = ((JsonGeneratorBase) gen).ioContext()._bufferRecycler;
            gen.writeString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(expectedSize, out.size);
        return bufferRecycler;
    }

    private static class NopOutputStream extends OutputStream {
        protected int size = 0;

        NopOutputStream() { }

        @Override
        public void write(int b) throws IOException { ++size; }

        @Override
        public void write(byte[] b) throws IOException { size += b.length; }

        @Override
        public void write(byte[] b, int offset, int len) throws IOException { size += len; }
    }


    @SuppressWarnings("serial")
    class TestPool implements BufferRecyclerPool<BufferRecycler>
    {
        private BufferRecycler bufferRecycler;

        @Override
        public BufferRecycler acquirePooled() {
            if (bufferRecycler != null) {
                BufferRecycler tmp = bufferRecycler;
                this.bufferRecycler = null;
                return tmp;
            }
            return new BufferRecycler();
        }

        @Override
        public void releasePooled(BufferRecycler recycler) {
            this.bufferRecycler = recycler;
        }
    }
}