package com.chirikhin.net;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class Socket implements Closeable {
    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    private SocketImpl socketImpl;

    Socket(int port, String host) {
       // socketImpl = new SocketImpl(host, port);
    }

    public Socket(String host, int port) {
        //socketImpl = new SocketImpl(host, port);
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
