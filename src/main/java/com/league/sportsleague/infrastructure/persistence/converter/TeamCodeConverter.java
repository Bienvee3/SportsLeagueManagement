package com.league.sportsleague.infrastructure.persistence.converter;

import com.league.sportsleague.domain.valueobject.TeamCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TeamCodeConverter implements AttributeConverter<TeamCode, String> {

    @Override
    public String convertToDatabaseColumn(TeamCode teamCode) {
        return teamCode == null ? null : teamCode.value();
    }

    @Override
    public TeamCode convertToEntityAttribute(String value) {
        return value == null ? null : new TeamCode(value);
    }
}
