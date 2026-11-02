-- =============================================================================
-- Migration: V3__create_security_tables.sql
-- Description: Users and authorities tables for Spring Security JdbcUserDetailsManager
-- Database: PostgreSQL 17 / H2 (test profile, PostgreSQL compatibility mode)
-- =============================================================================

CREATE TABLE users (
    username VARCHAR(50) NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username)
);

CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);
