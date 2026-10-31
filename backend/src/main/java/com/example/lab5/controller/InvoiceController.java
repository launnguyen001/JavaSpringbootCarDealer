package com.example.lab5.controller;

import com.example.lab5.dto.InvoiceRequest;
import com.example.lab5.dto.InvoiceResponse;
import com.example.lab5.entity.Invoice;
import com.example.lab5.mapper.InvoiceMapper;
import com.example.lab5.service.InvoiceService;
import com.example.lab5.service.PdfDocumentService;
import com.example.lab5.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;
    private final PdfDocumentService pdfDocumentService;

    public InvoiceController(
            InvoiceService invoiceService,
            InvoiceMapper invoiceMapper,
            PdfDocumentService pdfDocumentService
    ) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
        this.pdfDocumentService = pdfDocumentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return invoiceMapper.toResponse(invoiceService.create(request));
    }

    @GetMapping
    public List<InvoiceResponse> getAllInvoices(
            @RequestParam(required = false) String paymentStatus
    ) {
        return toResponses(invoiceService.getAllInvoices(paymentStatus));
    }

    /** Danh sách hóa đơn còn công nợ để nhân viên theo dõi và truy thu. */
    @GetMapping("/collections")
    public List<InvoiceResponse> getCollectionInvoices(
            @RequestParam(required = false) String status
    ) {
        return toResponses(invoiceService.getInvoicesForCollection(status));
    }

    @GetMapping("/vin/{vin}")
    public InvoiceResponse getByVin(@PathVariable String vin) {
        return invoiceMapper.toResponse(invoiceService.getByVin(vin));
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        return invoiceMapper.toResponse(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/order/{orderId}")
    public InvoiceResponse getByOrder(@PathVariable Long orderId) {
        return invoiceMapper.toResponse(invoiceService.getByOrder(orderId));
    }

    @GetMapping("/order-code/{orderCode}")
    public InvoiceResponse getByOrderCode(@PathVariable String orderCode) {
        return invoiceMapper.toResponse(invoiceService.getByOrderCode(orderCode));
    }

    /** Tải hóa đơn dạng PDF — chủ đơn hoặc nhân viên/admin. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        byte[] pdf = pdfDocumentService.renderInvoicePdf(id, userDetails.getUsername(), SecurityUtils.isStaff());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("hoa-don-" + id + ".pdf", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest request
    ) {
        return invoiceMapper.toResponse(invoiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Long id) {
        invoiceService.delete(id);
    }

    private List<InvoiceResponse> toResponses(List<Invoice> invoices) {
        return invoices.stream().map(invoiceMapper::toResponse).toList();
    }
}
