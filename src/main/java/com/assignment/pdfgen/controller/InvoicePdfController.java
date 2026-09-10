package com.assignment.pdfgen.controller;

import com.assignment.pdfgen.model.InvoiceRequest;
import com.assignment.pdfgen.model.PdfResult;
import com.assignment.pdfgen.service.PdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoicePdfController {

    private final PdfService pdfService;

    public InvoicePdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    
    @PostMapping(value = "/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generatePdf(@Valid @RequestBody InvoiceRequest request) {
        PdfResult result = pdfService.getOrGenerate(request);
        return buildPdfResponse(result.getContent(), result.getHash(), result.isCached());
    }

    
    @GetMapping("/pdf/{hash}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String hash) {
        byte[] content = pdfService.getByHash(hash);
        return buildPdfResponse(content, hash, true);
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] content, String hash, boolean cached) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + hash + ".pdf");
        headers.add("X-Pdf-Hash", hash);
        headers.add("X-Pdf-Cache-Status", cached ? "HIT" : "MISS");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
