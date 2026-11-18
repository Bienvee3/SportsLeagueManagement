# How AI assistants were used to build this project

This is about the **development process**. The AI feature of the product is in `docs/adr/003-ai-integration.md`
and `docs/ai-evaluation.md`.

> Drafted by the assistant from the working sessions and the git history; the student reviews and corrects it
> before submission. Every item below is traceable to a commit or a session note.

## Assistants

| Assistant | Used for |
|---|---|
| Gemini (chat) | The initial plan: milestones, the role split, the tie-break and role questions (the conversation is the PDF "Sports League Management Project Setup") |
| OMP agent with Claude Sonnet (4.6 on 2026-10-31, 5.5 later) | Almost all implementation: domain model, services, security, REST, UI, tests, migrations, jobs, Docker, CI, deployment files, audits |

## Where the assistant was strongest, and what stayed with the student

Used hardest for boilerplate, tests, mapping code, configuration syntax and reading stack traces. The student
made the decisions that shape the product; the assistant proposed options and the student chose:

| Decision | Who | Result |
|---|---|---|
| Keep an audited "Correct Result" even though match events now set the score | Assistant argued it is required by the audit requirement; student agreed | `Fixture.correctResult` + `ResultAudit`, labelled "Administrative correction" |
| Remove the "View as" role switcher and use real OFFICIAL/VIEWER logins from `.env` | Student | `V7` migration, `InitialUserSeeder` with 8 environment values |
| Translate a chronicle when the language changes instead of generating a new story | Student | `match_chronicles`, `ChroniclePort.translate` |
| Mobile navigation as a bottom tab bar | Student chose among three options | `layout.html`, `app.css` |
| Quartz with a JDBC job store rather than `@Scheduled` + ShedLock | Student | `V11`, two jobs |
| No manual approval of AI chronicles: the model must deliver a correct text directly | Student | `ChronicleValidator` + one corrected retry |
| Figma prototype | Left to the student | not done by the assistant |

## Prompts that mattered

Paraphrased; the originals are in the session notes of the Obsidian vault.

1. The opening Gemini prompt: role of a senior Spring architect, the topic P39 spec, constraints (constructor
   injection only, DTOs never entities, `ddl-auto=validate`), work in milestones and explain decisions in Spanish
   while code stays English.
2. "If the chronicle is generated in one language and I switch the app language, translate the text instead of
   making a new story" → led to storing chronicles per fixture and language.
3. "Read the whole project and check whether everything the professor asked for is done" → produced the audit
   against `Student_Project_Assignments_2026-27.docx` that drove the jobs, i18n, Docker/CI and deployment work.
4. "The AI should give a clear, good report directly, not wait for approval" → led to verifying the answer
   against the data instead of adding a review workflow.

## What the assistant got wrong, and how it was found

| Wrong output | How it was found | Fix |
|---|---|---|
| Gemini model `gemini-2.5-flash` no longer existed (404) and the adapter hid the error behind a stored fallback text | Running the feature against the real provider | `gemini-3.5-flash`, `ChronicleUnavailableException`, nothing fake is stored |
| `SwitchUserFilter` with Spring Security 7 ended in a 500 (missing success/failure handlers) | Browser run | handlers added, later the feature was removed |
| An SRI hash on `bootstrap.bundle.js` was invalid, so every dropdown and tab was dead | Clicking in the browser | hash removed |
| "Testcontainers integration works" was written in the docs, but `mvnw verify -Pintegration` ran **0 tests**; when forced to run, the test failed (H2 dialect forced on PostgreSQL) | The audit against the assignment | pom properties, `@TestPropertySource`, 4/4 on PostgreSQL |
| `springdoc` was nested under `spring:` in `application.yml`, so `/api-docs` never existed | Probing the deployed URL | moved to the top level |
| Goal events could change a finished score without an audit row | Security audit | status guard in `Fixture`, only `correctResult()` changes a finished score |
| VIEWER could trigger unlimited paid AI calls | Security audit | `POST` restricted, `AiCallLimiter` |
| The AI answered with its own word counting inside the text (`(143) 4-0. (144) NK…`) | A stored chronicle and then the live evaluation | `ChronicleValidator` rejects it and retries |
| The production session cookie had no `Secure` flag | Reading the deployed response headers | `server.servlet.session.cookie.secure=true` in the `prod` profile |
| Generated tests used team codes with digits (`H1X`) that the domain rejects | The test run | letters only |
| A bulk edit left `application.yml` and `SecurityConfig` unparsable | Compilation and the tests | files rewritten whole |

## Rules followed

* Every change was verified by running the code: unit tests, then a smoke run on the real PostgreSQL, a browser or
  the container. Claims in documents were re-checked against the code, not trusted.
* Tests first for new behaviour; `mvnw verify` green before each commit; commits are small and atomic.
* No secret in the repository; the assistant never received production credentials.
