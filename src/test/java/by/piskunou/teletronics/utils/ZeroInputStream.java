package by.piskunou.teletronics.utils;

import java.io.InputStream;
import java.util.Arrays;

public final class ZeroInputStream extends InputStream {
    private long remaining;

    public ZeroInputStream(long size) {
        this.remaining = size;
    }

    @Override
    public int read() {
        return remaining-- > 0 ? 0 : -1;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
        if (length == 0) {
            return 0;
        }
        if (remaining <= 0) {
            return -1;
        }
        int count = (int) Math.min(length, remaining);
        Arrays.fill(buffer, offset, offset + count, (byte) 0);
        remaining -= count;
        return count;
    }

    @Override
    public long skip(long n) {
        if (n <= 0) {
            return 0;
        }
        long skipped = Math.min(n, remaining);
        remaining -= skipped;
        return skipped;
    }
}
