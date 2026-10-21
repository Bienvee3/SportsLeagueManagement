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

* JDK 25 installed and available on `PATH`.
* Docker & Docker Compose (optional for local database container).

### Running PostgreSQL locally

```bash
docker compose up -d
```

### Running the Application

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Unix/Linux
./mvnw spring-boot:run
```

### Running Tests

```bash
.\mvnw.cmd test
```

---

## 📖 Architecture Decision Records (ADRs)

* [ADR 001: Relational Persistence Model with PostgreSQL, Spring Data JPA, and Flyway](docs/adr/001-persistence-model.md)
* [Development Guidelines & Clean Code Standards](docs/development-guidelines.md)