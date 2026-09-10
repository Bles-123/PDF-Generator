package com.assignment.pdfgen.model;


public class PdfResult {

    private final byte[] content;
    private final String hash;
    private final boolean cached;

    public PdfResult(byte[] content, String hash, boolean cached) {
        this.content = content;
        this.hash = hash;
        this.cached = cached;
    }

    public byte[] getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    public boolean isCached() {
        return cached;
    }
}
