# Phase 3 — Frontend: React 19 + Vite 8 + TypeScript (Steps 37–56)

**Where you are:** the API runs on Lambda (`dev`) and on your laptop. **At the end of this phase:**
the game is playable in a browser against the dev API — tavern, drag & drop, keyboard, battle
playback with sound — with unit, component, end-to-end and accessibility tests.

Design spec: `knowledge/ux_visual_sound_design.md`. Contract: the OpenAPI file from Step 26 and
the event catalogue from Step 24. Folder map: `knowledge/architecture.md` §8. Local loop:
`docker compose up -d` · backend on `:8080` (`local` profile) · `npm run dev` on `:5173`.

---

## Step 37 — Vite + React + TypeScript scaffold
**Branch:** `step-37-frontend-scaffold` · **est 2h**

**Goal:** a running React app with linting, formatting and unit tests wired.

**You will have:** `frontend/` created with `npm create vite@latest frontend -- --template react-ts`;
`tsconfig` strict + `noUncheckedIndexedAccess`; ESLint flat config (`@eslint/js`, `typescript-eslint`,
`eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`) + Prettier; Vitest + `jsdom` +
`@testing-library/react` + `@testing-library/user-event` + `@testing-library/jest-dom`;
`vite.config.ts` with `server.proxy['/api'] = 'http://localhost:8080'`; folder skeleton from
architecture §8 with `index.ts` barrels; scripts `dev, build, preview, lint, typecheck, test, test:watch`;
CI job `frontend` (Node 26, `npm ci`, lint, typecheck, test, build); Dependabot `npm` ecosystem.

**Sensei notes**
- *What:* **Vite** serves modules instantly in dev and bundles for prod; **TypeScript** adds
  types; **ESLint** finds mistakes; **Prettier** formats; **Vitest** runs tests in a fake browser (jsdom).
- *Why strict TS:* the API types will be generated — strictness is what makes that pay off.
- *How:* `npm create vite@latest`, then add tooling; `jsx-a11y` lint rules start the accessibility habit on day one.
- *Where:* `frontend/`.

**Do this:** scaffold; delete the demo; write `App.tsx` rendering "Forgotten Heroes" and one test
(`renders title`); configure the proxy; commit `package-lock.json`.

**Verify:**
```bash
cd frontend; npm ci; npm run lint; npm run typecheck; npm test; npm run build; npm run dev   # open http://localhost:5173
```

**Commit:** `step-37: react + vite + typescript scaffold with lint and tests`

**Check your understanding:** What does the dev proxy do, and why does it make CORS a non-issue locally?

---

## Step 38 — Theme, design tokens, app shell
**Branch:** `step-38-theme` · **est 3h**

**Goal:** the game's look exists before any screen does.

**You will have:** Tailwind v4 via `@tailwindcss/vite`; `src/styles/tokens.css` with `@theme`
(colours from UX §5, spacing scale in 4 px steps, radii, shadows, z-layers), fonts via
`@fontsource-variable/pixelify-sans` and `@fontsource-variable/inter`; utilities `.pixelated`
(`image-rendering: pixelated`), `.scale-3x`; `AppShell` (header HUD slot, main, footer info bar),
`Button`, `Panel` (9-slice frame), `StatBadge` primitives with stories on a `/dev/ui` route;
dark tavern background; `prefers-reduced-motion` CSS hook (`.motion-safe`).

**Sensei notes**
- *What:* **design tokens** are named values used everywhere (`bg-wood-900`, `text-gold`); change
  one token, the whole game changes.
- *Why Tailwind v4:* tokens in CSS, no config file, tiny output; utility classes keep styling next to markup.
- *How:* `@import "tailwindcss"; @theme { --color-gold: #e8b43a; --font-display: "Pixelify Sans Variable"; }`.
- *Where:* `src/styles/`, `src/components/ui/`.

**Do this:** implement; build the `/dev/ui` page showing every primitive at 1×, 2×, 3× scale;
check text contrast with the browser's accessibility panel (≥ 4.5:1).

