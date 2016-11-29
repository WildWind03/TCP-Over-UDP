package com.chirikhin.net;

import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public abstract class MySocketImpl implements SocketImpl {
    private static final Logger logger = Logger.getLogger(MySocketImpl.class.getName());

    private static final int TIME_TO_WAIT = 2000;

    private boolean isConnectionSet = false;
    private final Object lock = new Object();

    abstract void handleSalutationMessage(SalutationMessage salutationMessage);
    abstract void handleConfirmMessage(ConfirmMessage confirmMessage);
    abstract void handleCloseMessage(CloseMessage closeMessage);
    abstract void handleByteMessage(ByteMessage byteMessage);

    protected void waitForConnection() throws InterruptedException, SocketTimeoutException {
        long timeOfStart = System.currentTimeMillis();
        long timeToWait = TIME_TO_WAIT;

        while (timeToWait > 0) {
            lock.wait(timeToWait);

            if (isConnectionSet) {
                return;
            } else {
                timeToWait = System.currentTimeMillis() - timeOfStart;
            }
        }

        throw new SocketTimeoutException("Can't set up a connection");
    }

    protected boolean isConnectionSet() {
        return isConnectionSet;
    }

    protected void haveConnectionSet() {
        isConnectionSet = true;
        lock.notify();
    }



}
