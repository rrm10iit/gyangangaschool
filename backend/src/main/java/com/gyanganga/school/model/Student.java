package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student")
@Getter
@Setter
public class Student extends BaseEntity {
    @NotBlank
    @Size(max = 40)
    @Column(name = "admission_no")
    private String admissionNo;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name")
    private String fullName;

    @NotNull
    @Column(name = "class_id")
    private Long classId;

    @Size(max = 10)
    private String section;

    @Column(name = "roll_no")
    private Integer rollNo;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 10)
    private String gender;

    @Size(max = 150)
    @Column(name = "guardian_name")
    private String guardianName;

    @Size(max = 30)
    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Email
    @Size(max = 190)
    @Column(name = "guardian_email")
    private String guardianEmail;

    @Size(max = 30)
    @Column(name = "contact_phone")
    private String contactPhone;

    @Size(max = 500)
    private String address;

    @Size(max = 300)
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @DecimalMin("0")
    @Column(name = "fee_discount", nullable = false)
    private BigDecimal feeDiscount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.StudentStatus status = Enums.StudentStatus.ACTIVE;
}
