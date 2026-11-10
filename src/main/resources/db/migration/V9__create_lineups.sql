-- Migration: V9 — Fixture lineups and player-linked match events.
-- Description: One row per player named on the match sheet (starter or substitute).
--              Match events may reference squad players; the name columns stay as a snapshot.
CREATE TABLE lineup_entries (
    id           UUID PRIMARY KEY,
    fixture_id   UUID NOT NULL,
    player_id    UUID NOT NULL,
    lineup_role  VARCHAR(20) NOT NULL,
    captain      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lineup_entries_fixture FOREIGN KEY (fixture_id) REFERENCES fixtures(id) ON DELETE CASCADE,
    CONSTRAINT fk_lineup_entries_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT uk_lineup_entries_fixture_player UNIQUE (fixture_id, player_id)
);
CREATE INDEX idx_lineup_entries_fixture_id ON lineup_entries(fixture_id);

ALTER TABLE match_events ADD COLUMN player_id UUID;
ALTER TABLE match_events ADD COLUMN related_player_id UUID;
ALTER TABLE match_events ADD CONSTRAINT fk_match_events_player FOREIGN KEY (player_id) REFERENCES players(id);
ALTER TABLE match_events ADD CONSTRAINT fk_match_events_related_player FOREIGN KEY (related_player_id) REFERENCES players(id);
