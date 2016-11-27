package com.chirikhin.net;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class Socket implements Closeable {
    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    private final SocketImpl socketImpl;
    private final BlockingQueue<byte[]> inputBlockingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> outputBlockingQueue = new LinkedBlockingQueue<>();

    Socket(int port, String host) throws SocketException {
        socketImpl = new SocketImpl(host, port, outputBlockingQueue, inputBlockingQueue);
    }

    public Socket(String host, int port) throws SocketException {
        socketImpl = new SocketImpl(host, port, outputBlockingQueue, inputBlockingQueue);
        socketImpl.initAck();
    }

    public int getPort() {
        return socketImpl.getPort();
    }

    public InputStream getInputStream() {
        return socketImpl.getInputStream();
    }

    public OutputStream getOutputStream() {
        return socketImpl.getOutputStream();
    }

    public void close() {
        socketImpl.close();
    }
}
