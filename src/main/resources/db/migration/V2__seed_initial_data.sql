-- =============================================================================
-- Migration: V2__seed_initial_data.sql
-- Description: Seed initial data for testing and development
-- =============================================================================

-- 1. Insert League
INSERT INTO leagues (id, name, slug, sport_type, country, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'SuperSport HNL',
    'supersport-hnl',
    'FOOTBALL',
    'Croatia',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 2. Insert Teams
INSERT INTO teams (id, name, short_code, stadium_name, city, founded_year, created_at)
VALUES 
(
    '22222222-2222-2222-2222-222222222201',
    'GNK Dinamo Zagreb',
    'DIN',
    'Stadion Maksimir',
    'Zagreb',
    1911,
    CURRENT_TIMESTAMP
),
(
    '22222222-2222-2222-2222-222222222202',
    'HNK Hajduk Split',
    'HAJ',
    'Stadion Poljud',
    'Split',
    1911,
    CURRENT_TIMESTAMP
),
(
    '22222222-2222-2222-2222-222222222203',
    'HNK Rijeka',
    'RIJ',
    'Stadion Rujevica',
    'Rijeka',
    1906,
    CURRENT_TIMESTAMP
),
(
    '22222222-2222-2222-2222-222222222204',
    'NK Osijek',
    'OSI',
    'Opus Arena',
    'Osijek',
    1947,
    CURRENT_TIMESTAMP
);

-- 3. Insert Season
INSERT INTO seasons (id, league_id, name, year_start, year_end, status, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    '2026/2027 Season',
    2026,
    2027,
    'ACTIVE',
    CURRENT_TIMESTAMP
);

-- 4. Enroll Teams in Season (Association entity SeasonTeam)
INSERT INTO season_teams (id, season_id, team_id, initial_seed, registration_date, status)
VALUES
(
    '44444444-4444-4444-4444-444444444401',
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222201',
    1,
    CURRENT_TIMESTAMP,
    'CONFIRMED'
),
(
    '44444444-4444-4444-4444-444444444402',
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222202',
    2,
    CURRENT_TIMESTAMP,
    'CONFIRMED'
),
(
    '44444444-4444-4444-4444-444444444403',
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222203',
    3,
    CURRENT_TIMESTAMP,
    'CONFIRMED'
),
(
    '44444444-4444-4444-4444-444444444404',
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222204',
    4,
    CURRENT_TIMESTAMP,
    'CONFIRMED'
);

-- 5. Insert Round 1
INSERT INTO rounds (id, season_id, round_number, name, start_date, end_date, status)
VALUES (
    '55555555-5555-5555-5555-555555555501',
    '33333333-3333-3333-3333-333333333333',
    1,
    'Round 1',
    '2026-08-01',
    '2026-08-03',
    'COMPLETED'
);

-- 6. Insert Completed Fixtures for Round 1
INSERT INTO fixtures (id, round_id, home_team_id, away_team_id, home_score, away_score, status, scheduled_time, postponement_reason)
VALUES 
(
    '66666666-6666-6666-6666-666666666601',
    '55555555-5555-5555-5555-555555555501',
    '22222222-2222-2222-2222-222222222201', -- Dinamo
    '22222222-2222-2222-2222-222222222202', -- Hajduk
    2,
    1,
    'FINISHED',
    '2026-08-02 18:00:00+00',
    NULL
),
(
    '66666666-6666-6666-6666-666666666602',
    '55555555-5555-5555-5555-555555555501',
    '22222222-2222-2222-2222-222222222203', -- Rijeka
    '22222222-2222-2222-2222-222222222204', -- Osijek
    1,
    1,
    'FINISHED',
    '2026-08-03 20:00:00+00',
    NULL
);

-- 7. Insert Match Events for Dinamo vs Hajduk
INSERT INTO match_events (id, fixture_id, team_id, event_type, minute, player_name, assisting_player, notes, created_at)
VALUES
(
    '77777777-7777-7777-7777-777777777701',
    '66666666-6666-6666-6666-666666666601',
    '22222222-2222-2222-2222-222222222201', -- Dinamo
    'GOAL',
    23,
    'Bruno Petkovic',
    'Martin Baturina',
    'Left footed strike from penalty area',
    CURRENT_TIMESTAMP
),
(
    '77777777-7777-7777-7777-777777777702',
    '66666666-6666-6666-6666-666666666601',
    '22222222-2222-2222-2222-222222222202', -- Hajduk
    'GOAL',
    55,
    'Marko Livaja',
    NULL,
    'Header from corner kick',
    CURRENT_TIMESTAMP
),
(
    '77777777-7777-7777-7777-777777777703',
    '66666666-6666-6666-6666-666666666601',
    '22222222-2222-2222-2222-222222222201', -- Dinamo
    'GOAL',
    89,
    'Martin Baturina',
    'Josip Misic',
    'Curling shot into top right corner',
    CURRENT_TIMESTAMP
);
