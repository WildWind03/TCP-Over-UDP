package com.chirikhin.io;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.*;

public class ListInputStreamTest {
    @Test
    public void read() throws Exception {
        BlockingQueue<byte[]> blockingQueue = new LinkedBlockingQueue<>();
        blockingQueue.add(new byte[] {100, 50, 30, 40});
        blockingQueue.add(new byte[] {25, 60});
        ListInputStream listInputStream1 = new ListInputStream(blockingQueue);

        Assert.assertEquals(listInputStream1.read(), 100);
        Assert.assertEquals(listInputStream1.read(), 50);
        Assert.assertEquals(listInputStream1.read(), 30);
        Assert.assertEquals(listInputStream1.read(), 40);

        byte[] bytes = new byte[2];
        listInputStream1.read(bytes);
        Assert.assertEquals(new byte[] {25, 60}[0], bytes[0]);
    }

    @Test
    public void read1() throws Exception {
        BlockingQueue<byte[]> bytes = new LinkedBlockingQueue<>();
        String text1 = "Hello";
        String text2 = "Ha!";
        String text3 = "Hey!";

        bytes.add(text1.getBytes(Charset.forName("UTF-8")));
        bytes.add(text2.getBytes(Charset.forName("UTF-8")));
       // bytes.add(text3.getBytes(Charset.forName("UTF-8")));

        ListInputStream listInputStream = new ListInputStream(bytes);

        byte[] bytes1 = new byte[text1.getBytes(Charset.forName("UTF-8")).length];
        listInputStream.read(bytes1);
        String string = new String(bytes1);
        Assert.assertEquals(string, text1);

        byte[] bytes2 = new byte[text2.getBytes(Charset.forName("UTF-8")).length];
        listInputStream.read(bytes2);
        String string2 = new String(bytes2);
        Assert.assertEquals(string2, text2);

        bytes.add(text1.getBytes(Charset.forName("UTF-8")));
        bytes.add(text2.getBytes(Charset.forName("UTF-8")));
        bytes.add(text3.getBytes(Charset.forName("UTF-8")));

        byte[] bytes3 = new byte[text1.getBytes(Charset.forName("UTF-8")).length +
                text2.getBytes(Charset.forName("UTF-8")).length +
                text3.getBytes(Charset.forName("UTF-8")).length];

        listInputStream.read(bytes3);
        String string3 = new String(bytes3);
        Assert.assertEquals(string3, text1 + text2 + text3);
    }

}