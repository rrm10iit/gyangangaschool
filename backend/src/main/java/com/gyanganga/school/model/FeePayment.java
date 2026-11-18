package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fee_payment")
@Getter
@Setter
public class FeePayment extends BaseEntity {
    @Column(name = "receipt_no", nullable = false, updatable = false)
    private String receiptNo;

    @NotNull
    @Column(name = "student_id", updatable = false)
    private Long studentId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "academic_year", nullable = false)
    private int academicYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Enums.PayMode mode;

    @Size(max = 100)
    @Column(name = "for_period")
    private String forPeriod;

    @Size(max = 100)
    private String reference;

    @Size(max = 300)
    private String remarks;

    @Column(name = "collected_by")
    private String collectedBy;
}
