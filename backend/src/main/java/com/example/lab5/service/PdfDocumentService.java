package com.example.lab5.service;

public interface PdfDocumentService {

    byte[] renderContractPdf(Long contractId, String username, boolean staff);

    byte[] renderInvoicePdf(Long invoiceId, String username, boolean staff);
}