**Verify:** `/dev/ui` renders; `npm run build` CSS < 30 KB gz; tokens appear in DevTools as CSS variables.

**Commit:** `step-38: tailwind tokens, fonts, pixel utilities and app shell`

**Check your understanding:** Why integer-only scaling (2×/3×) for pixel art?

---

## Step 39 — Asset pipeline I: inventory and frame extraction (Python)
**Branch:** `step-39-asset-extract` · **est 4h**

**Goal:** game-ready frames for every hero, token, relic icon and FX, produced by a script.

**You will have:** `tools/assets/` with `requirements.txt` (Pillow, PyYAML), `config/assets.yaml`
(raw roots; per hero key: source set/char or monster file, trim/offset; icons by number; FX by
name), `inventory.py`, `extract_sv.py`, `extract_mon.py`, `extract_icons.py`, `extract_fx.py`,
`build.py --extract` → `tools/assets/build/frames/<key>/<anim>_<n>.png` (48×48, bottom-centre
anchored); `tools/assets/README.md`; **plus** a conversion job file in your Forgotten Kanji toolkit,
`forgotten_kanji_app/tool/derived/tf_to_elements_batch_heroes.json`, whose outputs (Elements-palette SV
hero sheets, monsters, icons, FX) land in `tool/derived/` so *both* games can use them (Q13, ADR-14).

**Sensei notes**
- *What:* raw art → normalized frames → (next step) atlases. **Normalizing** = same cell size,
  same anchor, same naming.
- *Why a script:* 36 heroes × 13 frames by hand is a week of clicking and an un-reviewable result; a
  script is a commit. Spec: `knowledge/asset_inventory_and_pipeline.md` §3–4.
- *How:* Pillow `Image.open`, `getbbox()` (pass `alpha_only=False`? — no: here we *want* the alpha
  bbox to trim transparent margins), `paste` onto a 48×48 canvas with the feet at y = 44;
  SV frames are already 48×48 — just copy/rename; monsters are trimmed from 128×134 and scaled
  **down by integer factors only if needed** (most fit in 48×48 or 64×64 — use 64 for big ones and
  record the cell size in the manifest).
