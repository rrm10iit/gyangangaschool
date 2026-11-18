package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "announcement")
@Getter
@Setter
public class Announcement extends BaseEntity {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    @Column(name = "title_hi")
    private String titleHi;

    @Column(columnDefinition = "text")
    @Size(max = 5000)
    private String body;

    @Column(name = "body_hi", columnDefinition = "text")
    @Size(max = 5000)
    private String bodyHi;

    @NotNull
    @Column(name = "publish_date")
    private LocalDate publishDate;

    private boolean pinned;

    private boolean published = true;
}
