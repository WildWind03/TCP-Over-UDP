package com.chirikhin.main;

import com.chirikhin.net.ServerSocket;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class MainServer {
    private static final Logger logger = Logger.getLogger(MainServer.class.getName());

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(12000);
            serverSocket.accept();
        } catch (SocketException | InterruptedException | SocketTimeoutException e) {
            e.printStackTrace();
        }
    }
}