- *Palette (ADR-14):* everything ships in the **Elements** palette. The animated SV heroes only exist in
  Time Fantasy colours, so they are recoloured first with your existing `gen_tf_to_elements.py --batch`
  (the artist's own colour table, recovered pixel-for-pixel from paired packs). Our `assets.yaml` raw
  roots point at `tool/derived/`, never at the raw TF packs.
- *Where:* `tools/assets/`; outputs are gitignored.

**Do this:** (1) in the Forgotten Kanji repo add `tool/derived/tf_to_elements_batch_heroes.json` — one
`{src, out}` job per SV set, monster, icon sheet and FX sheet the GDD uses, outputs under
`tool/derived/Battlers/sv/`, `tool/derived/Enemies/`, `tool/derived/Icons/`, `tool/derived/Fx/` — and run
`python tool/derived/gen_tf_to_elements.py --batch tool/derived/tf_to_elements_batch_heroes.json` from
that repo's root (read its report: unmapped colours fall back to `delta`); commit it **there**.
(2) here: write `assets.yaml` from the GDD art column with raw roots in `tool/derived/`; implement
scripts with `argparse`; `inventory.py` prints counts per source folder (sanity: 80 SV battlers, 77
monster battlers, 1,023 icons exist; we convert only what the GDD lists); run `build.py --extract`.

**Verify:** `python tools/assets/build.py --extract` → ~600 PNGs; `python -I tools/assets/inventory.py` matches the numbers above; open a few frames.

**Commit:** `step-39: asset pipeline — frame extraction`

**Check your understanding:** Why anchor frames at the bottom-centre rather than the image centre?

---

## Step 40 — `Sprite` component and the `/dev/gallery`
**Branch:** `step-40-sprite-gallery` · **est 3h**

**Goal:** sprites animate in the browser; every hero's art pick is confirmed by eye.

**You will have:** `src/sprites/Sprite.tsx` (props: `frames: string[]`, `fps`, `scale`, `flip`,
`loop`, `onEnd`; renders a `div` with `background-image` and a CSS `steps()` keyframe animation
generated per frame count, or swaps `src` via `requestAnimationFrame` for `<img>` — choose CSS
steps on a horizontal strip for performance), `useFrames(key, anim)` reading
`frontend/public/assets/frames/…` directly for now (atlases come in Step 41), `/dev/gallery`
route listing every hero/token with idle/attack/hit/fall animations side by side, the GDD art
column updated with final picks (remove every *(verify)*).

**Sensei notes**
- *What:* CSS `animation-timing-function: steps(n)` jumps between frames with zero JavaScript per frame.
- *Why a gallery:* visual QA in 2 minutes; recruiters love it; it forces the art mapping decision now.
- *How:* `@keyframes play { to { background-position: -{n*w}px 0 } }` with `steps(n)`; flip with `scaleX(-1)`.
- *Where:* `src/sprites/`, `src/dev/Gallery.tsx`.

**Do this:** implement; copy `tools/assets/build/frames` → `frontend/public/assets/frames` via
`npm run assets:copy`; walk the gallery with Bryan's eye: pick the final SV set/char for each human
hero — for the six Forgotten Kanji cameos start from the face-sheet hint in GDD §5 (e.g. Aoi =
`tf_char1` face 0 → SV set1 char1) and confirm the Elements recolour reads well at 3×; update
`assets.yaml` and the GDD; re-extract.

**Verify:** gallery shows 36 heroes + 7 tokens animating at 6 fps, pixel-crisp at 3×; no console errors.

**Commit:** `step-40: sprite component, dev gallery and final art picks`

**Check your understanding:** Why does flipping the *player's* side (not the ghost's) keep both bands facing the centre?

---

## Step 41 — Asset pipeline II: atlases, manifest, private S3
**Branch:** `step-41-asset-atlas` · **est 3h**

**Goal:** few, cacheable files; raw art kept out of the public repo.

**You will have:** `pack_atlas.py` (shelf packing into ≤ 2048×2048 PNGs: `heroes_sv`, `heroes_mon`,
`icons`, `fx`, `ui`, `portraits`), `build_manifest.py` (`manifest.json`: per key → atlas, x, y, w,
h, anchor, fps; plus content hash in file names `heroes_sv.a1b2c3.png`), `build.py --all`,
`publish.sh` (`aws s3 sync tools/assets/build/dist s3://forgotten-heroes-assets-<env>/<hash>/`),
`pull.sh` (used locally and by CI), SAM: `AssetsBucket` (private, versioning off, lifecycle
rule deleting non-current objects), IAM policy snippet for the CI role (read-only, Step 62);
`src/sprites/atlas.ts` loader replacing the per-frame loader; `Sprite` now takes a manifest key.

**Sensei notes**
- *What:* an **atlas** = many frames in one image + a JSON map; **content hashing** lets
  CloudFront cache forever (new content = new name).
- *Why private S3 (ADR-13):* the licence forbids redistribution; a public repo with packed atlases
  would be exactly that. The site still serves them normally.
- *How:* simple shelf packer (sort by height, fill rows); `hashlib.sha256` of file bytes.
- *Where:* `tools/assets/`, `infra/template.yaml`, `src/sprites/atlas.ts`.

**Do this:** implement; deploy dev (bucket created); `publish.sh dev`; `pull.sh dev` into `frontend/public/assets/`; gallery works from atlases.

**Verify:** `frontend/public/assets/` ≤ 1.5 MB total; `git status` shows no asset files; `aws s3 ls s3://forgotten-heroes-assets-dev/` lists the hash folder.

**Commit:** `step-41: atlases, manifest and private asset bucket`

**Check your understanding:** Why does hashing file names let us set `Cache-Control: max-age=31536000, immutable`?

---

## Step 42 — Typed API client
**Branch:** `step-42-api-client` · **est 3h**

