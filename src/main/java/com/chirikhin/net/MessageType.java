package com.chirikhin.net;


public enum MessageType {
    SALUTATION((byte) 0), CONFIRM((byte) 1), BYTE((byte) 2), CLOSE((byte) 3);

    private final byte value;
    MessageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
