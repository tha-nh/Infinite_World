package com.infinite.common.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> data;
    int totalPages;
    long totalElements;

    public static <T> PageResponse<T> success(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}