package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_document")
@Getter
@Setter
public class StudentDocument extends BaseEntity {
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;
}
