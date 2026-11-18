package com.gyanganga.school.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageDto<T>(List<T> content, long total, int page, int size) {
    public static <T> PageDto<T> of(Page<T> p) {
        return new PageDto<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }
}
