-- =============================================================================
-- Migration: V1__init_schema.sql
-- Description: Initial schema for P39 Sports League Management
-- Database: PostgreSQL 17
-- =============================================================================

CREATE TABLE leagues (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    sport_type VARCHAR(50) NOT NULL DEFAULT 'FOOTBALL',
    country VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    stadium_name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    founded_year INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seasons (
    id UUID PRIMARY KEY,
    league_id UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    year_start INTEGER NOT NULL,
    year_end INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seasons_league_name UNIQUE (league_id, name)
);

CREATE INDEX idx_seasons_league_id ON seasons(league_id);

-- Association entity between Season and Team with custom attributes (LO4 requirement)
CREATE TABLE season_teams (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    initial_seed INTEGER NOT NULL DEFAULT 0,
    registration_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT uk_season_teams_season_team UNIQUE (season_id, team_id)
);

CREATE INDEX idx_season_teams_season_id ON season_teams(season_id);
CREATE INDEX idx_season_teams_team_id ON season_teams(team_id);

CREATE TABLE rounds (
    id UUID PRIMARY KEY,
    season_id UUID NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT uk_rounds_season_round_number UNIQUE (season_id, round_number)
);

CREATE INDEX idx_rounds_season_id ON rounds(season_id);

CREATE TABLE fixtures (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    home_team_id UUID NOT NULL REFERENCES teams(id),
    away_team_id UUID NOT NULL REFERENCES teams(id),
    home_score INTEGER,
    away_score INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
    postponement_reason VARCHAR(255),
    CONSTRAINT chk_fixtures_different_teams CHECK (home_team_id <> away_team_id)
);

CREATE INDEX idx_fixtures_round_id ON fixtures(round_id);
CREATE INDEX idx_fixtures_home_team_id ON fixtures(home_team_id);
CREATE INDEX idx_fixtures_away_team_id ON fixtures(away_team_id);

CREATE TABLE match_events (
    id UUID PRIMARY KEY,
    fixture_id UUID NOT NULL REFERENCES fixtures(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id),
    event_type VARCHAR(30) NOT NULL,
    minute INTEGER NOT NULL,
    player_name VARCHAR(100) NOT NULL,
    assisting_player VARCHAR(100),
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_match_events_minute CHECK (minute >= 1 AND minute <= 130)
);

CREATE INDEX idx_match_events_fixture_id ON match_events(fixture_id);
CREATE INDEX idx_match_events_team_id ON match_events(team_id);

-- Result audit table for tracking corrections (Advanced Req 3)
CREATE TABLE result_audits (
    id UUID PRIMARY KEY,
    fixture_id UUID NOT NULL REFERENCES fixtures(id) ON DELETE CASCADE,
    old_home_score INTEGER,
    old_away_score INTEGER,
    new_home_score INTEGER NOT NULL,
    new_away_score INTEGER NOT NULL,
    reason TEXT NOT NULL,
    corrected_by VARCHAR(100) NOT NULL,
    corrected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_result_audits_fixture_id ON result_audits(fixture_id);
