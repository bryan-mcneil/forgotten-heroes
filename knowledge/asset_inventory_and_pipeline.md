# Asset Inventory & Pipeline

> What art and audio you own, exactly which files Forgotten Heroes uses, how they are sized, and how
> raw packs become web-ready atlases. Measurements were taken by reading the PNG headers of your
> files, so the numbers are exact.

---

## 1. Inventory (what exists on this machine)

### 1.1 `C:\Users\xbmcn\Documents\Code\kanji-rpg\art\timefantasy` — the Time Fantasy library (904 MB, 16,440 PNGs)
| Pack | What it is | Why it matters to us |
|---|---|---|
| `tf_svbattle/` | **80 animated side-view battlers** (7 "sets" × 8 heroes + 3 military sets × 8). Two formats: RPG Maker MV sheets `RMMV/sv_actors/*.png` at **1296×864** (9 cols × 6 rows of 144 px cells = 3× scale) and **`singleframes/<set>/<n>/*.png` at native 48×48** | ⭐ **Primary hero art.** Each hero has 13 animations × 3 frames: `idle1, idle2, walk, atk1, atk2, bow, magic, item, cheer, crouch, hit, status` + single `down` (fallen) |
| `tf_svbattle/RMMV/system/States.png` | 768×960 status-effect animations | perk/status indicators |
| `tf_monsters_assetpack/TimeFantasy_Monsters/Battlers/*.png` | **77 monster battlers**, single frame on a 128×134 canvas (orc1–16, golem1–2, elemental1–4, dknight1–2, wizard1–4, demon, minotaur, phoenix, treant, lick, skeleton, skull1–2, slime, snake, spider, turtle, undead, wasp, wolf1–2, worm, rat, scorpion, bat1–2, bird1–3, boar, cacto, crab, crocodile, elk, frog1–2, fungusaur, ghost1–2, imp, kobold1–2, lizardman1–2, mimic1–2, octo, plant, raptor1–2, scrub, shroom1–2, armadillo) | ⭐ **Monster heroes & tokens** (single pose; animated by motion, see §4) |
| `tf_monsters_assetpack/TimeFantasy_Monsters/1x/monster_*.png`, `orc*.png`, `chara6-8.png` | walking sprite sheets 312×288 (8 chars × 3×4 frames of 26×36) | optional 2–3 frame "breathing"/walk for monsters |
| `timefantasy_characters/` | 32 heroes + 24 military + 16 NPC walking sheets (312×288; frames 26×36) + `frames/` individual PNGs + emotes + 8 chests | NPCs for the tavern keeper, emotes for reactions |
| `tf-faces-6.11.20/transparent/1x/*.png` | 13 face sheets at 192×96 = 8 faces of **48×48** each (char1–8, military1–3, orc1–2) | hero portraits in cards/tooltips |
| `icons_12.26.19/fullcolor/individual_32x32/*.png` | **1,023 icons** at 32×32 (also 16 & 24); categories: UI/HUD, skills/magic/status, potions/herbs/food, general items, weapons, shields/armor, banners, crafting | ⭐ relic icons, perk badges, UI (gold, hearts, crowns) |
| `pixel_animations_gfxpack/` | FX sheets and `individual_frames/`: arrow, claw_bite, clock, darkness, diamond, dust, earth1-2, explosion, fire, heal, holy, ice, impact1-2, lightning, object, smoke, status_1-2, water, weapons_1-3, wind | ⭐ battle FX (hits, heals, summons, perks) |
| `tf_animals/`, `TimeFantasyAnimals2/` | animal sprite sheets (horse, wolves not here but in monsters, lots of fauna) | War Horse token (`horse1.png`) |
| `tf_dwarfvelf_v1.2/` | dwarf & elf character sheets | extra hero skins (stretch) |
| `tf_minis/` | 16×16 mini sprites | leaderboard avatars / map icons (stretch) |
| tilesets (`TimeFantasyTiles`, `tf_japan`, `hometown`, `castle`, …) | world tiles | tavern background scene, battle backgrounds |
| `elements_*` packs | FinalBossBlues' **Elements** character generator & packs (deeper, higher-contrast palette) | ⭐ **our palette** (Q13, ADR-14): everything that ships is Elements-coloured; TF-only art is converted, never mixed |
| `custom_exports/` | 4 of your custom generator exports (bunny_servant, forgotten, red_ranger_zero, snow_princess) | fun cameo heroes later |

