# Forgotten Heroes — Game Design Document (GDD)

> This is the **rulebook and content bible**. The engine implements exactly what is written here;
> when the two disagree, fix one of them *and* add a test. Numbers mirror Super Auto Pets' Turtle
> Pack at launch (see `super_auto_pets_knowledge.md`) and get tuned with the balance simulator
> (Phase 6). Theme: a warm, lighthearted fantasy tavern where *forgotten* heroes — including six old
> friends from *The Forgotten Kanji* — sign up for one more run. Tone confirmed by Bryan (Q8).

---

## 1. Vision & pillars

**One-liner:** *Recruit a band of forgotten heroes at the tavern, line them up, and watch them
auto-battle other players' bands. Win 10 battles before you lose 5 lives.*

Pillars (every feature must serve one):
1. **Readable** — you always know why something happened. Battles play back as a clear sequence of
   events with damage numbers and highlighted abilities.
2. **Snappy & tactile** — drag, drop, click, hotkeys; sounds and tiny animations on every action.
3. **Deterministic & fair** — the server computes every battle from a seed; nothing is hidden,
   replays are exact.
4. **Small but deep** — 36 heroes, 16 relics, 5 slots. Depth from positioning, timing, merging.
5. **A portfolio that teaches** — clean architecture over cleverness; every rule has a test.

Audience: recruiters/engineers evaluating the code (primary); friends playing a few runs (secondary).

---

## 2. Glossary (player-facing words)

| Word | Meaning | SAP equivalent |
|---|---|---|
| **Hero** | a unit on your team (humans *and* monsters are all "heroes") | Pet |
| **Band** | your team of up to 5 heroes | Team |
| **Tavern** | the shop | Shop |
| **Relic** | a purchasable item (instant or perk) | Food |
| **Perk** | a status a hero carries (one at a time) | Perk |
| **Fall / Fallen** | reaching 0 health | Faint |
| **Summon** | a hero appearing on the board (bought, or created by an ability) | Summon |
| **Rank** | hero tier I–VI (shown as pips) | Tier |
| **Level** | 1–3, raised by merging copies | Level |
| **Gold** | spent in the tavern, 10 each turn | Gold |
| **Crowns** | wins; 10 crowns wins the run | Trophies |
| **Hearts** | lives; 0 hearts ends the run | Lives |
| **Run** | one Arena attempt from turn 1 to win/loss | Participation |
| **Ghost** | a saved copy of another player's band you fight | Ghost |

---

## 3. Modes

**MVP: Arena** — asynchronous. Each End Turn pairs you with a ghost band saved by another player at
the same turn (bots fill in when no ghost exists). No timers. Pause any time; the run is saved on the server.

**Later (not planned in detail, Q11/Q29):** Daily seed (everyone gets the same shop RNG) and **private
rooms** — first as a **same-seed challenge link**: a friend plays the same seed and you compare results (no
live connection, no new infrastructure — decided Q29); a real-time room would be a later WebSocket upgrade.
No 8-player Versus.

---

## 4. Rules (authoritative)

### 4.1 Run
* Start: **turn 1**, **5 hearts**, **0 crowns**, empty band, 10 gold.
* Win the run at **10 crowns**. Lose the run at **0 hearts**.
* Battle result: **win → +1 crown**; **loss → −1 heart**; **draw → nothing**.
* Heart refund: at the start of turn 3, if you have fewer than 5 hearts, gain 1 (mirrors SAP's
  early-game mercy). (Constant `MERCY_TURN = 3`.)

### 4.2 Turn structure
1. **Start of turn**: gold = 10; tavern tier updated; free roll (frozen slots kept); *Start of turn* abilities.
2. **Tavern phase** (player acts freely): buy, sell, reorder, merge, roll (1 gold), freeze/unfreeze.
3. **End Turn**: *End turn* abilities → band snapshot saved as a ghost → opponent chosen →
   **battle resolved on the server** → result applied → turn + 1.

