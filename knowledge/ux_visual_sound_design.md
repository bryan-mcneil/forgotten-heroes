# UX, Visual & Sound Design

> How the game looks, feels, and sounds — and the rules that keep it simple. Design target:
> **1280×720 desktop**, scaling down to 1024 wide and up to 4K with integer pixel scaling.

---

## 1. Screen flow

```
[Corgi Space Cadet intro] → [Title] → (Sign in) → [Home] ──► [Leaderboard]
                                          │           └──► [How to play] / [Settings]
                                          ▼
                                   [Tavern]  ⇄  [Battle]  → [Result banner] ─┐
                                      ▲                                         │ turn+1
                                      └─────────────────────────────────────────┘
                                                      │ 10 crowns / 0 hearts
                                                      ▼
                                                 [Run over] → Home
```
Rules: the run is always resumable (server state); refreshing the page lands you back in the
tavern or mid-battle-replay; there is never a modal you cannot escape with `Esc`.

---

## 2. Tavern screen (wireframe, 1280×720)

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│ ♥ 4/5   👑 3/10   Turn 5 · Rank III        [⚙]  [?]        GOLD  ◉ 7        [END TURN ⏎] │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                              YOUR BAND  (front →)                                    │
│   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐                              │
│   │ 5    │   │      │   │ 2    │   │      │   │ 3    │        [ SELL ◉ ]  ← drop zone │
│   │ hero │   │ (+)  │   │ hero │   │ (+)  │   │ hero │                              │
│   │ 4 ♥7 │   │      │   │ 2 ♥5 │   │      │   │ 3 ♥4 │                              │
│   └──────┘   └──────┘   └──────┘   └──────┘   └──────┘                              │
│   back                                                        front (fights first)   │
├──────────────────────────────────────────────────────────────────────────────────────┤
│   TAVERN                                               [ROLL ◉1  (R)]  [FREEZE (F)]  │
│   ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐     ┆    ┌──────┐ ┌──────┐                    │
│   │ ◉3   │ │ ◉3 ❄ │ │ ◉3   │ │ ◉3   │     ┆    │ relic│ │ relic│                    │
│   │ hero │ │ hero │ │ hero │ │ hero │     ┆    │  ◉3  │ │  ◉1  │                    │
│   └──────┘ └──────┘ └──────┘ └──────┘     ┆    └──────┘ └──────┘                    │
├──────────────────────────────────────────────────────────────────────────────────────┤
│  Aoi    · Rank I · Lv 1 ●○○   "Fall → Give one random ally +1/+1."   (hover/focus) │
└──────────────────────────────────────────────────────────────────────────────────────┘
```
* **Hero card** (144×176 at 3×): sprite (idle animation), attack badge bottom-left (sword icon,
  warm orange), health badge bottom-right (heart, green), level pips top-right, rank pips under
  the name, perk badge top-left, frozen = frosted overlay + ❄, temporary stat values rendered in
  light blue with a small "⟳" to show they expire.
* **Info bar** at the bottom always shows the focused/hovered card's ability text — no hidden info,
  no tooltips that cover the board.
* **Cost** is printed on tavern cards; cards you cannot afford are desaturated and the gold
  counter shakes if you try.

## 3. Battle screen

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  You  ♥4 👑3                 TURN 5 BATTLE                  Ghost: "Bryan's Band" 👑3   │
│                                                                                       │
│      ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐        ⚔        ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐               │
│      │  │ │  │ │  │ │  │ │  │  →   clash   ←  │  │ │  │ │  │ │  │ │  │               │
│      └──┘ └──┘ └──┘ └──┘ └──┘                 └──┘ └──┘ └──┘ └──┘ └──┘               │
│      back ─────────────── front               front ─────────────── back              │
│                                                                                       │
│  ▶ "Archer: Start of battle → 1 damage to Orc"      ← event callout (auto-fades)      │
│  [▶ 1× ] [2×] [4×] [⏭ skip]                                 event 12 / 48 ▰▰▰▰▱▱▱▱     │
└──────────────────────────────────────────────────────────────────────────────────────┘
```
* Player's band on the **left** facing right; ghost on the right facing left; the two fronts meet
  in the middle (SAP convention, left-to-right reading).
