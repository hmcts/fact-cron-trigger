package uk.gov.hmcts.reform.fact.models;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFileName;
    private final String contentType;
    private final byte[] content;

    public StringMultipartFile(String name, String originalFileName, String contentType, String content) {
        this.name = name;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.content = content.getBytes();  // Convert the content string to bytes
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        // Optionally, you can implement the logic to transfer the content to a file here.
    }
}

