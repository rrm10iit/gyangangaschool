package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "salary_record")
@Getter
@Setter
public class SalaryRecord extends BaseEntity {
    @NotNull
    @Column(name = "teacher_id")
    private Long teacherId;

    @NotNull
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "must be YYYY-MM")
    private String month;

    @NotNull
    @DecimalMin("0")
    @Column(name = "base_amount")
    private BigDecimal baseAmount;

    @DecimalMin("0")
    private BigDecimal allowances = BigDecimal.ZERO;

    @DecimalMin("0")
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.SalaryStatus status = Enums.SalaryStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private Enums.PayMode mode;

    @Size(max = 300)
    private String remarks;
}
