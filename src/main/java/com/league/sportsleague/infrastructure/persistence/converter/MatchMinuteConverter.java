package com.league.sportsleague.infrastructure.persistence.converter;

import com.league.sportsleague.domain.valueobject.MatchMinute;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MatchMinuteConverter implements AttributeConverter<MatchMinute, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MatchMinute matchMinute) {
        return matchMinute == null ? null : matchMinute.value();
    }

    @Override
    public MatchMinute convertToEntityAttribute(Integer value) {
        return value == null ? null : new MatchMinute(value);
    }
}
