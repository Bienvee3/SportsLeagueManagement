# Claude Code Configuration & Instructions — Sports League Management

This file is automatically loaded by **Claude Code** when launched in the repository root.

---

## 📌 Single Source of Operational Truth

> ⚠️ **MANDATORY POLICY**: All master instructions, architecture guidelines, and domain conventions are centralized in **[AGENTS.md](AGENTS.md)**.
> Claude Code must read and strictly adhere to all guidelines, security constraints, and clean code standards defined in `AGENTS.md`.

---

## ⚡ Quick Command Reference (Windows Environment)

Due to the Windows host environment, execute Maven commands via `cmd.exe` to guarantee proper wrapper path resolution:

```powershell
# Run the complete test suite (unit and integration)
cmd.exe /c "mvnw.cmd test"

# Compile the project
cmd.exe /c "mvnw.cmd compile"

# Launch Spring Boot locally
cmd.exe /c "mvnw.cmd spring-boot:run"

# Check Git status
git status
```

---

## 🛡️ Critical Operational Guardrails

1. **DO NOT PUSH**: Executing `git push` without explicit, prior user authorization is strictly forbidden.
2. **ZERO SECRETS**: Never commit passwords, tokens, API keys, or private credentials into repository files.
3. **MANDATORY VERIFICATION**: Never propose a commit without executing `mvnw.cmd test` and verifying `BUILD SUCCESS`.
4. **CONSTRUCTOR INJECTION ONLY**: Field injection via `@Autowired` on private class fields is strictly prohibited.
5. **NEVER EXPOSE JPA ENTITIES**: All web and REST communications must strictly utilize dedicated Request/Response DTOs.
6. **STATUS UPDATES**: Always update `docs/CURRENT_STATUS.md` before concluding any session.
