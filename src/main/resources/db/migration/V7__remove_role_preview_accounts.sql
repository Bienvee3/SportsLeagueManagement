-- Migration: V7 — Remove dev role-preview artifacts.
-- Description: Real OFFICIAL/VIEWER accounts are now created from environment variables,
--              so the SwitchUserFilter authority and the hard-coded test accounts
--              (known passwords) are removed.
DELETE FROM authorities WHERE authority = 'ROLE_SWITCH_USER';
DELETE FROM authorities WHERE username IN ('test-official', 'test-viewer');
DELETE FROM users       WHERE username IN ('test-official', 'test-viewer');
