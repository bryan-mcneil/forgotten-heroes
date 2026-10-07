# Learning journal

One paragraph per session, in Bryan's words (grammar tidied, meaning untouched). Each step's
"What I learned" sign-off sentence lives here too, so it survives outside the pull request.

## 2026-10-07 — Step 01, Toolchain

Verified the toolchain and wrote `scripts/check-env.sh` and `docs/setup.md`. What I learned: be aware
of version differences between local and production environments, especially when using services like
AWS. Also be aware that tools can find a JDK through different paths: `java` on the PATH and
`JAVA_HOME` can point to different things and cause confusion when debugging. If local Java were 26 and
Lambda ran 25, everything would work locally and break on Lambda, and we would wonder why.
