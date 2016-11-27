package com.chirikhin.net;

import java.util.logging.Logger;

public class SentMessage {
    private static final Logger logger = Logger.getLogger(SentMessage.class.getName());
    private final BaseMessage baseMessage;
    private long time;

    public BaseMessage getBaseMessage() {
        return baseMessage;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long newTime) {
        this.time = newTime;
    }

    public SentMessage(BaseMessage baseMessage, long time) {

        this.baseMessage = baseMessage;
        this.time = time;
    }
}
