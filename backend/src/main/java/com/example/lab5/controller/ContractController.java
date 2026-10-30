package com.example.lab5.controller;

import com.example.lab5.dto.ContractRequest;
import com.example.lab5.dto.ContractResponse;
import com.example.lab5.entity.Contract;
import com.example.lab5.mapper.ContractMapper;
import com.example.lab5.service.ContractService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractMapper contractMapper;
    private final PdfDocumentService pdfDocumentService;

    public ContractController(
            ContractService contractService,
            ContractMapper contractMapper,
            PdfDocumentService pdfDocumentService
    ) {
        this.contractService = contractService;
        this.contractMapper = contractMapper;
        this.pdfDocumentService = pdfDocumentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse createContract(@Valid @RequestBody ContractRequest request) {
        return contractMapper.toResponse(contractService.create(request));
    }

    @GetMapping
    public List<ContractResponse> getAllContracts() {
        return toResponses(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ContractResponse getContractById(@PathVariable Long id) {
        return contractMapper.toResponse(contractService.getContractById(id));
    }

    @GetMapping("/order/{orderId}")
    public ContractResponse getByOrder(@PathVariable Long orderId) {
        return contractMapper.toResponse(contractService.getByOrder(orderId));
    }

    /** Tải hợp đồng dạng PDF — chủ đơn hoặc nhân viên/admin. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        byte[] pdf = pdfDocumentService.renderContractPdf(id, userDetails.getUsername(), SecurityUtils.isStaff());
        return pdfResponse(pdf, "hop-dong-" + id + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ContractResponse updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequest request
    ) {
        return contractMapper.toResponse(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContract(@PathVariable Long id) {
        contractService.delete(id);
    }

    private List<ContractResponse> toResponses(List<Contract> contracts) {
        return contracts.stream().map(contractMapper::toResponse).toList();
    }
}