**Goal:** calling the API is type-checked and testable.

**You will have:** `npm run api:types` → `openapi-typescript ../backend/target/openapi.json -o src/api/schema.d.ts`
(committed; CI checks it is up to date), `src/api/client.ts` (`fetch` wrapper: base URL from
`VITE_API_URL`, `Authorization` from an injected token getter, JSON, `ProblemDetail` → typed
`ApiError`, 409 → `VersionConflictError`), typed functions `startRun()`, `getRun(id)`,
`applyAction(id, action)`, `endTurn(id)`, `getContent()`, `getMe()`, `getLeaderboard()`; MSW
handlers in `src/test/msw/handlers.ts` with fixtures recorded from the real backend (`fixtures/*.json`).

**Sensei notes**
- *What:* generated types turn the server's contract into compile-time checks; **MSW** intercepts
  `fetch` in tests so components talk to a fake server.
- *Why fixtures from the real backend:* tests then mirror reality, and a contract change shows up as a type error here.
- *How:* `openapi-typescript`; `const run = await api.post<"/api/runs">(...)` pattern with helper types.
- *Where:* `src/api/`, `src/test/`.

**Verify:** `npm run api:types && git diff --exit-code src/api/schema.d.ts`; client tests (success, 422, 409) green.

**Commit:** `step-42: generated API types, fetch client and MSW fixtures`

**Check your understanding:** What breaks at compile time if the backend renames `crowns` to `trophies`? Who fixes what?

---

## Step 43 — `RunStore` and content hooks
**Branch:** `step-43-run-store` · **est 3h**

**Goal:** one place that holds the game state and talks to the API.

**You will have:** `src/features/run/runStore.ts` (Zustand 5: `state: RunStateDto | null`,
`pending: Set<string>`, `lastEvents`, `error`; actions `load(runId)`, `start()`, `act(action)`
(sets pending, calls API with `expectedVersion`, replaces state, stores events; on 409 → reload;
on 422 → `error` with the server `detail`), `endTurn()` → stores `battle` for the battle screen);
selectors (`useBand`, `useTavern`, `useGold`, `useHud`); `src/content/useContent.ts`
(load once, cache in memory + `localStorage` by ETag), lookups `heroById`, `relicById`; tests
with MSW for every action path including 409 and 422.

**Sensei notes**
- *What:* a **store** is shared state outside components; components subscribe to slices and
  re-render only when their slice changes.
- *Why not React Query:* our state is one document mutated by actions — a small store is clearer for learning; React Query is a fine alternative later.
- *How:* `create<RunStore>()((set, get) => ({...}))`; `useShallow` for object selectors.
- *Where:* `src/features/run/`, `src/content/`.

**Verify:** store tests green; a temporary debug page shows the band JSON after `start()` against the local backend.

**Commit:** `step-43: run store and content hooks`

**Check your understanding:** Why should the store, not components, own the `expectedVersion` logic?

---

## Step 44 — Authentication UI
**Branch:** `step-44-auth-ui` · **est 3h**

**Goal:** real sign-in with Cognito; painless local development.

**You will have:** `react-oidc-context` configured from `VITE_COGNITO_AUTHORITY`,
`VITE_COGNITO_CLIENT_ID`, `VITE_REDIRECT_URI` (PKCE, `response_type: code`, scopes
`openid email profile`, tokens in memory with silent renew); `AuthProvider` wrapper; `useToken()`
feeding the API client; `RequireAuth` route guard; `/callback` route; sign-out (Cognito logout
endpoint); `VITE_AUTH_MODE=dev` → a `DevAuthProvider` that sets header `X-Dev-User` and skips
Cognito; dev pool callback URLs include `http://localhost:5173/callback`; demo-account note on the title screen.

**Sensei notes**
- *What:* **PKCE** = the browser-safe OAuth flow: redirect to Cognito, come back with a code,
  exchange for tokens without a secret. Cognito's **Managed Login** hosts the forms.
