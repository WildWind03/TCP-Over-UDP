package com.chirikhin.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

public class ListOutputStream extends OutputStream {
    private static final Logger logger = Logger.getLogger(ListOutputStream.class.getName());
    private final Collection<byte[]> collection;

    public ListOutputStream(Collection<byte[]> collection) {
        this.collection = collection;
    }

    public void write(int b) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(b);
        byteBuffer.flip();

        byte[] a = new byte[4];
        byteBuffer.get(a);
        collection.add(a);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] a = Arrays.copyOfRange(b, off, off + len);
        collection.add(a);
    }


}
