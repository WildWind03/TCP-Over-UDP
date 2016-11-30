package com.chirikhin.net;

import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public abstract class MySocketImpl implements SocketImpl {
    private static final Logger logger = Logger.getLogger(MySocketImpl.class.getName());

    private static final int TIME_TO_WAIT = 5000;

    private boolean isConnectionSet = false;
    private final Object lock = new Object();

    abstract void handleSalutationMessage(SalutationMessage salutationMessage);
    abstract void handleConfirmMessage(ConfirmMessage confirmMessage);
    abstract void handleCloseMessage(CloseMessage closeMessage);
    abstract void handleByteMessage(ByteMessage byteMessage);

    protected void waitForConnection() throws InterruptedException, SocketTimeoutException {
        logger.info("Start waiting for connetion");

        long timeOfStart = System.currentTimeMillis();
        long timeToWait = TIME_TO_WAIT;

        synchronized (lock) {
            while (timeToWait > 0) {
                lock.wait(timeToWait);

                if (isConnectionSet) {
                    return;
                } else {
                    timeToWait = TIME_TO_WAIT - (System.currentTimeMillis() - timeOfStart);
                }
            }
        }

        throw new SocketTimeoutException("Can't set up a connection");
    }

    protected boolean isConnectionSet() {
        return isConnectionSet;
    }

    protected void haveConnectionSet() {
        synchronized (lock) {
            isConnectionSet = true;
            lock.notify();
        }
    }



}