- *Why hosted pages:* no password handling in our code; MFA/reset/verification come free.
- *How:* `AuthProvider` from `react-oidc-context`; `auth.signinRedirect()`; `auth.user?.access_token`.
- *Where:* `src/app/auth/`.

**Verify:** `npm run dev` in dev-auth mode → `/api/me` works with the header; `VITE_AUTH_MODE=cognito npm run dev` → sign in via Hosted UI → `/api/me` works with a Bearer token; refresh keeps you signed in.

**Commit:** `step-44: cognito pkce sign-in and dev auth mode`

**Check your understanding:** Why keep tokens in memory instead of `localStorage`? What is the trade-off?

---

## Step 45 — Title, Home, routing, settings skeleton
**Branch:** `step-45-home-routing` · **est 3h**

**Goal:** the first real screens and the navigation skeleton.

**You will have:** React Router 7 routes: `/` (Title: logo, "Click to start" which also unlocks
audio, demo-account hint), `/home` (Play/Continue if a run exists, How to play, Leaderboard,
Settings, Sign out), `/tavern`, `/battle`, `/results`, `/leaderboard`, `/how-to-play`,
`/callback`, `/dev/*` (only when `VITE_DEV_TOOLS=true`), `*` → not found; `SettingsDrawer`
(volume sliders, battle speed, reduced motion, keybinding list — persisted in `localStorage`);
route-level code splitting (`lazy`); page titles.

**Sensei notes:** routes are URLs → components; `Continue` reads `/api/me.activeRunId`; the title
screen's click is the browser's audio-unlock gesture (Step 50 relies on it).

**Verify:** navigate every route by mouse and keyboard; `npm run build` shows separate chunks per route.

**Commit:** `step-45: title and home screens, routes, settings drawer`

**Check your understanding:** What is code splitting and why does the battle screen deserve its own chunk?

---

## Step 46 — Tavern screen layout (static)
**Branch:** `step-46-tavern-layout` · **est 5h**

**Goal:** the tavern looks right with fake data before it does anything.

**You will have:** `TavernScreen` composed of `Hud` (hearts, crowns, turn, rank, gold, End Turn),
`BandRow` (5 slots, empty slot placeholders, front/back labels), `TavernRow` (hero slots + relic
slots, frozen overlay, prices, Roll/Freeze buttons), `HeroCard` (sprite, name, attack/health
badges, level pips, rank pips, perk badge, temporary-stat styling, "can't afford" state),
`RelicCard`, `InfoBar` (ability text of the hovered/focused card), fixtures in
`src/test/fixtures/tavern.json`; `/dev/tavern-static` route rendering the fixture.

**Sensei notes**
- *What:* **composition** — small components with clear props; the screen is just layout.
- *Why static first:* layout bugs are easier to see without network/state noise; snapshot tests freeze the look.
- *How:* CSS grid for rows; cards are `<button>`s (accessible by default) with `aria-label`
  built from the stats; the info bar is `aria-live="polite"`.
- *Where:* `src/features/tavern/`.

**Verify:** component tests: a card shows "3 / 5", level pips, frozen state; the static page matches the wireframe at 1280×720 and 1024×640; axe (dev tools) shows 0 violations.

**Commit:** `step-46: tavern screen layout with static fixtures`

**Check your understanding:** Why render cards as buttons rather than `div`s with click handlers?

---

## Step 47 — Tavern interactions wired to the API
**Branch:** `step-47-tavern-interactions` · **est 5h**

**Goal:** you can play the tavern phase for real.

**You will have:** dnd-kit `DndContext` with pointer + keyboard sensors; draggables (tavern heroes,
relics, band heroes) and droppables (band slots, band heroes for merge/target, sell zone);
`onDragEnd` → store actions (`BUY`, `MERGE`, `MOVE`, `SELL`, `BUY_RELIC` with target); click-select
fallback (select → valid targets glow → click to act → Esc cancels); `Roll`, `Freeze`, `End Turn`
buttons → actions; optimistic **pending** state (card shimmer; inputs locked for that card only);
server `422` → shake + info-bar message; `409` → silent reload + toast "Synced"; End Turn →
`endTurn()` → navigate to `/battle`.

