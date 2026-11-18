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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fee_structure")
@Getter
@Setter
public class FeeStructure extends BaseEntity {
    @NotNull
    @Column(name = "class_id")
    private Long classId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "head_name")
    private String headName;

    @NotNull
    @DecimalMin("0")
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Enums.Frequency frequency;
}
