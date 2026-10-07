# frontend — the game in the browser (React 19, Vite 8, TypeScript)

## What lives here

- The React app: the title, tavern, battle and results screens; components such as `HeroCard` and
  `BandRow` (Step 46); the typed API client generated from `openapi.json` (Step 42); the `RunStore`
  (Step 43); the battle playback engine that turns engine events into a timeline (Step 49); sound
  (Step 50); keyboard and accessibility work (Step 48); Vitest and Playwright tests (Steps 37, 56).
- The folder map is `knowledge/architecture.md` §8; the visual and sound spec is
  `knowledge/ux_visual_sound_design.md`.

## What must NOT live here

- Rules or rules data. The client never recomputes a rule; heroes and relics come from `GET /api/content`
  and every move is validated by the server.
- Art and audio in git (ADR-13). `public/assets/` is gitignored and filled by the asset pipeline in
  `tools/` from a private S3 bucket.
- Secrets. The only build-time configuration is the public API URL and the Cognito client id.

## How do I run it

TODO Step 37: `cd frontend && npm run dev` serves on `:5173` and proxies `/api` to the backend on
`:8080`; `npm test` runs every unit test; `npx vitest run src/<path>.test.ts` runs one file.
TODO Step 56: `npm run e2e` (Playwright) and `npm run lighthouse` (performance budgets).
