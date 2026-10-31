package com.league.sportsleague.infrastructure.persistence.converter;

import com.league.sportsleague.domain.valueobject.Slug;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SlugConverter implements AttributeConverter<Slug, String> {

    @Override
    public String convertToDatabaseColumn(Slug slug) {
        return slug == null ? null : slug.value();
    }

    @Override
    public Slug convertToEntityAttribute(String value) {
        return value == null ? null : new Slug(value);
    }
}
