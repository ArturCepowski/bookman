package com.bookman.apartment.calendar;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Fails once more than {@code limit} bytes are read, so a huge response cannot exhaust memory. */
final class LimitedInputStream extends FilterInputStream {

    private final long limit;
    private long read;

    LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            count(1);
        }
        return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int n = super.read(buffer, offset, length);
        if (n > 0) {
            count(n);
        }
        return n;
    }

    private void count(long n) throws IOException {
        read += n;
        if (read > limit) {
            throw new IOException("Calendar larger than " + limit + " bytes");
        }
    }
}
