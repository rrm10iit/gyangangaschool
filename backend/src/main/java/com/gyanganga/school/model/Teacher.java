package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teacher")
@Getter
@Setter
public class Teacher extends BaseEntity {
    @NotBlank
    @Size(max = 40)
    @Column(name = "employee_no")
    private String employeeNo;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name")
    private String fullName;

    @Size(max = 300)
    private String subjects;

    @Size(max = 300)
    @Column(name = "classes_assigned")
    private String classesAssigned;

    @Size(max = 200)
    private String qualification;

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 190)
    private String email;

    @Size(max = 500)
    private String address;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Size(max = 300)
    @Column(name = "photo_url")
    private String photoUrl;

    @DecimalMin("0")
    @Column(name = "monthly_salary", nullable = false)
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
}
