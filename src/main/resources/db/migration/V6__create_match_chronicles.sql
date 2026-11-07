-- Migration: V6 — Store AI match chronicles per fixture and language.
-- Description: Lets a language switch reuse or translate an existing chronicle instead of
--              generating a new story. Rows disappear with their fixture.
CREATE TABLE match_chronicles (
    id          UUID PRIMARY KEY,
    fixture_id  UUID NOT NULL,
    language    VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_chronicles_fixture FOREIGN KEY (fixture_id) REFERENCES fixtures(id) ON DELETE CASCADE,
    CONSTRAINT uk_match_chronicles_fixture_language UNIQUE (fixture_id, language)
);
CREATE INDEX idx_match_chronicles_fixture_id ON match_chronicles(fixture_id);