### 1.2 `C:\Users\xbmcn\Documents\Code\test\forgotten_kanji_app\tool` (3.4 GB) and `tool\derived` (1.3 GB)
| Folder | What it is | Use here |
|---|---|---|
| `derived/Audio/**` (199 files, 1.2 GB WAV) | BGM per region (beach, castle, city, dark, desert, dungeon, farm, forest, lake, ruins, snow, swamp, volcanic), `shared/bgm` (battle_music_one/two, battle_victory, you_lose_game_over, beer_and_ale, strange_goofy_shop, adventurous_music…), `shared/sfx` (footsteps, runes, portals), `Battle/sfx` (46 spell SFX: burn, freeze, hammer, weld, temper, nail, flame_blade, cleansing_light, …) | ⭐ **All game audio.** Candidates: tavern = `beer_and_ale`, battle = `battle_music_one/two`, win = `battle_victory`, loss = `you_lose_game_over`, menu = `adventurous_music`; SFX mapped per event (Step 50) |
| `derived/Battlers`, `derived/Enemies` | the TF monsters **already recoloured to Elements** (your other project), plus Elements-native enemies (dragons, hydra, harpies, gargoyle, beholder, bigskulls, blackknight) | ⭐ **primary monster source**; the SV hero sets get the same treatment in Step 39 and land next to these (`derived/Battlers/sv/`) |
| `derived/MenuUI/` | `cursor_hand(_sheet).png`, `pointers_1.png`, `lobitcardframes1.png`, `lobit_cards_selector.png`, `cardicons_mini.png`, `icons_ui_16.png` | ⭐ UI chrome: cursors, card frames, selection pointer |
| `derived/Battle/` | `magic_circles_1.png`, `meteors_1.png`, `newflames_1.png`, `flare_effect_sheet_1.png`, `etch_impact_1.png`, `lilsparkles`, `liltileflash`, weapon icon sheets (`mswords_icon16`, `mshields_icon16`, …) | FX & relic icons |
| `derived/SpellFX/<name>/` | 20 authored spell FX (Aseprite sources + `exports/*.png` + per-spell WAV) | burn/freeze/explosion/sunbeam etc. for big abilities (stretch) |
| `derived/Faces`, `emote`, `Fx` | TF→Elements conversions | ⭐ faces and FX already in our palette; extend the same batch for anything missing |
| `tool/time_fantasy_assets/` | a large dump of TF freebies by year (2017–2026) | browse for extra tokens/backgrounds |
| `tool/battle/roster.dart`, `simulate.dart` | your previous game's roster viewer & balance simulator (Dart) | **design precedent** for our `sim/` module and the `/dev` gallery |
| `tool/derived/gen_*.py`, `tf_to_elements*.json` | your Python/Pillow asset tooling | precedent: our pipeline is also Python + Pillow (Pillow 12.3 is installed) |

Not installed: Aseprite, ffmpeg (a portable `ffmpeg-win-x86_64-v7.1.exe` exists under
`tool/cache/video-tools/imageio_ffmpeg/binaries/` — we will install ffmpeg properly with winget).

---

## 2. What the game actually ships (target list)

> All sources below are read **after** TF→Elements conversion (Step 39 adds the SV sets, icons and FX to
> `forgotten_kanji_app/tool/derived/`), so the pipeline's raw roots are `tool/derived/**` only.

| Asset group | Count | Source | Output |
|---|---|---|---|
| Human heroes (SV) | 22 heroes × 5 animations (idle 3f, attack 3f, hit 3f, cheer 3f, down 1f) | `tf_svbattle/singleframes` | `atlas/heroes_sv.png` + `.json` |
| Monster heroes & tokens | 14 heroes + 7 tokens, 1 frame each (+ optional 3-frame walk) | `TimeFantasy_Monsters/Battlers` (+ `1x/monster_*.png`) | `atlas/heroes_mon.png` + `.json` |
| Portraits | 36 × 48×48 | `tf-faces` (humans), cropped battler heads (monsters) | `atlas/portraits.png` |
| Relic & perk icons | 16 relics + 9 perks + ~20 UI icons at 32×32 | `icons_12.26.19/fullcolor/individual_32x32` | `atlas/icons.png` |
| FX | ~10 animations (hit, slash, heal, fire, ice, holy, smoke, summon, shield, skull) | `pixel_animations_gfxpack/individual_frames` | `atlas/fx.png` |
| UI chrome | card frames, pointer, cursor, 9-slice panels | `derived/MenuUI`, hand-drawn 9-slices | `atlas/ui.png` |
| Backgrounds | tavern interior, 3 battle backdrops | TF tilesets composed in Tiled or Pillow script | `bg/*.png` (≤ 960×540 at 1×) |
| Audio | 5 BGM loops (OGG + M4A), 1 SFX sprite (~30 sounds) | `derived/Audio` | `audio/bgm_*.ogg/.m4a`, `audio/sfx.ogg/.m4a` + `sfx.json` |
| Studio | corgi frames, logo | **you supply** | `intro/*.png` |

Budget: all atlases ≤ 1.5 MB total; audio ≤ 2.5 MB (OGG q4 + AAC 96k); first paint < 1 MB.

---

## 3. Frame conventions

* **Cell size 48×48** at 1× for all units (SV battlers are already 48×48; monsters are trimmed and
  re-centred into 48×48 or 64×64 cells with the feet on a shared baseline at y = 44).