* Damage numbers float up; stat badges tick to new values; fallen units grey out and drop; summons
  pop in with dust FX; abilities flash the source card and show a callout line.
* Speed and skip are always available; a result banner (VICTORY / DEFEAT / DRAW) ends the replay
  with the hearts/crowns change animated.

---

## 4. Input model

### Mouse / touch
* **Drag** a tavern hero onto an empty band slot → buy. Onto a same hero → buy + merge. Onto a
  band hero of a different kind → swap into that slot (reorder).
* **Drag** a band hero to another band slot → reorder; onto the **SELL** zone → sell.
* **Drag** a relic onto a hero → apply. Instant tavern-wide relics are applied by dropping anywhere on the band.
* **Click** = select (card lifts, valid targets glow). Click a target = act. Click elsewhere = cancel.
  So everything can be done without dragging.
* **Right-click / long-press** a card → inspect panel (bigger art, full text at all levels).

### Keyboard (first-class, not an afterthought)
| Key | Action |
|---|---|
| `1–5` | select band slot 1–5 · `Q W E R T` select tavern hero slot 1–5 · `A S` tavern relic 1–2 |
| `←/→` | move selection within the current row · `↑/↓` switch row |
| `Enter` / `Space` | act with the selection (buy/place/apply/merge) · second Enter confirms a swap |
| `F` | freeze/unfreeze the selected tavern item |
| `R` | roll · `X` sell the selected band hero · `Esc` cancel selection / close dialog |
| `⏎ End` (or `Ctrl+Enter`) | End turn (confirmation only if gold ≥ 3 is unspent) |
| Battle: `Space` pause/play · `1 2 3` speed · `S` skip · `Esc` back after result |
| `?` | shortcut overlay · `M` mute · `Tab` cycles focusable controls as normal |

dnd-kit's keyboard sensor announces "Picked up Aoi. Press arrow keys to move, Enter to drop."

---

## 5. Visual language

* **Palette** = FinalBossBlues' **Elements** palette (deeper shadows than Time Fantasy; every sprite is converted to it, ADR-14); UI panels in dark oak browns (`#2b1d16`, `#3d2a1f`),
  parchment text (`#f1e3c6`), gold (`#e8b43a`), attack orange (`#e2703a`), health green
  (`#5fb26a`), temporary stat blue (`#7fc4e8`), frozen ice (`#a9dcf3`), danger red (`#c6423b`).
  All defined once as CSS variables (`--fh-gold`) and Tailwind theme tokens.
* **Typography**: `Pixelify Sans` for headings/numbers (reads well at small sizes), `Inter` for
  body text in info bars and menus (pixel fonts hurt for paragraphs). Minimum 14 px body.
* **Pixel discipline**: 3× scale; all sprite positions snap to whole pixels; no fractional scaling;
  `image-rendering: pixelated`; UI 9-slice frames also pixel-art.
* **Hierarchy**: the band is the hero of the tavern screen (largest), the tavern row second, HUD
  third. One primary button (END TURN) in the brand gold.

## 6. Motion
| Thing | Duration | Easing | Reduced motion |
|---|---|---|---|
| Card hover lift | 120 ms | ease-out | none |
| Buy/place (card flies to slot) | 250 ms | ease-in-out | instant |
| Reorder (layout animation) | 200 ms | spring (Motion `layout`) | instant |
| Gold change (counter ticks, coin flies) | 300 ms | ease-out | number only |
| Clash (lunge + recoil) | 450 ms | custom | 150 ms fade |
| Damage number | 300 ms rise + fade | ease-out | static 300 ms |
| Fall | 400 ms (grey + drop) | ease-in | fade |
| Summon | 350 ms (scale 0→1 + dust) | back-out | fade |
| Ability callout | 500 ms hold | — | same |
Rule: nothing blocks input in the tavern; in battle the timeline is the clock.

## 7. Sound design
* **Mix**: master / music / effects sliders (0–100, default 70/50/80), persisted in `localStorage`;
  `M` mutes; first user gesture unlocks audio (browser policy) — the title screen's "Click to start".
