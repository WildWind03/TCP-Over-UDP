package com.chirikhin.main;

import com.chirikhin.net.ServerSocket;
import com.chirikhin.net.Socket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            try {
                ServerSocket serverSocket = new ServerSocket(12000);
                Socket socket = serverSocket.accept();
                socket.getOutputStream().write("Hello".getBytes());
                socket.close();

                logger.info("Socket is accepted!");
            } catch (SocketException | InterruptedException | SocketTimeoutException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Socket socket = new Socket("localhost", 12000);

                InputStream inputStream = socket.getInputStream();
                byte[] bytes = new byte[5];
                inputStream.read(bytes);

                System.out.println("Delivered message: " + new String(bytes));

                logger.info("Socket is created!");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
