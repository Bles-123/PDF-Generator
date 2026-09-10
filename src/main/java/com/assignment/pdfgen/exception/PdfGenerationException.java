package com.assignment.pdfgen.exception;

/**
 * Thrown when the HTML-to-PDF rendering pipeline fails.
 */
public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
