package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "testimonial")
@Getter
@Setter
public class Testimonial extends BaseEntity {
    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 200)
    private String role;

    @NotBlank
    @Column(columnDefinition = "text")
    @Size(max = 3000)
    private String quote;

    @Column(name = "quote_hi", columnDefinition = "text")
    @Size(max = 3000)
    private String quoteHi;

    @Size(max = 300)
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "display_order")
    private int displayOrder;

    private boolean published = true;
}
