package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gallery_album")
@Getter
@Setter
public class GalleryAlbum extends BaseEntity {
    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 150)
    @Column(name = "title_hi")
    private String titleHi;

    @NotBlank
    @Size(max = 30)
    private String category;

    @Column(name = "event_date")
    private LocalDate eventDate;

    private boolean published = true;
}
