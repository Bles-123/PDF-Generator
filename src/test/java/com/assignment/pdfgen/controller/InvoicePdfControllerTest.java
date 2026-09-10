package com.assignment.pdfgen.controller;

import com.assignment.pdfgen.exception.PdfNotFoundException;
import com.assignment.pdfgen.model.PdfResult;
import com.assignment.pdfgen.service.PdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoicePdfController.class)
class InvoicePdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdfService pdfService;

    private static final byte[] FAKE_PDF = "%PDF-1.4 fake content".getBytes();

    private String validPayload() {
        return """
                {
                  "seller": "XYZ Pvt. Ltd.",
                  "sellerGstin": "29AABBCCDD121ZD",
                  "sellerAddress": "New Delhi, India",
                  "buyer": "Vedant Computers",
                  "buyerGstin": "29AABBCCDD131ZD",
                  "buyerAddress": "New Delhi, India",
                  "items": [
                    {
                      "name": "Product 1",
                      "quantity": "12 Nos",
                      "rate": 123.00,
                      "amount": 1476.00
                    }
                  ]
                }
                """;
    }

    @Test
    void generatePdf_returnsPdfWithMissHeaderOnFirstCall() throws Exception {
        when(pdfService.getOrGenerate(any())).thenReturn(new PdfResult(FAKE_PDF, "abc123", false));

        mockMvc.perform(post("/api/invoices/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("X-Pdf-Hash", "abc123"))
                .andExpect(header().string("X-Pdf-Cache-Status", "MISS"))
                .andExpect(header().string("Content-Disposition", containsAttachmentFilename("invoice-abc123.pdf")))
                .andExpect(content().bytes(FAKE_PDF));
    }

    @Test
    void generatePdf_returnsCacheHitHeaderWhenServiceReportsCached() throws Exception {
        when(pdfService.getOrGenerate(any())).thenReturn(new PdfResult(FAKE_PDF, "abc123", true));

        mockMvc.perform(post("/api/invoices/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Pdf-Cache-Status", "HIT"));
    }

    @Test
    void generatePdf_rejectsPayloadMissingRequiredFields() throws Exception {
        String invalidPayload = """
                {
                  "seller": "",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/invoices/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void downloadPdf_returnsPdfWhenHashExists() throws Exception {
        when(pdfService.getByHash("abc123")).thenReturn(FAKE_PDF);

        mockMvc.perform(get("/api/invoices/pdf/abc123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Pdf-Hash", "abc123"))
                .andExpect(content().bytes(FAKE_PDF));
    }

    @Test
    void downloadPdf_returns404WhenHashUnknown() throws Exception {
        when(pdfService.getByHash("missing")).thenThrow(new PdfNotFoundException("missing"));

        mockMvc.perform(get("/api/invoices/pdf/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No PDF found for hash: missing"));
    }

    // Small helper matcher so the Content-Disposition assertion reads cleanly.
    private static org.hamcrest.Matcher<String> containsAttachmentFilename(String filename) {
        return org.hamcrest.Matchers.containsString(filename);
    }
}
