package com.chirikhin.net;

import com.chirikhin.io.ListOutputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

class SocketImpl {
    private static final Logger logger = Logger.getLogger(SocketImpl.class.getName());
    private static final int TCP_PORT = 12301;

    private final DatagramSocket datagramSocket;
    private final InetSocketAddress serverInetSocketAddress;
    private final ListOutputStream listOutputStream;

    SocketImpl(String host, int port) throws SocketException {
        datagramSocket = new DatagramSocket(TCP_PORT);
        serverInetSocketAddress = new InetSocketAddress(host, port);
        listOutputStream = new ListOutputStream(new LinkedBlockingQueue<byte[]>());
    }

    void initAck() {

    }

    int getPort() {
        return serverInetSocketAddress.getPort();
    }

    InputStream getInputStream() {
        return null;
    }

    OutputStream getOutputStream() {
        return listOutputStream;
    }

    void close() {

    }
}
