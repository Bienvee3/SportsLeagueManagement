-- Migration: V5 — Add sport type to teams.
-- Description: Every team belongs to one sport. Existing teams default to FOOTBALL
--              (the only sport seeded so far). Matches leagues.sport_type values.
ALTER TABLE teams ADD COLUMN sport_type VARCHAR(50) NOT NULL DEFAULT 'FOOTBALL';
CREATE INDEX idx_teams_sport_type ON teams(sport_type);
