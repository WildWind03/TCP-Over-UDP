package com.chirikhin.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ListOutputStream extends OutputStream {
    private static final Logger logger = Logger.getLogger(ListOutputStream.class.getName());

    //private final Collection<byte[]> collection;

    private boolean isClosed = false;
    private final Consumer<byte[]> consumer;

    /*public ListOutputStream(Collection<byte[]> collection) {
        this.collection = collection;
    }*/
    public ListOutputStream(Consumer<byte[]> consumer) {
        this.consumer = consumer;
    }

    public void write(int b) throws IOException {
        if (isClosed) {
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(b);
        byteBuffer.flip();

        byte[] a = new byte[4];
        byteBuffer.get(a);
        consumer.accept(a);
        //collection.add(a);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (isClosed) {
            return;
        }

        byte[] a = Arrays.copyOfRange(b, off, off + len);
        consumer.accept(a);
        //collection.add(a);
    }

    @Override
    public void write(byte[] b) {
        if (isClosed) {
            return;
        }

        consumer.accept(b);
        //collection.add(b);
        logger.info("New message was added to queue");
    }

    @Override
    public void close() {
        isClosed = true;
    }


}
