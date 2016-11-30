package com.chirikhin.net;

import org.apache.commons.lang3.ArrayUtils;

import java.util.logging.Logger;

public class ByteMessage extends BaseMessage {
    private static final Logger logger = Logger.getLogger(ByteMessage.class.getName());
    private byte[] bytes;

    public ByteMessage(int id, byte[] bytes) {
        super(id);
        this.bytes = bytes;
    }

    @Override
    void process(MySocketImpl mySocketImpl) {
        mySocketImpl.handleByteMessage(this);
    }

    @Override
    byte[] bytes() {
        return ArrayUtils.addAll(getByteArrayWithTypeAndId(MessageType.BYTE), bytes);
    }

    byte[] getContentedByted() {
        return bytes;
    }
}
