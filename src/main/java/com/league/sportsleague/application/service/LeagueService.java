package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.LeagueResponse;
import com.league.sportsleague.application.dto.UpdateLeagueRequest;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.repository.LeagueRepository;
import com.league.sportsleague.domain.valueobject.Slug;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LeagueService {

    private final LeagueRepository leagueRepository;

    public LeagueService(LeagueRepository leagueRepository) {
        this.leagueRepository = leagueRepository;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public UUID createLeague(CreateLeagueRequest request) {
        League league = new League(
                request.name(),
                request.slug(),
                request.sportType(),
                request.country()
        );
        leagueRepository.save(league);
        return league.getId();
    }

    public void rename(UUID id, UpdateLeagueRequest request) {
        League league = leagueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("League", id));
        league.rename(request.name(), request.country(), request.sportType());
        leagueRepository.save(league);
    }

    public void delete(UUID id) {
        League league = leagueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("League", id));
        leagueRepository.delete(league);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LeagueResponse findById(UUID id) {
        return leagueRepository.findById(id)
                .map(LeagueResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("League", id));
    }

    @Transactional(readOnly = true)
    public LeagueResponse findBySlug(String slug) {
        return leagueRepository.findBySlug(new Slug(slug))
                .map(LeagueResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("League", slug));
    }

    @Transactional(readOnly = true)
    public List<LeagueResponse> findAll() {
        return leagueRepository.findAll().stream()
                .map(LeagueResponse::from)
                .toList();
    }
}