### 4.3 Tavern
| Rule | Value |
|---|---|
| Gold per turn | 10 (unspent gold is lost) |
| Hero price | 3 |
| Relic price | 3 (exceptions are per-relic; Poison Vial costs 1) |
| Roll | 1 gold (first roll each turn free) |
| Sell | 1 / 2 / 3 gold at level 1 / 2 / 3; selling triggers *Sell*; selling is not a Fall |
| Hero slots in tavern | 3 (turns 1–4) · 4 (turns 5–8) · 5 (turns 9+) |
| Relic slots in tavern | 1 (turns 1–2) · 2 (turns 3+) |
| Rank available | I (t1) · II (t3) · III (t5) · IV (t7) · V (t9) · VI (t11+) |
| Tavern fill | each slot draws uniformly from all heroes of rank ≤ current rank (relics likewise) |
| Freeze | frozen items survive rolls and the next start-of-turn fill; unfreezing frees the slot on next roll |
| Band size | 5 |
| Stat cap | 50/50 |

### 4.4 Leveling
* Merge = drop a copy onto a hero (from tavern = also a purchase for 3 gold; from band = free).
* Result keeps the **higher** attack and **higher** health of the two, then **+1/+1** for the XP.
* XP thresholds: **level 2 at 2 XP, level 3 at 5 XP**. Max level 3.
* On level-up: *Level-up* abilities fire and the tavern gains **one extra hero of rank + 1**
  (capped at VI) in a temporary bonus slot for this turn.
* Ability scaling: `perLevel` values in the DSL (most are ×1/×2/×3).
* Tome of Experience relic: +1 XP.

### 4.5 Battle
Setup: both bands are copied; temporary stats start at 0; a seeded RNG is created from
`hash(runId, turn)`; the "home" side is the player on odd turns and the ghost on even turns (used
only for tie-breaking so battles stay deterministic and symmetric over time).

```
events += BattleStarted
fire START_OF_BATTLE for all units (ordering rule §4.6)
while both sides have units and clashes < 300:
    a = home front, b = away front
    fire BEFORE_ATTACK (a, b)
    dmgToB = damage(a → b), dmgToA = damage(b → a)      # perks modify (§4.8)
    apply both simultaneously → events DamageDealt ×2 (+ Flame Oil splash to 2nd enemy)
    fire HURT for every unit that took > 0 damage (ordering rule)
    fire KNOCKOUT for attackers whose damage reduced a target to ≤ 0
    resolve FALLS: for each unit at ≤ 0 (ordering rule): fire FALL, apply Phoenix Feather,
        then ALLY_FALL / ALLY_AHEAD_FALLS on friends; remove unit; summons appear in its slot
    fire ALLY_AHEAD_ATTACKS, AFTER_ATTACK
    close gaps (shift forward); re-check falls caused by abilities (loop until stable)
    clashes += 1
result = WIN / LOSS / DRAW (both empty or clash cap hit → DRAW)
events += BattleEnded(result)
```

### 4.6 Ability ordering rule (deterministic)
When several abilities are eligible at the same moment, resolve in this order:
1. higher **attack** first; 2. then higher **health**; 3. then **home side** before away;
4. then **front position** before back. Units that fall during resolution still get their *Fall*
abilities (queued, same rule). A summon that cannot fit (band full) produces `SummonFailed`.

### 4.7 Temporary vs permanent
* Battle effects are **temporary** (`untilEndOfBattle: true`) unless the ability says otherwise.
* Tavern effects are **permanent**, except *Battle Brew* (temporary) which is flagged per relic.
* Temporary stats are shown with a distinct colour in the UI and discarded after the battle.

### 4.8 Damage & perks
`damage = max(0, attack + bonusDamage − damageReduction)` with exceptions below; a unit falls at
health ≤ 0. A hero holds **one** perk; gaining another replaces it.

