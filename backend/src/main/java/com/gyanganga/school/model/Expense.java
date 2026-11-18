package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "expense")
@Getter
@Setter
public class Expense extends BaseEntity {
    @NotBlank
    @Size(max = 60)
    private String category;

    @NotBlank
    @Size(max = 300)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Size(max = 150)
    @Column(name = "paid_to")
    private String paidTo;

    @Enumerated(EnumType.STRING)
    private Enums.PayMode mode;

    @Column(name = "created_by")
    private String createdBy;
}
