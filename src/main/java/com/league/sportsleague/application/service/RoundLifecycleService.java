package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.repository.RoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps round statuses in step with their matches. Run by the scheduled round-lifecycle job, so it must
 * be idempotent: a second run right after the first changes nothing.
 */
@Service
public class RoundLifecycleService {

    private final RoundRepository roundRepository;

    public RoundLifecycleService(RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    /**
     * Starts and completes the open rounds of active seasons according to their matches.
     *
     * @return how many rounds changed status
     */
    @Transactional
    public int advanceRounds() {
        int changed = 0;
        for (Round round : roundRepository.findOpenRoundsOfActiveSeasons()) {
            if (round.advanceFromFixtures()) {
                roundRepository.save(round);
                changed++;
            }
        }
        return changed;
    }
}