**Sensei notes**
- *What:* drag & drop is just "which draggable ended over which droppable → which action".
  dnd-kit handles pointer math and announces moves for screen readers.
- *Why click fallback:* touchpads, accessibility, and testing (click paths are easy to test).
- *How:* `useDraggable({id, data})`, `useDroppable({id, data})`, `DragOverlay` for the lifted card;
  derive the action from `{active.data, over.data}` in one pure function `resolveDrop()` — unit-test it exhaustively.
- *Where:* `src/features/tavern/dnd.ts`, `TavernScreen.tsx`.

**Do this:** implement; tests: `resolveDrop` table test (every source×target combination → action or null); component tests with MSW for buy, sell, merge, 422 message, 409 reload.

**Verify:** play 3 turns end-to-end locally (dev auth), watch the DynamoDB admin UI update; keyboard drag (space, arrows, space) works.

**Commit:** `step-47: tavern drag and drop, selection and API actions`

**Check your understanding:** Why is `resolveDrop()` pure, and what would make it hard to test?

---

## Step 48 — Keyboard controls and accessibility pass
**Branch:** `step-48-keyboard-a11y` · **est 4h**

**Goal:** the tavern is fully playable without a mouse and understandable with a screen reader.

**You will have:** `useHotkeys` (the map in UX §4: `1–5`, `Q W E R T`, `A S`, arrows, Enter,
`F`, `R`, `X`, `Ctrl+Enter`, `Esc`, `?`, `M`), a visible selection model (`selected: {row, index}`)
shared with click-select, focus management (focus follows selection; roving `tabIndex`), ARIA
names on every card and button, live-region announcements for actions ("Bought Aoi for 3 gold.
7 gold left."), `?` overlay listing shortcuts, reduced-motion handling for all tavern animations,
axe-core run in a Vitest test (`vitest-axe`) for the tavern with fixtures.

**Sensei notes:** keyboard support is both accessibility and a power-user feature; the trick is one
selection model for mouse and keyboard so they never disagree.

**Verify:** unplug the mouse (or don't touch it): start a run, buy, move, merge, freeze, roll, sell,
end turn — all by keyboard; NVDA (free) reads cards sensibly; axe test green.

**Commit:** `step-48: keyboard controls, focus model and accessibility`

**Check your understanding:** What is a roving `tabIndex` and why is it better than making every card a tab stop?

---

## Step 49 — Battle playback engine (pure TypeScript)
**Branch:** `step-49-playback-engine` · **est 4h**

**Goal:** turn the server's event list into a timeline the UI can render — with no DOM involved.

**You will have:** `src/features/battle/playback/` — `types.ts` (events mirror `schema.d.ts`),
`viewModel.ts` (`BattleView`: both bands as arrays of `UnitView{id, heroId, attack, health,
level, perk, status: idle|attacking|hurt|fallen|summoned}`, `callout`, `result`), `reducer.ts`
(`applyEvent(view, event) → view`), `timeline.ts` (`buildTimeline(events) → Step[]` where each
step has `durationMs` by event type (UX §6), `events` grouped so simultaneous damage renders
together), `player.ts` (a small class: `play/pause/seek/setSpeed/skipToEnd`, emits `(view, step)`
via subscription, uses `performance.now()` with injectable clock), exhaustive unit tests with the
golden event log from Step 12 and a few recorded battles from the dev API.

**Sensei notes**
- *What:* the **view model** is "what should be on screen now"; the **reducer** moves it one
  event at a time; the **timeline** adds time; the **player** adds a clock. React only renders views.
- *Why:* animation bugs become unit-test failures; speed/skip are trivial; the same engine drives the `/dev` replay tool.
- *How:* a `switch` over `event.type` (TypeScript's discriminated unions give exhaustiveness via `never`).
- *Where:* `src/features/battle/playback/`.

**Verify:** `npm test` — reducer reaches the same final stats as the server's `BattleEnded` summary for 10 recorded battles; `skipToEnd()` equals playing through.

**Commit:** `step-49: battle playback engine with timeline and tests`

**Check your understanding:** Why can `skipToEnd()` be trusted to show the right final state?

---

## Step 50 — Sound
**Branch:** `step-50-sound` · **est 4h**

**Goal:** music and effects, built by script, respectful of the browser and the player.

**You will have:** `tools/assets/build_audio.py` (ffmpeg: BGM WAV → OGG q4 + M4A 96k with loop
metadata; SFX → one sprite `sfx.ogg/.m4a` + `sfx.json` offsets) and the picks from UX §7
recorded in `assets.yaml`; `src/audio/AudioManager.ts` (Howler: `playSfx(name, {pitchJitter})`,
`playBgm(track, {crossfadeMs})`, `setVolumes`, `mute`, unlock on first gesture, persist settings);
`src/audio/eventSounds.ts` (event type → SFX; ability callouts duck music); hooks in the tavern
actions and in the playback player; settings drawer sliders live.

**Sensei notes**
- *What:* an **audio sprite** = one file, many clips (fewer requests, instant playback); browsers
  block audio until a user gesture — the title screen click handles it.
- *Why OGG + M4A:* Safari lacks OGG; Chrome/Firefox prefer it. Howler picks automatically.
- *How:* `new Howl({src: ['sfx.ogg','sfx.m4a'], sprite})`; `howl.rate(1 + jitter)`.
- *Where:* `tools/assets/build_audio.py`, `src/audio/`.

**Verify:** every tavern action makes a sound; BGM crossfades tavern→battle; mute persists on reload; total audio ≤ 2.5 MB; no autoplay warnings in the console.

**Commit:** `step-50: audio pipeline, howler manager and event sounds`

**Check your understanding:** Why does pitch jitter make repeated hits sound better?

---

## Step 51 — Battle screen rendering + dev replay tools
**Branch:** `step-51-battle-screen` · **est 6h**

**Goal:** battles look alive and are easy to debug.

**You will have:** `BattleScreen` subscribing to the player; `UnitSprite` (idle loop, attack lunge
toward the centre via Motion, hit flash + shake, fall grey + drop, summon pop), `DamageNumber`
(floating, pooled), `StatBadges` animating on change, `FxLayer` (hit/slash/heal/fire/shield
frames from the FX atlas at the target position), `Callout` (ability text line, ducks music),
`Controls` (play/pause, 1×/2×/4×, skip, progress), `ResultBanner`; `/dev/replay` (paste an event
JSON → play) and `/dev/inspector` (step through events, view model side by side); route guard →
if no battle in store, load the last battle from `GET /runs/{id}/battles/{turn}`.

**Sensei notes**
- *What:* the renderer maps `UnitView.status` to CSS classes/animations; it never computes rules.
- *Why pooled damage numbers:* creating DOM per hit stutters; reuse 12 elements.
- *How:* `AnimatePresence` + `layout` for line shifts; `transform` animations only (GPU-cheap);
  sprites switch animation by `status`.
- *Where:* `src/features/battle/`, `src/dev/`.

**Do this:** implement; test with recorded battles; profile with Chrome Performance — no long
tasks > 50 ms during playback at 4×.

**Verify:** a 48-event battle plays smoothly at 1×/2×/4×; skip shows the final state instantly; `/dev/replay` plays a pasted log; reduced-motion mode still communicates every event.

**Commit:** `step-51: battle screen rendering, fx and dev replay tools`

**Check your understanding:** Why animate `transform` and `opacity` rather than `left`/`top`?

---

## Step 52 — Results and run progression
**Branch:** `step-52-results-progression` · **est 3h**

**Goal:** the loop closes: battle → result → next turn, until the run ends.

**You will have:** `ResultBanner` (VICTORY/DEFEAT/DRAW with hearts/crowns ticking),
"Continue" → store applies the returned post-battle state → `/tavern` for turn + 1;
`RunOverScreen` (`/results`): WON (10 crowns) or LOST (0 hearts) with run stats (turns, heroes
used, best battle), "Play again" (`startRun`) and "Home"; `/home` Continue works mid-run; page
refresh during battle reloads the battle log; `abandon` with confirm dialog in the settings drawer.

**Verify:** play a full run against bot ghosts on dev to a WON or LOST screen; refresh at every screen → state restored.

**Commit:** `step-52: results, run progression and run-over screen`

**Check your understanding:** Which piece of state is the source of truth after a battle — the playback view model or the server's returned run state? Why?

---

## Step 53 — `[opt]` Juice & polish
**Branch:** `step-53-polish` · **est 4h**

**Goal:** the hundred small things that make it feel good.

**You will have:** Motion `layout` transitions for band reorder and tavern refills; coin flyouts on
buy/sell; card hover lift; gold counter tick; toasts (`Synced`, errors); loading skeletons for
content/run fetch; "Waking the tavern…" message if the first API call exceeds 1.5 s (SnapStart
first hit); offline detection with retry; error boundary with "Reload" and a copyable error id;
empty states; favicon swap while battle plays.

**Verify:** no layout shift (CLS 0) in Lighthouse; all transitions disabled under reduced motion.

**Commit:** `step-53: juice, transitions, toasts and error recovery`

---

## Step 54 — `[opt]` Leaderboard and profile
**Branch:** `step-54-leaderboard-profile` · **est 3h**

**You will have:** `/leaderboard` (season selector, top 50, your rank highlighted), display-name
picker on first login (`PUT /api/me`, validation messages mirrored from the server), profile chip in the HUD.

**Verify:** finish a run → appears on the board; invalid name → inline error.

**Commit:** `step-54: leaderboard and profile screens`

---

## Step 55 — `[opt]` Onboarding, How to play, responsive rules
**Branch:** `step-55-onboarding-responsive` · **est 3h**

**You will have:** first-run callouts (3 steps, `localStorage` flag), `/how-to-play` page generated
from the GDD rules section with pictures, responsive breakpoints from UX §10 (2× sprites below
1100 px, rotate prompt in portrait), 44 px touch targets, touch drag tested on a tablet or DevTools emulation.

**Verify:** DevTools device emulation: iPad landscape playable; iPhone portrait shows the rotate card.

**Commit:** `step-55: onboarding, how-to-play and responsive behaviour`

---

## Step 56 — Frontend QA suite + Phase 3 review
**Branch:** `step-56-frontend-qa` · **est 5h**

**Goal:** the robot plays the game.

**You will have:** backend `local`-only endpoint `POST /dev/seed {turn, crowns, hearts, band}` (ArchUnit-guarded
like dev auth) for fixtures; Playwright config (Chromium; WebKit in nightly) with `webServer`
starting backend + frontend; E2E specs: `signin-dev.spec` (dev auth), `first-turn.spec` (start →
buy → reorder → end turn → battle plays → continue), `keyboard.spec` (same with keys only),
`run-over.spec` (seeded at 9 crowns → win), `error.spec` (409 reload); `@axe-core/playwright`
on Title, Home, Tavern, Battle, Results; visual snapshots at 1280×720; Lighthouse CI
(`lhci autorun`) with budgets from UX §9 against `npm run preview`; CI: `e2e` job on label/nightly,
`lighthouse` job on frontend changes; `docs/reviews/phase-3.md`; tag `phase-3-complete`.

**Sensei notes:** E2E tests are slow and precious — few, meaningful flows; fixtures via a seed
endpoint keep them fast; visual snapshots catch "it looks wrong" regressions that unit tests cannot.

**Verify:** `npm run e2e` green locally (Docker + backend running); `npm run lighthouse` meets budgets; CI nightly green.

**Commit:** `step-56: playwright e2e, axe, visual snapshots, lighthouse budgets and phase 3 review`

**Check your understanding:** Why seed test fixtures through a local-only endpoint instead of playing 10 turns in every test?
