package com.chirikhin.net;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public interface SocketImpl extends Closeable {
    InputStream getInputStream();
    OutputStream getOutputStream();
    int getPort();
}
