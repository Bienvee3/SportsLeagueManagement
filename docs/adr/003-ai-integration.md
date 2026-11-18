# ADR 003: AI match chronicles behind a port, verified before they are shown

## Status
Accepted

## Date
2026-11-19

## Context

Topic P39 requires an AI feature: *generate a short match report from the recorded events and the standings
effect, in Croatian and English, with every scoreline taken from the data.* Three properties follow:

1. **Facts must come from the data.** A language model can write fluent text that contains a wrong score.
2. **Two languages.** A match that is read in English and then in Croatian must tell the same story.
3. **The provider is paid or quota-limited and slow.** It must not be called by accident, must not block
   database connections, and the application must keep working when it is down. Tests must never call it.

## Decision

**1. Port and adapter.** The domain owns `ChroniclePort` (`generate`, `translate`); `GeminiChronicleAdapter`
(Spring AI 2.0.2, Google GenAI, model `gemini-3.5-flash`) implements it. `StubChronicleAdapter` is registered
when `spring.ai.model.chat` is not `google-genai` (all tests, `SPRING_AI_MODEL_CHAT=none`). The stub throws
`ChronicleUnavailableException` instead of returning canned text, so a fake report can never be stored or
shown; every screen still works without the provider and the chronicle endpoints answer 503.

**2. The model gets facts only through a request record.** `ChronicleRequest` carries the teams, the final
score, the events in order and the *table effect* (position and points of both teams before and after the
match, computed by `StandingsCalculator` from the finished matches with and without this one). The prompt is
always English; the output language is an instruction.

**3. Verify, do not trust.** `ChronicleValidator` checks every answer against the request before anything is
stored: the final score must be stated, every scoreline in the text must be the final score or a running score
derivable from the events, no drafting notes may be present (word counts such as `(144)`, markdown, "Here is…"),
and the length must be sensible. A rejected answer is retried once with the reasons added to the prompt; a
second rejection stores nothing and the request fails with 503. Translations are checked to keep exactly the
same set of scorelines. There is no manual approval step: the check replaces it, so what a user reads has
already passed.

**4. One chronicle per fixture and language, translated rather than regenerated.** `match_chronicles` keeps one
row per (fixture, language). Switching language translates the stored text, so both languages tell the same
story. Recording or correcting a result, or changing events, deletes the stored chronicles because they would
describe the old match.

**5. Cost and failure containment.**
* Only `ADMIN` and `OFFICIAL` can generate or translate; `VIEWER` reads stored text (`GET` never calls the provider).
* The provider call runs between two short transactions (`ChronicleStore`) so no pooled connection is held
  while waiting for it, and nothing is deleted before the provider has answered.
* `AiCallLimiter`: at most `app.ai.max-calls-per-hour` provider calls for the whole application, answering 429
  beyond it. Every attempt counts, including the retry of a rejected answer.
* A custom `com.google.genai.Client` sets the HTTP timeout (60 s) and one SDK-level attempt, because Spring AI
  2.0.2 has no property for either and the SDK default retried silently.

## Alternatives rejected

| Alternative | Why not |
|---|---|
| Call the Gemini REST API directly with `RestClient` | Loses Spring AI's model abstraction and options binding; the port already isolates the provider, so switching providers is one adapter either way |
| Manual approval of every chronicle by staff | Adds a workflow and delays what viewers see. The student decided the model should deliver a clear, correct report directly, so the verification above does the reviewing in code |
| Template text without a model (`"{home} beat {away} {h}-{a}"`) | Always correct, but it does not satisfy "an AI feature" |
| Regenerate on every language switch | Two languages would describe different stories (different invented details) and it doubles the paid calls |
| Return a canned text from the stub | A fake chronicle could be stored and presented as real; failing loudly is safer |
| Validate only by prompt ("do not invent details") | Observed not to hold: see Consequences |

## Consequences

**Observed in a live run (2026-11-19, `gemini-3.5-flash`).**
* One answer was a fragment of the model's own word counting (`to (143) 4-0. (144) NK (145) Lokomotiva…`), the
  same defect seen earlier in a stored chronicle; another had 54 words when 150-200 were requested. Both were
  caught by the validator, which is why verification exists.
* Answers took 16 to 29 s; the original 30 s timeout cancelled one. The timeout is now 60 s.
* The free tier allows **20 requests per day per model**. A retry or an SDK-level retry spends one of them, which
  is why the SDK no longer retries. `app.ai.max-calls-per-hour` (30) is above that daily limit, so on a free key
  the provider's own 429 arrives first and is reported as 503.

**Accepted limits.**
* The validator checks numbers, notes and length. It cannot detect an invented *non-numeric* fact (a made-up
  scorer, a wrong adjective). The prompt forbids it and `docs/ai-evaluation.md` records how often it happens, but
  nothing blocks it.
* The in-memory limiter resets on restart and is per instance.
* Up to two paid calls per chronicle; a rejected pair costs two and produces no text.
* Non-determinism: the same match gives different text each time; tests therefore assert properties of the
  validator and the prompt, never the model's words.
