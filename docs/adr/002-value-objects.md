# ADR 002: Value Objects as Java Records with JPA AttributeConverter

## Status
Accepted

## Date
2026-10-31

## Context

Domain entities were using raw Java primitives (`String slug`, `String shortCode`, `int matchMinute`) to represent domain concepts that carry non-trivial invariants. The only enforcement was at the database level (CHECK constraints, UNIQUE indexes), meaning:

1. An invalid domain object could be constructed in memory without any error.
2. Validation arrived late — at persistence time, not at object construction time.
3. The type system offered no distinction between "a league slug" and "any String".

This violates the DDD principle of making illegal states unrepresentable.

## Decision

Introduce **Value Objects as Java Records** for domain primitives with non-trivial invariants. The compact constructor of the record enforces the invariant at construction time.

| VO | Previous type | Invariant enforced |
|---|---|---|
| `Slug` | `String` | Pattern `[a-z0-9]+(-[a-z0-9]+)*`, max 100 chars |
| `TeamCode` | `String` | Pattern `[A-Z]{2,10}` (uppercase alpha) |
| `MatchMinute` | `int` | Range 1–130 (mirrors DB CHECK constraint) |
| `Score` | `int home, int away` | Both ≥ 0 (parameter record, not persisted) |

For VOs that map to persisted columns, implement `jakarta.persistence.AttributeConverter<VO, DbType>` with `autoApply = true` in `infrastructure.persistence.converter`. This keeps the converter out of the domain layer and respects the dependency rule (domain does not import infrastructure).

Java Records are chosen over classes because they provide: value-based `equals()`/`hashCode()`, compact validation syntax, immutability, and readable `toString()` — without boilerplate.

## Why only these four VOs

| Candidate | Decision | Reason |
|---|---|---|
| `Slug` | ✅ Keep | Non-trivial regex + max length. Used as League's business key and in URLs. |
| `TeamCode` | ✅ Keep | Non-trivial pattern (alpha-only, length bounds). Used in Fixture equality. |
| `MatchMinute` | ✅ Keep | Mirrors DB CHECK constraint. Prevents silent domain corruption. |
| `Score` | ✅ Keep | Parameter type for result recording and correction. Prevents negative scores. |
| `LeagueName`, `TeamName`, etc. | ❌ Drop | Plain non-blank Strings. `@NotBlank` Bean Validation at DTO boundary is sufficient. |
| Year/YearRange | ❌ Drop | Simple `yearStart < yearEnd` check belongs in the Season constructor. |

## Consequences

- **Positive**: Illegal states caught at object construction, not at persistence. Richer type safety in method signatures.
- **Positive**: Business key equality (`League.equals()`, `Team.equals()`) naturally delegates to Record value equality — no manual implementation changes.
- **Neutral**: `getSlug()` returns `Slug`, not `String`. Callers needing the String call `.value()`. One `DomainModelTest` assertion updated.
- **Neutral**: `MatchEvent.equals()` changed from `matchMinute == other.matchMinute` (primitive `==`) to `Objects.equals(matchMinute, other.matchMinute)` (object equality).
- **Negative**: One additional concept to explain per VO. Mitigated by clear naming and Javadoc.
- **Database schema**: No change. Converters map `Slug → VARCHAR`, `TeamCode → VARCHAR`, `MatchMinute → INTEGER` — same column types as before.
