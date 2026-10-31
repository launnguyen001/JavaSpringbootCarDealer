package com.example.lab5.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private Double total;

    @Column(name = "paid_amount")
    private Double paidAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private InvoicePaymentStatus paymentStatus = InvoicePaymentStatus.UNPAID;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SalesOrder order;

    public double getRemainingAmount() {
        return Math.max((total == null ? 0.0 : total) - (paidAmount == null ? 0.0 : paidAmount), 0.0);
    }

    public void refreshPaymentStatus() {
        if (paymentStatus == InvoicePaymentStatus.CANCELLED) return;
        if (paidAmount == null) paidAmount = 0.0;
        double paid = paidAmount;
        double amount = total == null ? 0.0 : total;
        if (amount > 0 && paid >= amount - 0.0001d) {
            paymentStatus = InvoicePaymentStatus.PAID;
        } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            paymentStatus = InvoicePaymentStatus.OVERDUE;
        } else if (paid > 0.0001d) {
            paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID;
        } else {
            paymentStatus = InvoicePaymentStatus.UNPAID;
        }
    }
}
