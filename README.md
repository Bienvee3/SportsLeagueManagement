# P39 Sports League Management

**Rapid Development of Java Applications Using Programming Frameworks**  
Algebra Bernays University — Academic Year 2026/2027  
**Student:** Doalto Cavada Bienvenido  

---

## 📌 Project Overview

A robust multi-tier web application designed to manage sports leagues, schedules, match events, dynamic standings, and historical result corrections with full audit logging and AI-assisted match report generation.

### Aggregate Structure

$$\text{League} \longrightarrow \text{Season} \longrightarrow \text{Round} \longrightarrow \text{Fixture} \longrightarrow \text{Team} \quad \text{and} \quad \text{Fixture} \longrightarrow \text{MatchEvent}$$

### Advanced Requirements

1. **Round-Robin Fixture Generation**: Home/away balance algorithm with no team playing twice in the same round.
2. **Dynamic Standings with Tie-Breaks**: Configurable tie-break criteria applied in strict order:
   - Total Points
   - Head-to-Head Points
   - Head-to-Head Goal Difference
   - Overall Goal Difference
   - Goals Scored
3. **Audited Result Corrections**: Retroactive score correction recalculating affected standings with an immutable audit trail (`ResultAudit`).
4. **AI Match Reports**: Bilingual (English & Croatian) fact-strict match chronicles generated via Ports & Adapters architecture.

---

## 🛠️ Technology Stack

* **Language & Runtime**: Java 25 LTS
* **Framework**: Spring Boot 4.1
* **Persistence & ORM**: Spring Data JPA over Hibernate 7
* **Database**: PostgreSQL 17 (managed locally with Docker Compose)
* **Migrations**: Flyway (`validate` mode in production)
* **Connection Pool**: HikariCP (deliberately tuned)
* **Security**: Spring Security 7 (Form Login, CSRF, BCrypt, 3 distinct roles: `ADMIN`, `OFFICIAL`, `VIEWER`)
* **Frontend**: Thymeleaf with layout dialect, Bean Validation, POST-Redirect-GET, and i18n
* **API Documentation**: OpenAPI 3.x with SpringDoc
* **Testing**: JUnit 6, Mockito, Testcontainers, JaCoCo ($\ge 80\%$ service line coverage)

---

## 🚀 Getting Started

### Prerequisites

* JDK 25 installed and on `PATH` — [Microsoft Build of OpenJDK 25](https://learn.microsoft.com/en-us/java/openjdk/download)
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

---

### 1 — First-time setup: create your `.env` file

Copy the example and fill in your values:

```bash
copy .env.example .env
```

Edit `.env` — the minimum required:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=choose-a-password
```

> `.env` is in `.gitignore` and is **never committed**. Keep your credentials there.

---

### 2 — Start the database

```powershell
docker compose up -d
```

This starts a PostgreSQL 17 container on `localhost:5432`.  
Flyway runs all migrations automatically on first app start.

---

### 3 — Start the application

**Windows (PowerShell) — recommended:**

```powershell
.\run.ps1
```

This script loads your `.env` automatically and starts the app.  
If PowerShell blocks scripts, run this once first:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

**Alternative (CMD):**

```cmd
set ADMIN_USERNAME=admin
set ADMIN_PASSWORD=choose-a-password
mvnw.cmd spring-boot:run
```

---

### 4 — Open in browser

| URL | Description |
|---|---|
| `http://localhost:8080/login` | Login page |
| `http://localhost:8080/` | Dashboard |
| `http://localhost:8080/leagues` | League management |
| `http://localhost:8080/teams` | Team management |
| `http://localhost:8080/swagger-ui` | REST API (Swagger UI) |
| `http://localhost:8080/api-docs` | OpenAPI JSON |

Log in with the `ADMIN_USERNAME` / `ADMIN_PASSWORD` you set in `.env`.

---

### 5 — Stop the application

Press `Ctrl+C` in the terminal where the app is running.

---

### 6 — Stop the database

```powershell
docker compose down
```

> To also **delete all data** (full reset): `docker compose down -v`

---

### Running Tests

```powershell
# Standard test suite (325 tests, no Docker required)
.\mvnw.cmd test

# Integration tests with real PostgreSQL via Testcontainers (requires Docker)
.\mvnw.cmd verify -Pintegration
```

---

## 📖 Architecture Decision Records (ADRs)

* [ADR 001: Relational Persistence Model with PostgreSQL, Spring Data JPA, and Flyway](docs/adr/001-persistence-model.md)
* [Development Guidelines & Clean Code Standards](docs/development-guidelines.md)