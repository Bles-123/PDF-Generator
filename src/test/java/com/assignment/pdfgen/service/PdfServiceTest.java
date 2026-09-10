package com.assignment.pdfgen.service;

import com.assignment.pdfgen.exception.PdfNotFoundException;
import com.assignment.pdfgen.model.InvoiceItem;
import com.assignment.pdfgen.model.InvoiceRequest;
import com.assignment.pdfgen.model.PdfResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfServiceTest {

    @TempDir
    Path tempStorageDir;

    private PdfService pdfService;

    @BeforeEach
    void setUp() throws IOException {
        TemplateEngine templateEngine = buildTemplateEngine();
        pdfService = new PdfService(templateEngine, tempStorageDir.toString());
        pdfService.init();
    }

    private TemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private InvoiceRequest sampleRequest() {
        InvoiceRequest request = new InvoiceRequest();
        request.setSeller("XYZ Pvt. Ltd.");
        request.setSellerGstin("29AABBCCDD121ZD");
        request.setSellerAddress("New Delhi, India");
        request.setBuyer("Vedant Computers");
        request.setBuyerGstin("29AABBCCDD131ZD");
        request.setBuyerAddress("New Delhi, India");
        request.setItems(List.of(
                new InvoiceItem("Product 1", "12 Nos", new BigDecimal("123.00"), new BigDecimal("1476.00"))
        ));
        return request;
    }

    @Test
    void generatesAValidNonEmptyPdfOnFirstRequest() {
        PdfResult result = pdfService.getOrGenerate(sampleRequest());

        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.isCached()).isFalse();
        assertThat(result.getHash()).isNotBlank();
        // %PDF magic bytes at the very start of the file.
        assertThat(new String(result.getContent(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void persistsGeneratedPdfToStorageDirectory() throws IOException {
        PdfResult result = pdfService.getOrGenerate(sampleRequest());

        Path expectedPath = tempStorageDir.resolve(result.getHash() + ".pdf");
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(Files.size(expectedPath)).isGreaterThan(0);
    }

    @Test
    void secondIdenticalRequestIsServedFromCacheNotRegenerated() throws IOException {
        PdfResult first = pdfService.getOrGenerate(sampleRequest());
        Path path = tempStorageDir.resolve(first.getHash() + ".pdf");
        long firstModifiedTime = Files.getLastModifiedTime(path).toMillis();

        // Small delay so a regeneration (if it wrongly happened) would produce
        // a detectably different mtime / content behaviour.
        PdfResult second = pdfService.getOrGenerate(sampleRequest());

        assertThat(second.isCached()).isTrue();
        assertThat(second.getHash()).isEqualTo(first.getHash());
        assertThat(second.getContent()).isEqualTo(first.getContent());
        assertThat(Files.getLastModifiedTime(path).toMillis()).isEqualTo(firstModifiedTime);

        // Only one file should exist for this data.
        try (Stream<Path> files = Files.list(tempStorageDir)) {
            assertThat(files.count()).isEqualTo(1);
        }
    }

    @Test
    void differentRequestDataProducesDifferentHashAndFile() {
        InvoiceRequest first = sampleRequest();
        InvoiceRequest second = sampleRequest();
        second.setBuyer("A Totally Different Buyer");

        PdfResult firstResult = pdfService.getOrGenerate(first);
        PdfResult secondResult = pdfService.getOrGenerate(second);

        assertThat(firstResult.getHash()).isNotEqualTo(secondResult.getHash());
    }

    @Test
    void sameLogicalDataProducesSameHashRegardlessOfFieldPopulationOrder() {
        InvoiceRequest a = sampleRequest();

        InvoiceRequest b = new InvoiceRequest();
        // populate fields in a different order than sampleRequest()
        b.setBuyerAddress("New Delhi, India");
        b.setBuyer("Vedant Computers");
        b.setBuyerGstin("29AABBCCDD131ZD");
        b.setSellerAddress("New Delhi, India");
        b.setSeller("XYZ Pvt. Ltd.");
        b.setSellerGstin("29AABBCCDD121ZD");
        b.setItems(List.of(
                new InvoiceItem("Product 1", "12 Nos", new BigDecimal("123.00"), new BigDecimal("1476.00"))
        ));

        assertThat(pdfService.computeHash(a)).isEqualTo(pdfService.computeHash(b));
    }

    @Test
    void getByHashReturnsPreviouslyGeneratedPdf() {
        PdfResult generated = pdfService.getOrGenerate(sampleRequest());

        byte[] redownloaded = pdfService.getByHash(generated.getHash());

        assertThat(redownloaded).isEqualTo(generated.getContent());
    }

    @Test
    void getByHashThrowsWhenHashUnknown() {
        assertThatThrownBy(() -> pdfService.getByHash("does-not-exist"))
                .isInstanceOf(PdfNotFoundException.class);
    }
}
