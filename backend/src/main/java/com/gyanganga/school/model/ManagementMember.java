package com.gyanganga.school.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "management_member")
@Getter
@Setter
public class ManagementMember extends BaseEntity {
    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 150)
    private String designation;

    @Size(max = 150)
    @Column(name = "designation_hi")
    private String designationHi;

    @Size(max = 300)
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "display_order")
    private int displayOrder;

    private boolean published = true;
}
