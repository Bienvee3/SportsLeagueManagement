package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.OverdueFixtureResponse;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Finds matches that should already have started. Read-only: whether to postpone, cancel or start a late
 * match is a decision for an official, so the scheduled job only reports them.
 */
@Service
public class OverdueFixtureService {

    private final FixtureRepository fixtureRepository;

    public OverdueFixtureService(FixtureRepository fixtureRepository) {
        this.fixtureRepository = fixtureRepository;
    }

    /** SCHEDULED matches whose kick-off was more than {@code grace} before {@code now}, oldest first. */
    @Transactional(readOnly = true)
    public List<OverdueFixtureResponse> findOverdue(Instant now, Duration grace) {
        return fixtureRepository.findScheduledBefore(now.minus(grace)).stream()
                .map(OverdueFixtureResponse::from)
                .toList();
    }
}
