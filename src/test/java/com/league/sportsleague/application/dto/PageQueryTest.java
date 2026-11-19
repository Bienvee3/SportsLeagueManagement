package com.league.sportsleague.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageQueryTest {

    private static final Set<String> FIELDS = Set.of("name", "city");

    @Test
    @DisplayName("defaults: first page, 20 items, sorted by the default field ascending")
    void defaults() {
        Pageable p = PageQuery.of(null, null, null, FIELDS, "name");

        assertEquals(0, p.getPageNumber());
        assertEquals(20, p.getPageSize());
        assertEquals(Sort.by(Sort.Order.asc("name")), p.getSort());
    }

    @Test
    @DisplayName("sort accepts field and direction, case-insensitively")
    void sortWithDirection() {
        Pageable p = PageQuery.of(2, 5, "city,DESC", FIELDS, "name");

        assertEquals(2, p.getPageNumber());
        assertEquals(5, p.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("city")), p.getSort());
    }

    @Test
    @DisplayName("a sort field outside the whitelist is rejected, never passed to the query")
    void unknownSortField() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PageQuery.of(0, 10, "password", FIELDS, "name"));
        assertEquals("Cannot sort by 'password'; allowed: [city, name]", ex.getMessage());
    }

    @Test
    @DisplayName("page below 0, size outside 1..100 and a bad direction are rejected")
    void outOfRange() {
        assertThrows(IllegalArgumentException.class, () -> PageQuery.of(-1, 10, null, FIELDS, "name"));
        assertThrows(IllegalArgumentException.class, () -> PageQuery.of(0, 0, null, FIELDS, "name"));
        assertThrows(IllegalArgumentException.class, () -> PageQuery.of(0, 101, null, FIELDS, "name"));
        assertThrows(IllegalArgumentException.class, () -> PageQuery.of(0, 10, "name,sideways", FIELDS, "name"));
    }
}
