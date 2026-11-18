package com.gyanganga.school.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "site_setting")
@Getter
@Setter
public class SiteSetting extends BaseEntity {
    @Column(name = "setting_key", nullable = false, updatable = false)
    @JsonProperty("key")
    private String settingKey;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.SettingKind kind = Enums.SettingKind.TEXT;

    @Column(name = "value_en", columnDefinition = "text")
    @Size(max = 20000)
    private String valueEn;

    @Column(name = "value_hi", columnDefinition = "text")
    @Size(max = 20000)
    private String valueHi;

    @Column(name = "sort_order")
    private int sortOrder;
}
