package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_class")
@Getter
@Setter
public class SchoolClass extends BaseEntity {
    @NotBlank
    @Size(max = 60)
    private String name;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(columnDefinition = "text")
    @Size(max = 4000)
    private String description;

    @Column(name = "description_hi", columnDefinition = "text")
    @Size(max = 4000)
    private String descriptionHi;

    @Size(max = 500)
    private String subjects;

    @NotBlank
    @Size(max = 100)
    private String sections = "A";

    private Integer seats;

    @Column(name = "age_group")
    @Size(max = 60)
    private String ageGroup;
}
