package com.assignment.pdfgen.exception;

/**
 * Thrown when a client requests a PDF by hash that does not exist in storage.
 */
public class PdfNotFoundException extends RuntimeException {

    public PdfNotFoundException(String hash) {
        super("No PDF found for hash: " + hash);
    }
}
