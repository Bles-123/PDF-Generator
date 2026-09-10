package com.assignment.pdfgen.service;

import com.assignment.pdfgen.exception.PdfGenerationException;
import com.assignment.pdfgen.exception.PdfNotFoundException;
import com.assignment.pdfgen.model.InvoiceRequest;
import com.assignment.pdfgen.model.PdfResult;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@Service
public class PdfService {

    private static final String TEMPLATE_NAME = "invoice";

    private final TemplateEngine templateEngine;
    private final ObjectMapper hashingMapper;
    private final Path storageDir;

    public PdfService(TemplateEngine templateEngine,
                       @Value("${pdf.storage.dir:./pdf-storage}") String storageDirPath) {
        this.templateEngine = templateEngine;
        this.storageDir = Paths.get(storageDirPath);
        // A mapper dedicated to producing a canonical (key-sorted) JSON
        // representation so that the same logical payload always hashes the
        // same way regardless of field ordering.
        this.hashingMapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    
    public PdfResult getOrGenerate(InvoiceRequest request) {
        String hash = computeHash(request);
        Path pdfPath = storageDir.resolve(hash + ".pdf");

        if (Files.exists(pdfPath)) {
            return new PdfResult(readFile(pdfPath, hash), hash, true);
        }

        byte[] pdfBytes = render(request);
        writeFile(pdfPath, pdfBytes);
        return new PdfResult(pdfBytes, hash, false);
    }

    
    public byte[] getByHash(String hash) {
        Path pdfPath = storageDir.resolve(hash + ".pdf");
        if (!Files.exists(pdfPath)) {
            throw new PdfNotFoundException(hash);
        }
        return readFile(pdfPath, hash);
    }

   
    private byte[] render(InvoiceRequest request) {
        String html = renderHtml(request);
        return htmlToPdf(html);
    }

    private String renderHtml(InvoiceRequest request) {
        Context context = new Context();
        context.setVariable("invoice", request);
        return templateEngine.process(TEMPLATE_NAME, context);
    }

    private byte[] htmlToPdf(String html) {
        try {
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
            jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withW3cDocument(w3cDoc, "");
                builder.toStream(os);
                builder.run();
                return os.toByteArray();
            }
        } catch (IOException e) {
            throw new PdfGenerationException("Could not render PDF stream", e);
        } catch (RuntimeException e) {
            throw new PdfGenerationException("Could not convert HTML to PDF", e);
        }
    }

    String computeHash(InvoiceRequest request) {
        try {
            String canonicalJson = hashingMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new PdfGenerationException("SHA-256 algorithm unavailable", e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new PdfGenerationException("Could not serialize request for hashing", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] readFile(Path path, String hash) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new PdfNotFoundException(hash);
        }
    }

    private void writeFile(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new PdfGenerationException("Could not persist generated PDF", e);
        }
    }
}
