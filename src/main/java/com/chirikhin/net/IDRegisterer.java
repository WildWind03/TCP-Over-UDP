package com.chirikhin.net;

import java.util.logging.Logger;

public enum IDRegisterer {
    INSTANCE;

    private static final Logger logger = Logger.getLogger(IDRegisterer.class.getName());
    private static volatile int counter = 0;

    public int getNext() {
        return counter++;
    }

    public static IDRegisterer getInstance() {
        return INSTANCE;
    }
}
