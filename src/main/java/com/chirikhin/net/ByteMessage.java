package com.chirikhin.net;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class ByteMessage extends BaseMessage {
    private static final Logger logger = Logger.getLogger(ByteMessage.class.getName());
    private byte[] bytes;
    private final int part;

    public ByteMessage(int id, int part, byte[] bytes) {
        super(id);
        this.bytes = bytes;
        this.part = part;
    }

    @Override
    void process(MySocketImpl mySocketImpl) {
        mySocketImpl.handleByteMessage(this);
    }

    @Override
    byte[] bytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(part);
        byteBuffer.flip();

        byte[] partBytes = new byte[4];
        byteBuffer.get(partBytes);

        byte[] bytesWithPart = ArrayUtils.addAll(getByteArrayWithTypeAndId(MessageType.BYTE), partBytes);

        return ArrayUtils.addAll(bytesWithPart, bytes);
    }

    int getPart() {
        return part;
    }

    byte[] getContentedByted() {
        return bytes;
    }
}