* **BGM** (OGG + M4A fallbacks, loop points): title & home `adventurous_music`; tavern
  `beer_and_ale` (fits a tavern); battle `battle_music_one`/`two` (alternate per turn); victory
  sting `battle_victory`; defeat `you_lose_game_over` (shortened). Crossfade 600 ms between screens.
* **SFX** (one Howler sprite): `ui_hover`, `ui_click`, `buy` (coin clink), `sell` (coin pour),
  `roll` (card shuffle), `freeze`/`unfreeze` (ice), `merge` (chime), `level_up` (fanfare short),
  `end_turn` (horn), `clash` (impact1), `hurt` (thud), `fall` (low drum), `summon` (pop),
  `ability` (sparkle), `perk_gain`, `shield` (Ward/Aegis), `fire` (Flame Oil), `win`, `lose`, `draw`,
  `error` (soft buzz). Pitch-randomize ±5 % on repeated hits to avoid machine-gun sound.
* **Ducking**: music −6 dB during ability callouts and the result banner.

## 8. Accessibility (ship-blocking, tested with axe + a keyboard-only run)
* Fully playable with keyboard; visible focus ring (gold, 2 px); logical tab order.
* ARIA: cards are buttons with accessible names ("Aoi, attack 2, health 2, level 1, costs 3 gold");
  a polite live region narrates battle events at reading pace ("Archer deals 1 to Orc").
* Colour is never the only signal (icons + numbers for stats, ❄ for frozen, ⟳ for temporary).
* Contrast ≥ 4.5:1 for text; `prefers-reduced-motion` honoured (table above); speed control; no
  flashing above 3 Hz.
* Text scales with browser zoom (rem units); layout holds to 200 % zoom at 1280 wide.

## 9. Performance budgets (Lighthouse CI enforces)
| Metric | Budget |
|---|---|
| JS (gzipped, initial) | ≤ 250 KB |
| Initial assets (atlases needed for home + tavern) | ≤ 1.0 MB; battle FX & BGM lazy-loaded |
| Largest Contentful Paint | ≤ 2.0 s on "Slow 4G" preset |
| Interaction to next paint | ≤ 100 ms |
| Frame rate during battle | 60 fps on an integrated-GPU laptop |
| Lighthouse scores | Performance ≥ 90 · Accessibility ≥ 95 · Best practices ≥ 95 |

## 10. Responsive behaviour
* ≥ 1280: 3× sprites, full layout. 1024–1279: 3× sprites, tighter gutters. 768–1023 (tablet
  landscape): 2× sprites. < 768 portrait: "Rotate your device or use a larger screen" card with the
  leaderboard still readable. Touch targets ≥ 44 px.

## 11. Onboarding & copy
* First run: three callouts (Buy → Arrange → End Turn), dismissible, never shown again (localStorage).
* "How to play" page = GDD §4 in plain words with pictures; glossary tooltips on key words.
* Tone: warm, short, tavern-flavoured ("The tavern keeper restocks for 1 gold.") — confirmed lighthearted (Q8).
* Names: cameo heroes read "Aoi — the Novice Scribe" with the kanji 葵 as a small subtitle (system font fallback; pixel fonts lack kanji). Others read "Farmhand".
* Errors: human sentences from the server's `detail` field plus what to do ("Reloading your run…").
* Blocked page (`blocked.html`, served by CloudFront to visitors outside the US — Q27): one static card, no
  JS, same palette. *"The tavern only opens its doors from the United States for now. Travelling, or on a
  VPN? Switch it off and knock again."* Plus a plain line for recruiters: *"Portfolio and source:
  bryanmcneil.pro · github.com/bryan-mcneil/forgotten-heroes"* — the block must never hide the work.

## 12. Dev tools (`/dev`, hidden in prod builds)
Sprite gallery with every hero/animation; "play battle from JSON" (paste an event log); event
inspector (step through events, see the view model); content table (all heroes/relics with
images). These make visual QA a 2-minute job and double as recruiter eye-candy.
