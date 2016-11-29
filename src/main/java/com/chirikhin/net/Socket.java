package com.chirikhin.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.logging.Logger;

public class Socket implements Closeable {
    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    private final SocketImpl socketImpl;

    Socket (SocketImpl socketImpl) {
        this.socketImpl = socketImpl;
    }

    public Socket(String host, int port) throws SocketException, SocketTimeoutException, InterruptedException {
        socketImpl = new CreatedByUserSocketImpl(host, port);
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

    public void close() throws IOException {
        socketImpl.close();
    }
}
