package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enquiry")
@Getter
@Setter
public class Enquiry extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "class_interested")
    private String classInterested;

    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.EnquiryStatus status = Enums.EnquiryStatus.NEW;
}