| Perk | Granted by | Effect |
|---|---|---|
| **Fairy Charm** | relic (rank I) | Fall → summon a 1/1 Sprite in this slot |
| **Iron Blade** | relic (rank II) | attacks deal +3 damage |
| **Chainmail** | relic (rank III) | take 2 less damage (minimum 1 if damage > 0) |
| **Flame Oil** | relic (rank V), War Horse token | also hit the enemy *behind the target* for 5 |
| **Ward** | relic (rank VI), Templar, Guardian | take 20 less damage, **once** (perk consumed) |
| **Phoenix Feather** | relic (rank VI) | Fall → return as a 1/1 in the same slot (perk consumed) |
| **Enchanted Blade** | relic (rank VI) | +20 damage on the next attack, **once** |
| **Aegis** | Demon ability | ignore the next damage entirely, **once** |
| **Venom** | Venomancer ability | any unit this hero damages falls immediately |

---

## 5. Hero roster

> **Names (Q15):** six heroes are cameos from *The Forgotten Kanji* — **Aoi** 葵, **Hotaru** 蛍, **Iwao** 岩男,
> **Kashi** 樫, **Kaede** 楓, **Nagi** 凪 — placed by element and ability (placement confirmed Q28; rows marked ✦; the old archetype
> stays as the hero's *title*, e.g. "Aoi, the Novice Scribe"). The other 30 keep archetype names.
> **Art (Q13, ADR-14):** every sprite ships in the **Elements** palette; the SV sets named below are
> converted first (Step 39). Face-sheet hints for the cameos come from `characters.json` in your other
> game (Aoi `tf_char1`#0, Nagi `tf_char2`#7, Kashi `tf_char2`#3, Hotaru `tf_char3`#2, Kaede `tf_char7`#4,
> Iwao `tf_char8`#2) — the matching SV set/char is the first thing to try in the Step 40 gallery.

Stats are **attack/health** at level 1. `×L` = scales with level (1/2/3). Art column names the
Time Fantasy source (see `asset_inventory_and_pipeline.md`); "SV" = side-view battler sets,
"MON" = monster battlers. Picks marked *(verify)* get confirmed in the sprite gallery (Step 40).

### Rank I (available turn 1) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 1 | ✦ **Aoi**, the Novice Scribe *(Squire)* | 2/2 | Fall → one random ally +1/+1 ×L | death→value | SV set1 char1 (blue tunic swordsman; face `tf_char1`#0) |
| 2 | **Farmhand** | 1/3 | Fall → summon a 1/1 **Scarecrow** ×L (L2: 2/2, L3: 3/3) | summon | SV set2 (villager) *(verify)* |
| 3 | **Merchant** | 4/1 | Sell → +1 gold ×L | economy | SV set3 (robed man) *(verify)* |
| 4 | **Bard** | 2/1 | Ally summoned → it gains +1 attack ×L until end of battle | summon synergy | SV set2 (lute-ready pose: use *item* frames) *(verify)* |
| 5 | **Archer** | 2/2 | Start of battle → 1 damage ×L to one random enemy | burst opener | SV set1 char3 (bow frames exist) |
| 6 | ✦ **Hotaru**, the Calligrapher *(Acolyte)* | 1/3 | Buy → one random ally +1 health ×L | shop buff | SV set3 char3 (face `tf_char3`#2) *(verify)* |

### Rank II (turn 3) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 7 | **Shield Maiden** | 2/5 | Hurt → +4 attack ×L | scaling tank | SV military1 (shield soldier) |
| 8 | **Alchemist** | 4/2 | Fall → 2 damage ×L to **all** units | AoE | SV set4 (goggles) *(verify)* |
| 9 | **Plague Rat** | 3/6 | Fall → summon a 1/1 **Dirty Rat** ×L up front **for the enemy** | drawback | MON rat |
| 10 | **Treasurer** | 1/2 | Start of turn → +1 gold ×L | economy | SV set3 (noble) *(verify)* |
| 11 | **Herbalist** | 1/3 | Start of turn → stock a 2-gold **Ration** in the tavern (L2: 1 gold, L3: free) | economy | SV set5 (green robe) *(verify)* |
| 12 | **Standard Bearer** | 3/2 | Fall → the two nearest allies behind +1/+1 ×L | position | SV military2 (banner) |

### Rank III (turn 5) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 13 | **Berserker** | 6/3 | Fall → deal 50 % ×L of attack to adjacent units (both sides) | AoE | MON orc1 |
| 14 | ✦ **Iwao**, the Stone Scribe *(Paladin)* | 3/4 | Hurt → nearest ally behind +1/+2 ×L | position | SV set8 char3 (face `tf_char8`#2) *(verify)* |
| 15 | **Beastmaster** | 3/2 | Ally summoned → +2/+1 ×L until end of battle | summon synergy | SV set6 *(verify)* |
| 16 | **Assassin** | 4/3 | Start of battle → 4 damage ×L to the lowest-health enemy | burst | SV set4 (hooded) *(verify)* |
| 17 | **Clay Golem** | 3/7 | After attack → 1 damage ×L to nearest ally behind | drawback tank | MON golem1 |
| 18 | **Druid** | 2/2 | Fall → summon two 2/2 **Wolves** ×L (L2: 4/4, L3: 6/6) | summon | SV set5 (staff) + MON wolf1 token |

### Rank IV (turn 7) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 19 | ✦ **Kaede**, the Ink Warden *(Pyromancer)* | 3/6 | Hurt → 3 damage ×L to one random enemy | punish | SV set7 char5 (face `tf_char7`#4) *(verify)* |
| 20 | **Stable Master** | 2/2 | Fall → summon a 5/3 **War Horse** with Flame Oil ×L (L2: 10/6, L3: 15/9) | summon | SV set7 + horse token (tf_animals horse) |
| 21 | **Ogre** | 4/5 | Knockout → +3/+3 ×L | snowball | MON orc8 (large) *(verify)* |
| 22 | **Witch** | 3/5 | Start of battle → highest-health enemy loses 33 % ×L of health | burst | SV set6 (hat) *(verify)* |
| 23 | ✦ **Kashi**, the Warden Scribe *(Templar)* | 2/5 | Fall → nearest ally behind gains **Ward** (L2: two allies, L3: three) | protect | SV set2 char4 (face `tf_char2`#3) *(verify)* |
| 24 | **Drill Sergeant** | 1/3 | End turn → two level-2+ allies +1/+1 ×L | shop buff | SV military2 |

### Rank V (turn 9) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 25 | **Stone Golem** | 2/6 | Start of battle → **all** units +8 health ×L | symmetric | MON golem2 |
| 26 | **Dragon Knight** | 8/4 | Start of battle → 8 damage ×L to the last enemy | burst | MON dknight1 |
| 27 | **Minotaur** | 6/9 | Knockout → 4 damage ×L to the first enemy (double vs rank I) | snowball | MON minotaur |
| 28 | **Phoenix** | 6/4 | Fall → summon a **Hatchling** with 1 health and 50 % ×L of this attack | rebirth | MON phoenix |
| 29 | **Lich** | 2/2 | Ally falls → +2/+2 ×L | scaling | MON lick (lich) |
| 30 | **Warlord** | 3/4 | Ally summoned → it gains +3/+1 ×L | summon synergy | MON orc3 (chief) *(verify)* |

### Rank VI (turn 11) — 6 heroes
| # | Hero | Stats | Trigger → effect | Pattern | Art |
|---|---|---|---|---|---|
| 31 | **Juggernaut** | 10/6 | Before attack → +4/+2 ×L | scaling | MON dknight2 |
| 32 | **Elder Treant** | 4/12 | Fall → all allies +2/+2 ×L | death→value | MON treant |
| 33 | ✦ **Nagi**, the Blade Scribe *(Shadow Blade)* | 10/4 | Start of battle → 50 % ×L of attack as damage to one random enemy | burst | SV set2 char8 (face `tf_char2`#7) *(verify)* |
| 34 | **Hydra** | 6/6 | Ally ahead attacks → 5 damage ×L to one random enemy | position | derived/Enemies hydra_5_1 |
| 35 | **Demon** | 7/10 | Hurt → gain **Aegis** (1× per battle; L2: 2×, L3: 3×) | tank | MON demon |
| 36 | **Necromancer** | 5/5 | Ally falls → summon a 4/4 **Skeleton** ×L in its slot (3× per battle) | summon | MON wizard4 + MON skeleton token *(verify)* |

### Tokens (never in the tavern)
| Token | Stats | Art |
|---|---|---|
| Scarecrow | 1/1 ×L | derived/Enemies plant or MON scrub *(verify)* |
| Dirty Rat | 1/1 ×L | MON rat |
| Wolf | 2/2 ×L | MON wolf1 |
| Sprite | 1/1 | MON elemental3 (light) *(verify)* |
| War Horse | 5/3 ×L, Flame Oil | tf_animals horse1 |
| Hatchling | X/1 | MON bird1 |
| Skeleton | 4/4 ×L | MON skeleton |

### Stretch roster (Phase 6, +18) — mapped from SAP for later
Trader (Beaver), Innkeeper (Duck), Apprentice (Fish), Page (Kangaroo), Mimic (Crab), Summoner
(Spider), Veteran (Snail), Mentor (Giraffe), Guardian (Ox), Herald (Dodo), Doppelganger (Parrot),
Quartermaster (Squirrel), Champion (Bison), Royal Advisor (Monkey), Venomancer (Scorpion),
Glutton (Seal), Oracle (Tiger), Archmage (Dragon). The DSL already covers all of their effects
except Oracle (repeat ability) and Mimic Chest/Whale (swallow), which need two new effect types.

---

## 6. Relics

| Rank | Relic | Price | Effect | Kind |
|---|---|---|---|---|
| I | **Ration** | 3 | one hero +1/+1 | instant |
| I | **Fairy Charm** | 3 | perk: Fall → summon a 1/1 Sprite | perk |
| II | **Battle Brew** | 3 | one hero +3/+3 **until end of battle** | instant, temporary |
| II | **Iron Blade** | 3 | perk: +3 damage | perk |
| II | **Poison Vial** | **1** | a hero falls (its Fall ability triggers in the tavern) | instant |
| III | **Chainmail** | 3 | perk: take 2 less damage | perk |
| III | **Feast** | 3 | two random heroes +1/+1 | instant |
| IV | **Royal Decree** | 3 | all current **and future** tavern heroes +1/+1 | instant, tavern-wide |
| IV | **Elixir** | 3 | one hero +2/+2 | instant |
| V | **Flame Oil** | 3 | perk: also hit the second enemy for 5 | perk |
| V | **Tome of Experience** | 3 | one hero +1 XP | instant |
| V | **Banquet** | 3 | three random heroes +1/+1 | instant |
| VI | **Ward** | 3 | perk: take 20 less damage, once | perk |
| VI | **Phoenix Feather** | 3 | perk: Fall → return as 1/1 | perk |
| VI | **Enchanted Blade** | 3 | perk: +20 damage, once | perk |
| VI | **Grand Feast** | 3 | two random heroes +2/+2 | instant |

Relic art: 32×32 icons from `icons_12.26.19` (potions, food, weapons, armor sections).

---

## 7. Ability DSL (what the JSON looks like)

Every hero/relic is data. One interpreter in the engine. Example (`content/heroes.json`):

```json
{
  "id": "aoi",
  "name": "Aoi",
  "title": "the Novice Scribe",
  "kanji": "葵",
  "rank": 1,
  "attack": 2,
  "health": 2,
  "sprite": "sv/set1/1",
  "ability": {
    "trigger": "FALL",
    "text": "Fall → Give one random ally +{atk}/+{hp}.",
    "effects": [
      { "type": "MODIFY_STATS",
        "target": { "select": "RANDOM_ALLY", "count": 1 },
        "attack": { "perLevel": 1 }, "health": { "perLevel": 1 },
        "untilEndOfBattle": false }
    ]
  }
}
```

**Triggers:** `START_OF_TURN, END_OF_TURN, BUY, SELL, LEVEL_UP, ALLY_SUMMONED, EAT_RELIC,
START_OF_BATTLE, BEFORE_ATTACK, AFTER_ATTACK, HURT, FALL, ALLY_FALL, ALLY_AHEAD_FALLS,
ALLY_AHEAD_ATTACKS, KNOCKOUT, SUMMONED`.

**Targets:** `SELF, RANDOM_ALLY(count), RANDOM_ENEMY(count), ALL_ALLIES, ALL_ENEMIES, ALL_UNITS,
ADJACENT, NEAREST_ALLIES_BEHIND(count), NEAREST_ALLIES_AHEAD(count), FIRST_ENEMY, LAST_ENEMY,
LOWEST_HEALTH_ENEMY, HIGHEST_HEALTH_ENEMY, TRIGGERING_UNIT, FRONT_ALLY, LEVEL_2_PLUS_ALLIES(count),
TAVERN_HEROES, ALL_FUTURE_TAVERN_HEROES`.

**Effects:** `MODIFY_STATS(attack, health, untilEndOfBattle)`, `DEAL_DAMAGE(amount | percentOfAttack)`,
`SET_HEALTH_PERCENT(percent)`, `SUMMON(tokenId, count, stats, perk, side=ALLY|ENEMY, position=HERE|FRONT)`,
`GAIN_GOLD(amount)`, `GIVE_PERK(perk)`, `STOCK_RELIC(relicId, price)`, `GIVE_XP(amount)`,
`BUFF_TAVERN(attack, health, permanent)`.

**Amounts:** `{ "flat": 4 }`, `{ "perLevel": 1 }`, `{ "byLevel": [1, 2, 3] }`, `{ "percentOfAttack": 50 }`.

**Limits:** `"limit": { "perTurn": 1 }` or `{ "perBattle": 3 }`.

**Conditions:** `{ "ifTargetRank": 1, "multiplier": 2 }` (Minotaur), `{ "requiresAllyLevel": 3 }`.

The schema is validated at load time (JSON Schema) and every hero has at least one scenario test.

---

## 8. Opponents when nobody else is playing (cold-start)

The balance simulator (Step 22) runs thousands of bot runs and stores their bands as **ghosts**
tagged `bot=true` for every turn. Matchmaking prefers real ghosts at the same turn with a similar
crown count, then bot ghosts, then a mirror of your own band (SAP's `ArenaMirror`). Players never
see "no opponent found".

---

## 9. Balance targets (checked by the simulator)

* Average run length for a greedy bot: 8–12 turns. Win rate of greedy bot vs random bot: 75–90 %.
* No hero with > 60 % "win rate when on band" at its rank, none < 40 %, after 10,000 runs.
* Pick rate of every hero between 5 % and 25 % for the greedy bot.
* Median battle length: 6–14 clashes; 99.9th percentile < 60; **zero** clash-cap draws.
* Economy heroes should be net-positive over a run (Merchant ≥ +4 gold, Treasurer ≥ +6).

---

## 10. Out of scope for v1.0 (write it down so it stops nagging)
8-player Versus · live rooms · daily seeds · cosmetics · achievements · in-app purchases · mobile apps ·
localization beyond English · chat/social · spectating live · more than 36 heroes (Q7: keep it simple,
ship fast).

---

## 11. Studio: Corgi Space Cadet (intro + landing page)

Beat sheet for the ~5-second skippable intro (Phase 4):
1. Black. Three stars twinkle in (0.5 s). Soft space hum.
2. A corgi in a bubble helmet floats in from the left, slight bob (1.5 s). Jetpack puff SFX.
3. The corgi taps the glass: "boop" chime; a pixel "WOOF" speech bubble (0.5 s).
4. Logo text **CORGI SPACE CADET** types in pixel font, underline sweeps (1 s).
5. "presents" fades (0.5 s) → cut to the Forgotten Heroes title screen. Any click/key skips.

You will supply the corgi art later (Q14) — Phase 4 is deferred until it arrives and may follow the
launch; a placeholder sprite lets the code be built and tested whenever you get to it. The studio also
gets a one-page site on Hostinger at `corgi.bryanmcneil.pro` (logo, game link, GitHub link).
