package com.taskflow.api.dto.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Envelope for paged results. Keeps the wire format stable and independent of Spring Data's
 * {@code Page} serialization, which is discouraged for public APIs.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
