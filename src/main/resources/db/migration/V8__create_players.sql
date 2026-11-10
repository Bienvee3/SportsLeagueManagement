-- Migration: V8 — Team squads.
-- Description: Players belong to one team. A removed player is deactivated (active = FALSE)
--              so lineups and match events keep their reference.
CREATE TABLE players (
    id                UUID PRIMARY KEY,
    team_id           UUID NOT NULL,
    name              VARCHAR(100) NOT NULL,
    shirt_number      INTEGER NOT NULL,
    playing_position  VARCHAR(30),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT chk_players_shirt_number CHECK (shirt_number >= 1 AND shirt_number <= 99)
);
CREATE INDEX idx_players_team_id ON players(team_id);
