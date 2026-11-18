package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facility")
@Getter
@Setter
public class Facility extends BaseEntity {
    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 150)
    @Column(name = "title_hi")
    private String titleHi;

    @Column(columnDefinition = "text")
    @Size(max = 4000)
    private String description;

    @Column(name = "description_hi", columnDefinition = "text")
    @Size(max = 4000)
    private String descriptionHi;

    @Size(max = 300)
    @Column(name = "image_url")
    private String imageUrl;

    @Size(max = 40)
    private String icon;

    @Column(name = "display_order")
    private int displayOrder;

    private boolean published = true;
}
