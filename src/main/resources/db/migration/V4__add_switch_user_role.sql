-- Migration: V4 — Add dev role-preview authority to existing ADMIN users.
-- Description: Grants ROLE_SWITCH_USER to every user that already has ROLE_ADMIN
--              so the SwitchUserFilter role-preview dropdown works for pre-existing
--              admin accounts. Idempotent: skips rows that already exist.
INSERT INTO authorities (username, authority)
SELECT a.username, 'ROLE_SWITCH_USER'
FROM   authorities a
WHERE  a.authority = 'ROLE_ADMIN'
  AND  NOT EXISTS (
         SELECT 1 FROM authorities b
         WHERE  b.username  = a.username
           AND  b.authority = 'ROLE_SWITCH_USER'
       );
