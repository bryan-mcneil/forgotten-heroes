# knowledge — research and design behind the plan

## What lives here

The thirteen documents the 77-step plan was built from: the Super Auto Pets deep dive, the tech-stack
research, the cost estimate, the game design document (the rulebook, authoritative), the architecture
with its 15 ADRs, the asset inventory, the UX and sound spec, the testing strategy, project management,
the environment audit, the learning path, the glossary and the answered questions. The index with a
one-line summary of each file is [`docs/README.md`](../docs/README.md).

## What must NOT live here

- Step instructions (`plan/`), documents written as the build goes (`docs/`), or code.
- Silent edits that change a decision. A design change adds an ADR row to `architecture.md` §13 and a line
  to the changelog in `PLAN.md`; a new question is appended to `open_questions.md` §F with a log row.
  Everything else in here is read, not rewritten.

## How do I use it

`CLAUDE.md` lists which sections to read in each phase. Section numbers (§) are the `##` headings inside
each file: read the section, not the file.
