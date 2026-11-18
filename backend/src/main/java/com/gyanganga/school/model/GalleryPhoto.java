package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gallery_photo")
@Getter
@Setter
public class GalleryPhoto extends BaseEntity {
    @Column(name = "album_id", nullable = false)
    private Long albumId;

    @Column(nullable = false)
    private String url;

    private String caption;

    @Column(name = "display_order")
    private int displayOrder;
}
