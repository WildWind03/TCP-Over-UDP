package com.chirikhin.net;

import java.util.logging.Logger;

public class CloseMessage extends BaseMessage {
    private static final Logger logger = Logger.getLogger(CloseMessage.class.getName());

    public CloseMessage(int id) {
        super(id);
    }

    @Override
    byte[] bytes() {
        return getByteArrayWithTypeAndId(MessageType.CLOSE);
    }
}
