package com.chirikhin.io;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ListInputStream extends InputStream {
    private static final Logger logger = Logger.getLogger(ListInputStream.class.getName());

    private byte[] currentBytes;
    private int currentPos = 0;

    private final BlockingQueue<byte[]> list;

    public ListInputStream(BlockingQueue<byte[]> list) {
        this.list = list;
    }

    public int read() throws IOException {
        byte[] bytes = new byte[1];

        read(bytes);

        return bytes[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        int length = b.length;
        int currentLength = 0;

        try {
            if (currentBytes == null) {
                currentBytes = list.take();
            } else {
                if (currentPos >= currentBytes.length) {
                    currentBytes = list.take();
                    currentPos = 0;
                }
            }

            if (currentBytes.length - currentPos >= length) {
                for (currentLength = 0; currentLength < length; ++currentLength, ++currentPos) {
                    b[currentLength] = currentBytes[currentPos];
                }

                return length;
            } else {
                for (currentLength = 0; currentLength < currentBytes.length; ++currentLength) {
                    b[currentLength] = currentBytes[currentPos++];
                }
            }

            while(true) {
                byte[] bytes = list.take();

                if (currentLength + bytes.length < length) {
                    for (byte c: bytes) {
                        b[currentLength++] = c;
                    }
                } else {
                    currentBytes = bytes;
                    int k = 0;
                    while (currentLength < length) {
                        b[currentLength++] = bytes[k++];
                    }

                    currentPos = k;
                    break;
                }
            }

        } catch (InterruptedException e) {
                e.printStackTrace();
        }

        return currentLength;

    }
}
