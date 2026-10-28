package by.piskunou.teletronics.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public final class ZeroStreamingMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final long size;

    public ZeroStreamingMultipartFile(String name, String originalFilename, String contentType, long size) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException("A 2 GiB payload must not be loaded into memory");
    }

    @Override
    public InputStream getInputStream() {
        return new ZeroInputStream(size);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(dest, "rw")) {
            file.setLength(size);
        }
    }

    @Override
    public void transferTo(Path dest) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(dest.toFile(), "rw")) {
            file.setLength(size);
        }
    }
}