* **Anchor**: bottom-centre. The UI scales by exactly **3×** (144 px cards) or **2×** on small
  screens — integers only, `image-rendering: pixelated`.
* **Facing**: SV battlers face **left** (RPG Maker convention). Our band is drawn on the left
  facing right → we flip the player's side with `transform: scaleX(-1)` and leave ghosts unflipped.
* **Animation map per hero** (`manifest.json`): `{ "idle": ["set1_1_idle1_1", …], "attack": [...],
  "hit": [...], "fall": ["set1_1_down"], "cheer": [...], "fps": 6 }`. Monsters: `idle` = 1 frame
  (`bob` tween), `attack` = lunge tween, `hit` = white flash + shake, `fall` = fade + drop.
* **Naming**: `sv/<set>/<n>` for humans (e.g. `sv/set1/1`), `mon/<file>` for monsters, `fx/<name>`,
  `icon/<number>`. The GDD's `sprite` field uses these keys.

---

## 4. The pipeline (`tools/assets/`, Python 3.13 + Pillow)

**Step 0 — palette conversion (lives in the other repo).**
`python tool/derived/gen_tf_to_elements.py --batch tool/derived/tf_to_elements_batch_heroes.json`, run from
the `forgotten_kanji_app` root, recolours every TF sheet we use into the Elements palette with the colour
table recovered from FinalBossBlues' own paired packs (`--build-table`; unmapped colours fall back to
`delta`). The jobs file (`{src, out}` pairs) is ours to add; outputs stay in `tool/derived/` so *The
Forgotten Kanji* can reuse them (Q13). Our scripts below read only from there.

```
tools/assets/
├── config/assets.yaml      ← the ONLY hand-edited file: which raw files → which keys, trims, offsets
├── inventory.py            ← scans raw folders, prints counts & sizes (sanity check; Step 39)
├── extract_sv.py           ← picks SV single frames per hero key → normalized 48×48 PNGs
├── extract_mon.py          ← trims monster battlers to their bounding box, re-centres, baseline-aligns
├── extract_icons.py        ← copies/renames the chosen icon numbers; builds perk badges
├── extract_fx.py           ← collects FX frames
├── pack_atlas.py           ← shelf-packs PNGs into power-of-two atlases + JSON (x, y, w, h, anchor)
├── build_audio.py          ← ffmpeg: WAV → OGG/M4A loops; concatenates SFX into one sprite + JSON map
├── build_manifest.py       ← merges everything into frontend/public/assets/manifest.json with content hashes
└── publish.sh         ← `aws s3 sync build/ s3://forgotten-heroes-assets-<env>/v<hash>/`
```
Commands (Step 39–41): `python tools/assets/build.py --all` → writes `tools/assets/build/` and
copies to `frontend/public/assets/` (gitignored). CI pulls the same files from S3 by manifest hash.

Why Python: Pillow is already installed, your previous tooling is Python, and image munging in
Node would mean learning a second image library. Why not Aseprite/TexturePacker: not installed and
not needed for ~400 frames.

---

## 5. Licensing (important because the repo is public)

* **Time Fantasy (FinalBossBlues) licence**: commercial use allowed, no credit required, **"just
  don't redistribute directly."** Shipping sprites inside a playable web game is normal use.
  Putting the raw packs — or neatly packed, game-ready atlases — in a **public GitHub repo** is
  arguably redistribution. **Decision (ADR-13):** raw packs and generated atlases are
  **gitignored**; the pipeline publishes them to a **private S3 bucket**; CI downloads them at build
  time with a least-privilege role; the deployed site serves them like any game. `CREDITS.md`
  thanks FinalBossBlues anyway.
* **Your custom exports & corgi art**: yours; may be committed (small).
* **Audio in `derived/Audio`** (Q12): royalty-free sources — **DRAGON-STUDIO** (ko-fi.com/dragonstudio),
  **Helton Yan**, **Freesound_Community** (pixabay.com/users/freesound_community-46691455/), **OxidVideos**,
  **HorrorSFXFree**. Pixabay's Content Licence allows commercial use without attribution but not
  redistributing the files as-is — same as TF, so same private-S3 treatment; Ko-fi/itch packs carry their
  own terms per download. Step 74 records, per shipped track: source, licence name, download date and a
  screenshot/receipt in `docs/licences/` (kept private); `CREDITS.md` names every creator anyway.
* **Fonts**: Google Fonts (`Pixelify Sans`, `Press Start 2P` or `VT323`) are OFL-licensed — fine to self-host.

---

## 6. Corgi Space Cadet art you will supply (spec)
* Corgi astronaut: 4–6 frame idle bob, **96×96** per frame, transparent PNG, facing right.
* Optional: 2-frame "boop" (paw on glass) and a jetpack puff (3 frames, 32×32).
* Logo: PNG at 3× (≈ 720 px wide) **and** SVG if you have it; monochrome variant for favicon.
* A single-colour star/sparkle sprite (8×8). Everything else is generated.
