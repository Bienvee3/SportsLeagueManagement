package com.league.sportsleague.application.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Builds a validated {@link Pageable} from raw request parameters. The sort field is checked against a
 * whitelist because it ends up in the query: an unknown name would otherwise be a 500, and a free choice
 * would let a client sort by any column.
 */
public final class PageQuery {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageQuery() {
    }

    /**
     * @param sort {@code field} or {@code field,asc|desc}; null uses {@code defaultField} ascending
     * @throws IllegalArgumentException for a negative page, a size outside 1..{@value MAX_SIZE}, an unknown
     *                                  field or a direction other than asc/desc (answered as 400)
     */
    public static Pageable of(Integer page, Integer size, String sort, Set<String> allowedFields, String defaultField) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        String field = defaultField;
        Sort.Direction direction = Sort.Direction.ASC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            field = parts[0].trim();
            if (parts.length == 2) {
                direction = Sort.Direction.fromOptionalString(parts[1].trim())
                        .orElseThrow(() -> new IllegalArgumentException("sort direction must be asc or desc"));
            }
        }
        if (!allowedFields.contains(field)) {
            throw new IllegalArgumentException("Cannot sort by '" + field + "'; allowed: " + new TreeSet<>(allowedFields));
        }
        return PageRequest.of(p, s, Sort.by(new Sort.Order(direction, field)));
    }
}
