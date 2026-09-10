# Dynamic PDF Generation — Spring Boot Assignment

A Spring Boot REST API that generates invoice PDFs from JSON input using a
Thymeleaf template rendered to PDF, with local-disk caching: if the exact
same data is submitted again, the previously generated file is served back
instead of being regenerated.

## Tech stack

- **Spring Boot 3.2.5** (Java 17)
- **Thymeleaf** — HTML templating engine
- **openhtmltopdf** (+ jsoup) — HTML → PDF rendering
- **Bean Validation** (`spring-boot-starter-validation`) — request schema enforcement
- **JUnit 5 / Mockito / MockMvc / AssertJ** — tests (TDD-style)

## How caching works

1. The controller receives an `InvoiceRequest` JSON body.
2. `PdfService` serializes the request to a **canonical, key-sorted JSON
   string** and computes its **SHA-256** hash.
3. If `pdf-storage/<hash>.pdf` already exists on disk, it's read and returned
   immediately (`X-Pdf-Cache-Status: HIT`) — **no regeneration**.
4. Otherwise, the invoice is rendered via the Thymeleaf template, converted
   to a PDF, written to `pdf-storage/<hash>.pdf`, and returned
   (`X-Pdf-Cache-Status: MISS`).
5. The hash is also returned in the `X-Pdf-Hash` header, so a client can
   later `GET /api/invoices/pdf/{hash}` to redownload the same file directly,
   without resending the JSON body.

## Project layout

```
src/main/java/com/assignment/pdfgen/
  PdfGenerationApplication.java        Spring Boot entry point
  controller/InvoicePdfController.java REST endpoints
  service/PdfService.java              Hashing, caching, HTML->PDF rendering
  model/InvoiceRequest.java            Request schema (+ validation)
  model/InvoiceItem.java               Line-item schema (+ validation)
  model/PdfResult.java                 Service return value (bytes/hash/cached)
  exception/                           Custom exceptions + @RestControllerAdvice
src/main/resources/
  templates/invoice.html               Thymeleaf invoice layout
  application.properties
src/test/java/com/assignment/pdfgen/
  service/PdfServiceTest.java          Unit tests: generation, caching, hashing
  controller/InvoicePdfControllerTest.java  MockMvc tests: endpoints, validation
```

  !(docs/screenshots/cache-miss.png)


## Running it

Requires Java 17+ and Maven (with normal internet access to Maven Central —
this project was authored in a sandboxed environment without that access, so
the build/tests should be run locally to verify):

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Generated PDFs are cached under
`./pdf-storage/` (configurable via `pdf.storage.dir` in
`application.properties`).

## Running the tests

```bash
mvn test
```

`PdfServiceTest` covers:
- A generated PDF is a valid non-empty file (starts with the `%PDF` magic bytes).
- The file is persisted under the storage directory.
- Submitting identical data twice returns `cached=true` on the second call,
  serves byte-identical content, does **not** touch/rewrite the file (same
  mtime), and only one file exists on disk for that data.
- Different data produces a different hash.
- Logically-identical data produces the **same** hash even if fields were
  populated/serialized in a different order (canonical hashing).
- `getByHash` returns the stored PDF; an unknown hash throws `PdfNotFoundException`.

`InvoicePdfControllerTest` covers:
- Successful generation returns `200`, `application/pdf`, correct
  `Content-Disposition`, and the `X-Pdf-Hash` / `X-Pdf-Cache-Status` headers.
- A cache hit is reflected in `X-Pdf-Cache-Status: HIT`.
- Invalid/incomplete payloads return `400` with per-field error messages.
- `GET /api/invoices/pdf/{hash}` returns the stored PDF, or `404` if unknown.

## API

### `POST /api/invoices/pdf`

Request body (matches the assignment's example schema):

```json
{
  "seller": "XYZ Pvt. Ltd.",
  "sellerGstin": "29AABBCCDD121ZD",
  "sellerAddress": "New Delhi, India",
  "buyer": "Vedant Computers",
  "buyerGstin": "29AABBCCDD131ZD",
  "buyerAddress": "New Delhi, India",
  "items": [
    { "name": "Product 1", "quantity": "12 Nos", "rate": 123.00, "amount": 1476.00 }
  ]
}
```

Response: the PDF file itself (`Content-Type: application/pdf`,
`Content-Disposition: attachment; filename="invoice-<hash>.pdf"`), plus:

| Header                 | Meaning                                   |
|-------------------------|--------------------------------------------|
| `X-Pdf-Hash`            | SHA-256 hash identifying this exact data   |
| `X-Pdf-Cache-Status`    | `MISS` (just generated) or `HIT` (cached)  |

Example with curl (downloads the PDF to `invoice.pdf`):

```bash
curl -X POST http://localhost:8080/api/invoices/pdf \
  -H "Content-Type: application/json" \
  -d @sample-request.json \
  -o invoice.pdf -D -
```

Run it again with the same body — the response headers will show
`X-Pdf-Cache-Status: HIT` and the same `X-Pdf-Hash`, and no new file is
generated.

### `GET /api/invoices/pdf/{hash}`

Redownloads a previously generated PDF using the hash returned above,
without resubmitting the JSON body.

```bash
curl http://localhost:8080/api/invoices/pdf/<hash> -o invoice.pdf
```

Returns `404` with a JSON error body if the hash is unknown.

## Notes / design choices

- **No UI** — REST only, testable via Postman/curl/Swagger, per the spec.
- **Validation**: all required string fields use `@NotBlank`, `items` must be
  non-empty, and each item's `rate`/`amount` must be non-negative numbers.
  Invalid requests get a `400` with a `fieldErrors` map.
- **HTML → PDF**: Thymeleaf renders the invoice to an HTML string, which is
  parsed with jsoup (tolerant of real-world HTML) and converted to XHTML for
  `openhtmltopdf`, which rasterizes it to PDF bytes.
- **Storage key**: the SHA-256 hash is computed from a Jackson serialization
  with alphabetically-sorted properties, so the same logical invoice data
  always yields the same key regardless of how the object was populated.
