package com.adren.travel.shared;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * The stable collection-endpoint response shape RULES.md §3.4 mandates —
 * {@code content/page/size/totalElements/totalPages} — rather than exposing
 * Spring Data's {@code Page}/{@code PageImpl} JSON serialization directly,
 * which nests paging metadata under a {@code pageable} object that isn't
 * meant to be a stable public contract. A generic, business-logic-free
 * shape like {@link com.adren.travel.shared.Money} — fits `shared`'s OPEN
 * module charter, not a module-specific concern.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
