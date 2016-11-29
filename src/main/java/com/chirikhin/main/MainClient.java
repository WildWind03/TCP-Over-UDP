package com.chirikhin.main;

import com.chirikhin.net.Socket;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class MainClient {
    private static final Logger logger = Logger.getLogger(MainClient.class.getName());

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12000);
        } catch (SocketException | SocketTimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
