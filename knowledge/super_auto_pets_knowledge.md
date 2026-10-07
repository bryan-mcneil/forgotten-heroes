# Super Auto Pets — Knowledge Base

> Purpose: understand *how Super Auto Pets (SAP) works* — rules, economy, battle resolution,
> and (uniquely) how the real game is **structured internally** — so Forgotten Heroes can borrow
> the proven design and architecture while using our own fantasy theme and art.
>
> Sources, in order of trust:
> 1. **The game install you gave me** (`Super Auto Pets/`). It is a Unity IL2CPP build. I extracted
>    the symbol table from `global-metadata.dat` (168,880 strings) and the English text bundle.
>    Class/namespace names tell us the *architecture*. Anything marked **[inferred]** is my
>    reading of those names, not documentation.
> 2. The community wiki (superautopets.wiki.gg) for exact rules and the Turtle Pack roster.
> 3. Guides/forums for the fuzzy bits (tie-breaking), marked **[community]**.

---

## 1. The game in one paragraph

SAP is an **asynchronous auto-battler**. Each turn has two phases. In the **shop phase** you
spend 10 gold on pets (3 gold) and food (3 gold), arrange up to 5 pets in a line, and press *End
Turn*. In the **battle phase** your team fights an opponent's team automatically — the front pet
of each side hits the other simultaneously until one side has nothing left. Win 10 battles before
losing all your lives. The depth comes from **abilities that trigger on events** (Faint, Hurt,
Start of battle, Sell, …) and from **leveling** pets by merging copies. Nothing requires reflexes;
it is a pure decision game, which is exactly why it ports perfectly to a web app with a Java
engine.

---

## 2. Exact rules (verified on the wiki unless marked)

### 2.1 Turn & economy

| Rule | Value |
|---|---|
| Gold per turn | **10** (resets every turn; unspent gold is lost unless an ability says otherwise) |
| Pet cost | **3** |
| Food cost | **3** (exceptions: Sleeping Pill 1, Strawberry Jam 1) |
| Reroll shop | **1** gold; the first roll each turn is free |
| Sell value | **1 / 2 / 3** gold for a level 1 / 2 / 3 pet |
| Team size | **5** slots |
| Freeze | Any shop item can be frozen; it stays through rerolls and into the next turn and occupies its slot |
| Shop tier schedule | Tier 1 at turn 1, **tier 2 at turn 3, tier 3 at turn 5, tier 4 at turn 7, tier 5 at turn 9, tier 6 at turn 11+** (a new tier every odd turn) |
| Shop pet slots | 3 pets on turns 1–4, 4 pets on turns 5–8, 5 pets on turns 9+ **[community]** |
| Shop food slots | 1 food on turns 1–2, 2 foods from turn 3 **[community]** |
| Stat cap | Attack and health cap at **50** (one special pet caps at 100) |

### 2.2 Leveling (the "merge" mechanic)

* Every pet starts at **level 1 with 0 XP**.
* Dropping a copy of the same pet onto it (from shop or team) merges them: **+1 XP** and the merged
  pet keeps the **higher** of each stat, then gains **+1/+1** from the XP.
* **Level 2 at 2 XP** (3 copies total). **Level 3 at 5 XP** (6 copies total). Level 3 is max.
* Every XP point gives **+1 attack, +1 health** permanently.
* **Level-up bonus:** when a pet levels up, the shop spawns a pet from **one tier higher** than
  currently available (the symbol names `LevelUpBonusBeforeTierUp` / `LevelUpBonusCurrentTier` /
  `LevelUpBonusFinalLevel` confirm the server computes which tier that bonus comes from **[inferred]**).
* Abilities scale with level (usually ×1, ×2, ×3 — e.g. Ant gives +1/+1, +2/+2, +3/+3).
* Chocolate (food) gives +1 XP directly.

### 2.3 Lives, trophies, modes

| Mode | Lives | Win condition | Notes |
|---|---|---|---|
| Arena (normal) | **5** | **10 trophies (wins)** | Asynchronous, untimed. Opponent = another player's team saved at the same turn |
| Arena (easy) | 7 | 10 | No achievements |
| Arena (hard) | 5 | 10 | Forced handicap toys |
| Versus / With friends | 6 | Last player standing | **8 players**, synchronous, **turn timer** |
| Weekly / Custom packs | — | — | Different pet pools; weekly pack regenerates every Monday |

* A **draw** (both teams wiped) costs nothing and awards nothing.
* On turn 3 you regain one life if you lost one on turns 1–2 (softens early randomness) **[community]**.
* Battles 1–2 are low-stakes by design: tiny rosters, lots of RNG.

### 2.4 Battle resolution (the algorithm)

1. **Start of battle** — every pet with a *Start of battle* ability fires, ordered by **attack,
   highest first**; ties go to **higher health** **[community]**, and after that the game's own
   internal ordering (which players describe as "wonky"). Effects can kill pets before any attack.
2. **Loop while both teams have pets:**
   1. **Before attack** abilities of the two front pets fire.
   2. The two front pets **hit each other simultaneously**; damage = attacker's attack, modified
      by perks (Meat Bone +3, Garlic −2 taken, Melon −20 once, Coconut ignore once, Steak +20 once,
      Chili also hits the second enemy for 5, Peanut = any damage is a knockout).
   3. **Hurt** abilities fire for anyone damaged (including splash victims).
   4. **Knockout** abilities fire for the pet whose damage reduced another to 0.
   5. **Faint** abilities fire for every pet at 0 health, again highest attack first. Faint summons
      appear **in the fainted pet's position** if the team has fewer than 5 pets; otherwise the
      summon fails (a `TriggerSummonFailSystem` exists — a *failed summon is itself an event* **[inferred]**).
   6. **Friend summoned / Friend faints / Friend ahead attacks / After attack** abilities fire.
   7. Dead pets are removed and the line **shifts forward** (nobody "jumps" — gaps close).
3. **End:** one side empty → the other wins. Both empty → draw. SAP has no visible round cap; we
   will add one (a safety valve against infinite summon loops).

Important nuances the engine must respect:

* **Temporary vs permanent stats.** Battle buffs ("until end of battle", e.g. Horse) vanish after
  the fight; shop buffs persist. Cupcake is a shop food that is nevertheless temporary. The symbol
  `PermarizeMinionMutator` shows SAP has an explicit "make temporary stats permanent" operation **[inferred]**.
* **Per-turn / per-battle ability limits** ("Works 1 time per turn"). Symbols `RefreshLimitMutator`,
  `HurtCountsThisBattle`, `AttackCountsThisTurn` show the engine tracks counters per unit **[inferred]**.
* **Perks**: a pet carries **one** perk at a time; a new one replaces it.
* **Summoning counts for shop too**: buying a pet from the shop is a "summon" (Horse buffs it).
  Merging is **not** a summon.
* **Opponent summons** (Rat puts a Dirty Rat on *their* side) mean effects can target either board.

### 2.5 Ability trigger vocabulary (complete list seen in SAP)

Shop-phase triggers: **Buy, Buy food, Buy tier-1 pet, Eat shop food, Sell, Level-up, Roll,
Start of turn, End turn, Friend bought, Friend sold, Shop tier upgraded, Friend gained perk**.
Battle triggers: **Start of battle, Before attack, After attack, Hurt, Faint, Knock out, Summoned,
Friend summoned, Friend faints, Friend ahead attacks, Friend ahead faints, Friend ahead hurt,
Enemy summoned, Enemy hurt/pushed, Empty front space, Friendly toy broke, Four friends hurt**.
The game's trigger systems (from symbols): `TriggerStartBattleSystem, TriggerBeforeAttackSystem,
TriggerAttackSystem, TriggerHurtSystem, TriggerKillSystem, TriggerAllFaintSystem,
TriggerBeforeDeathSystem, TriggerDestroySystem, TriggerSummonSystem, TriggerSummonFailSystem,
TriggerInfrontSystem, TriggerEmptyFrontSystem, TriggerPlaySystem (buy), TriggerSellSystem,
TriggerRollSystem, TriggerBeforeRollSystem, TriggerStartTurnSystem, TriggerEndTurnSystem,
TriggerGiveExpSystem, TriggerGainedBuffSystem, TriggerGainedHealthSystem, TriggerPerkGainedSystem,
TriggerPerkLostSystem, TriggerStealPerkSystem, TriggerUpgradeTierSystem, TriggerSpendGoldSystem,
TriggerTransformSystem, TriggerPushMinionSystem, TriggerJumpAttackSystem, TriggerChargedMinionSystem,
TriggerPlaySpellSystem, TriggerPlaySpellOnSystem, TriggerStockMinionSystem`.
**Lesson:** one small "system" per trigger type is how they keep 300+ pets manageable.

---

## 3. The Turtle Pack roster (our balance reference)

Why this matters: the Turtle Pack is the beginner pack and has been tuned for years. Fantasy
Heroes' first roster mirrors its *stat curve and ability patterns* (not its names). Stats are
attack/health at level 1.

### Tier 1
| Pet | Stats | Trigger → effect |
|---|---|---|
| Ant | 2/2 | Faint → one random friend +1/+1 |
| Beaver | 3/2 | Sell → two random friends +1 attack |
| Cricket | 1/3 | Faint → summon a 1/1 Zombie Cricket |
| Duck | 2/3 | Sell → shop pets +1 health |
| Fish | 2/3 | Level-up → two friends +1/+1 |
| Horse | 2/1 | Friend summoned → it gets +1 attack until end of battle |
| Mosquito | 2/2 | Start of battle → 1 damage to one random enemy |
| Otter | 1/3 | Buy → one random friend +1 health |
| Pig | 4/1 | Sell → +1 gold |
| Pigeon | 3/1 | Sell → stock a free Bread Crumbs |
| Sloth | 1/1 | No ability (joke pet) |

### Tier 2
| Pet | Stats | Trigger → effect |
|---|---|---|
| Crab | 4/1 | Start of battle → copy 50% health from healthiest friend |
| Flamingo | 3/2 | Faint → two nearest friends behind +1/+1 |
| Hedgehog | 4/2 | Faint → 2 damage to **all** |
| Kangaroo | 2/3 | Friend ahead attacks → +1/+1 |
| Peacock | 2/5 | Hurt → +4 attack |
| Rat | 3/6 | Faint → summon a 1/1 Dirty Rat **for the opponent**, up front |
| Snail | 2/2 | End turn → if you lost last battle, three nearest friends ahead +1 attack |
| Spider | 2/2 | Faint → summon a random tier-3 pet as a 2/2 |
| Swan | 1/2 | Start of turn → +1 gold |
| Worm | 1/3 | Start of turn → stock a 2-gold Apple |

### Tier 3
| Pet | Stats | Trigger → effect |
|---|---|---|
| Badger | 6/3 | Faint → 50% attack damage to adjacent pets (both sides) |
| Camel | 3/4 | Hurt → nearest friend behind +1/+2 |
| Dodo | 4/2 | Start of battle → give 50% attack to nearest friend ahead |
| Dog | 3/2 | Friend summoned → +2/+1 until end of battle |
| Dolphin | 4/3 | Start of battle → 4 damage to lowest-health enemy |
| Elephant | 3/7 | After attack → 1 damage to nearest friend behind |
| Giraffe | 1/2 | End turn → nearest friend ahead +1/+1 |
| Ox | 1/3 | Friend ahead faints → gain Melon + 1 attack (1×/turn) |
| Rabbit | 1/2 | Friend eats food → +1 health (4×/turn) |
| Sheep | 2/2 | Faint → summon two 2/2 Rams |

### Tier 4
| Pet | Stats | Trigger → effect |
|---|---|---|
| Bison | 4/4 | End turn → if a level-3 friend exists, +1/+2 |
| Blowfish | 3/6 | Hurt → 3 damage to one random enemy |
| Deer | 2/2 | Faint → summon a 5/3 Bus with Chili |
| Hippo | 4/5 | Knock out → +3/+3 |
| Parrot | 4/2 | End turn → copy ability of nearest friend ahead (level 1) |
| Penguin | 1/3 | End turn → two level 2+ friends +1/+1 |
| Skunk | 3/5 | Start of battle → highest-health enemy loses 33% health |
| Squirrel | 2/5 | Start of turn → shop food 1 gold cheaper |
| Turtle | 2/5 | Faint → nearest friend behind gets Melon |
| Whale | 3/7 | Start of battle → swallow friend ahead, release as level 1 on faint |

### Tier 5
| Pet | Stats | Trigger → effect |
|---|---|---|
| Armadillo | 2/6 | Start of battle → **all** pets +8 health |
| Cow | 4/6 | Buy → replace food shop with two free Milk |
| Crocodile | 8/4 | Start of battle → 8 damage to last enemy |
| Monkey | 1/2 | End turn → front friend +2/+2 |
| Rhino | 6/9 | Knock out → 4 damage to first enemy (double vs tier 1) |
| Rooster | 6/4 | Faint → summon a Chick with 1 health and 50% of this attack |
| Scorpion | 1/1 | Summoned → gain Peanut |
| Seal | 3/8 | Eats food → three random friends +1 attack |
| Shark | 2/2 | Friend faints → +2/+2 |
| Turkey | 3/4 | Friend summoned → it gets +3/+1 |

### Tier 6
| Pet | Stats | Trigger → effect |
|---|---|---|
| Boar | 10/6 | Before attack → +4/+2 |
| Cat | 4/5 | Food gives double stats (2×/turn) |
| Dragon | 6/8 | Tier-1 friend bought → friends +1/+1 |
| Fly | 5/5 | Friend faints → summon a 4/4 Zombie Fly in its place (3×/battle) |
| Gorilla | 7/10 | Hurt → gain Coconut (1×/battle) |
| Leopard | 10/4 | Start of battle → 50% attack damage to one random enemy |
| Mammoth | 4/12 | Faint → all friends +2/+2 |
| Snake | 6/6 | Friend ahead attacks → 5 damage to one random enemy |
| Tiger | 6/4 | Friend ahead repeats its ability (as level 1) |
| Wolverine | 5/4 | Four friends hurt → all enemies lose 2 health |

### Tokens (summoned units)
Zombie Cricket 1/1 · Dirty Rat 1/1 · Ram 2/2 · Bee 1/1 (from Honey) · Bus 5/3 (with Chili) · Chick X/1 · Zombie Fly 4/4.

### Foods (Turtle Pack)
| Tier | Food | Effect | Kind |
|---|---|---|---|
| 1 | Apple | one pet +1/+1 | instant |
| 1 | Honey | **perk**: Faint → summon a 1/1 Bee | perk |
| 2 | Cupcake | one pet +3/+3 **until end of battle** | instant (temporary) |
| 2 | Meat Bone | **perk**: attacks deal +3 | perk |
| 2 | Sleeping Pill (1 gold) | make one pet faint (triggers Faint abilities in the shop!) | instant |
| 3 | Garlic | **perk**: take 2 less damage (min 1) | perk |
| 3 | Salad Bowl | two random pets +1/+1 | instant |
| 3 | Cake | perk: End turn → sell value +1 | perk |
| 4 | Canned Food | all current **and future** shop pets +1/+1 | instant (shop-wide) |
| 4 | Pear | one pet +2/+2 | instant |
| 5 | Chili | **perk**: also hit the second enemy for 5 | perk |
| 5 | Chocolate | +1 XP | instant |
| 5 | Sushi | three random pets +1/+1 | instant |
| 6 | Melon | **perk**: take 20 less damage, once | perk |
| 6 | Mushroom | **perk**: Faint → come back as 1/1 | perk |
| 6 | Pizza | two random pets +2/+2 | instant |
| 6 | Steak | **perk**: +20 damage, once | perk |
| — | Coconut | perk: ignore damage once (granted by abilities) | perk |
| — | Peanut | perk: anything hurt by this pet is knocked out | perk |

**Design patterns to copy** (these recur across every tier):
1. *Death → value* (Ant, Cricket, Sheep, Mammoth): makes losing a unit feel good.
2. *Economy pets* (Pig, Swan, Worm, Squirrel): give a second axis to optimize besides stats.
3. *Position matters* (Flamingo, Camel, Kangaroo, Elephant): rewards arranging the line.
4. *Summon synergy* (Horse, Dog, Turkey + Cricket/Sheep/Deer): a buildable archetype.
5. *Burst openers* (Mosquito, Dolphin, Crocodile, Leopard): counter big single targets.
6. *Scaling tanks* (Peacock, Hippo, Shark, Boar): reward keeping a carry alive.
7. *Perks as "equipment"*: a cheap way to add depth without new units.

---

## 4. How SAP is built internally **[inferred from symbols]**

This is the part you cannot get from any wiki, and it is the most valuable for us.

### 4.1 Tech stack of the real game
* **Unity** (IL2CPP, DirectX 12), C#. Shipped libraries: **UniTask** (async), **DOTween** (tweens),
  **BestHTTP** (HTTP/REST client), **Steamworks.NET**, Unity **Addressables**, Unity
  **Localization** (14 languages), **Easy Save 3**, Unity **IAP**, **Quantum Console** (in-game
  debug console), **ProCamera2D**, **All In 1 Sprite Shader**, Unity Analytics.
* The client talks to a **REST backend over HTTP** (BestHTTP + `BaseRequest`/`BaseResponse`
  DTOs). There is no realtime socket for Arena. Versus has `PokeVersusRequest` and a
  `FetchVersusDateTimeRequest` (server time for the turn timer) — polling, not websockets.
* Game-wide code sits under the namespace **`Spacewood.Core`** (their shared engine; the Unity
  side is `Spacewood.Unity`). Separating *core rules* from *Unity rendering* is precisely what we
  will do with `engine` (Java) vs `frontend` (React).

### 4.2 Vocabulary (their names → ours)
| SAP internal | Meaning | Forgotten Heroes name |
|---|---|---|
| Minion | a pet | **Hero** |
| Spell | a food item | **Relic** |
| Perk | a held status (Honey, Garlic…) | **Perk** |
| Relic / Toy | hard-mode toys | (not in MVP) |
| Board | the whole player state (team + shop + gold) | **Board** |
| Build | the shop phase (the player "builds" a board) | **Tavern phase** |
| Battle | the fight | **Battle** |
| Participation | one run of Arena | **Run** |
| Ghost | the saved opponent team | **Ghost** |
| Playback | battle replay data | **Battle log** |
| Trumpets | Versus-only currency | — |

### 4.3 The core pattern: Actions → Resolver → Mutators → Board Events
Namespaces: `Spacewood.Core.Actions.{Build, Battle, Board, Deck, Minions, Spells, User, Resolver}`,
`Spacewood.Core.Models.{Board, BoardResolver, Abilities}`, `Spacewood.Core.Models.BoardResolver.BoardEvents`.

```
Player intent            Rules engine                     Atomic state change            Fact that happened
(Action)          →      (BoardResolver + Systems)   →    (Mutator)                 →    (BoardEvent)
"RollShop"               checks gold, tier, freezes       RollShopMutator, SpendGold      ShopRolled, GoldChanged
"EndTurn"                runs battle phases               DamageAttempt, MarkMinionDead   Hit, Hurt, Faint, Summon
```

* **Mutators** are tiny, single-purpose state changes. The complete list found: `AddMinionToShop,
  AddSpellToShop, BalanceStats, ChangeVictory, CopyMinionAbility, CopyMinionStats, DamageAttempt,
  DestroyMinion, Discount, EndTurn, ExchangeStats, FreezeShop, GainGold, GainLives, GainRolls,
  GiveMinionAbility, GiveMinionBuff, GiveMinionDebuff, GiveMinionExp, GiveMinionSellValue,
  GiveShopMinionBuff, GiveShopMinionPermanentBuff, LoseMinionPerk, MarkDamageTaken, MarkMinionDead,
  Move, OrderMinion, PerkMutator, PermarizeMinion, PlayMinion, PlaySpell, Push, RefreshLimit,
  RemoveMinion, ReorderMutator, ReplaceMinionShop, ReplaceSpellShop, RollPriority, RollShop,
  SellMinion, SetBattleState, SetGold, SetMinionPrice, SetMinionStats, SetSpellPrice,
  ShuffleMinions, SpendAttack, SpendGold, SpendHealth, StackMinion (merge), StartTurn, StealMinion,
  StealMinionPerk, SummonMinion, SwallowShop, SwapStats, ThrowMinion, TransformMinion, UpgradeTier`
  … about 80 in total. **Every ability in the game is a combination of these.**
* **Systems** scan the board and decide *when* mutators run: one `Trigger…System` per trigger type
  (listed in §2.5), plus `ActivateAbilityTrySystem` / `ActivateAbilityDoSystem` (try = check
  conditions and limits, do = apply), `AbilityDamageSystem`, `SummonMinionAttemptSystem`,
  `MovePhaseSystem`, `TradePhaseSystem` (shop), `PhaseManagerSystem`, `CleanupBoardModelSystem`,
  `CheckAuraSystem`. A `BoardSystemCoordinator` orders them.
* **Board events** are the output. `BoardEventRendererSystem` and `BoardRenderer` on the Unity side
  consume events to animate. There are `BattleAutoPlay`, `BattleFastForward`, `BattlePause` — the
  client is a **replayer**, not a simulator. Debug wrappers (`BoardEvents.Debug`) exist for the
  developers' event inspector. **We will build the same: the Java engine emits an event log; React
  plays it back.**

### 4.4 Abilities are data, not code
`Spacewood.Core.Models.Abilities.{Catalogue, Triggers, Conditions, Effects, Parameters}` — an
ability = **Trigger + Conditions + Effects + Parameters (targets, amounts)**. Supporting symbols:
`EffectCondition, OnceCondition, ForeverCondition, TargetCondition, TargetsCondition,
ParameterCondition, AmountParameter, AttackParameter, AbilityRandomTarget, AbilityFilter,
LevelFilter, FreezeSpellFilter, EffectRepeatTrigger, ChargeTrigger`. The per-pet classes
(`AntAbility`, `BeaverAbility`, … hundreds) are thin configuration objects **[inferred]**.
**Lesson:** describe heroes in JSON with a small DSL (trigger/target/effect) and write *one*
interpreter. Adding a hero then means adding data + a test, not code.

### 4.5 The API surface (request DTO names)
Account: `RegisterGuestRequest`, `UpgradeGuestRequest` (guest → full account), `LoginRequest`,
`SteamLoginRequest`, `ForgotPasswordRequest`, `SendConfirmEmailRequest`.
Arena run: `QueueArenaRequest` (start/queue a run), `ReadyArenaRequest`, `StartTurnRequest`,
`RollShopRequest`, `SellMinionRequest`, `OrderMinionRequest`/`ReorderMinionRequest`,
`NameBoardRequest`, `CommitBoardRequest`, `BuildRequest`/`BuildResponse`, `EndTurnRequest`,
`FetchPlaybackRequest`/`FetchPlaybackByHistoryRequest` (battle replay), `HistoryRequest`,
`WatchArenaRequest`, `SpectatorMatchesRequest`, `AbandonArenaRequest`, `ChooseRequest` (pick one
of offered choices), `AddStatsRequest`/`FetchStatsResponse`.
Other modes: `QueueVersusRequest`, `ReadyVersusRequest`, `LeaveVersusRequest`, `PokeVersusRequest`,
`QueueBattleRoyaleRequest`, `QueueBullyRushRequest`, `JoinSkirmishRequest`, `StartSkirmishRequest`.
Meta: `AddOrUpdateDeckRequest` (custom packs), `ClaimRewardRequest`, `GiveGiftRequest`, `NewsResponse`.

**The clever bit — `BuildRequest` + `BuildResultHash` + `BuildHashNotMatching`:** SAP lets the client
simulate the whole shop phase locally for snappy UX, then sends the *list of actions* plus a *hash
of the resulting board*. The server **replays** the actions with the same RNG seed and rejects the
turn if its hash differs (error codes `BuildCantBuyPet`, `BuildCantRoll`, `BuildCantFreezeShop`,
`BuildCantOrderPets`, `BuildCantSellPet`, `BuildCantStackPets`, `BuildCantEndTurn`). That is
server-authoritative *and* latency-free. We will start simpler (one request per action — the
server is the only simulator) and keep this as a documented upgrade path.

### 4.6 Matchmaking & ghosts
`GhostParticipationId`, `ArenaMatch`, `ArenaMatchNotFound`, `ArenaMirror`, `ArenaOpponentAllPacks`,
`ArenaRank`, `ArenaStreak`, `ArenaDifficulty`, `BattlePairModel`, `BattleNotFoundWithParticipationId`.
Reading: when you end turn *N*, the server stores your board as a ghost for turn *N* and pairs you
with a stored ghost from another run at the same turn (likely bucketed by wins/lives, and
`ArenaMirror` suggests a fallback where you fight a mirror of your own team when no ghost exists)
**[inferred]**. The battle is computed once (`BattleId`) and the playback is fetched. **We copy
this exactly, with a bot-generated ghost pool as the cold-start fallback.**

### 4.7 Other things worth knowing
* `BoardRandom`, `BoardHash`, `BoardFuture`, `BoardFutureGold` — the resolver is **seeded and
  deterministic** (same seed + same actions → same board). This is non-negotiable for replay,
  anti-cheat, and testing. Our `GameRandom` does the same.
* `LevelupNoTrippleReward`, `FreezeDuplicates`, `BoardCapacity`, `LivesMax` — rules are
  **constants in one place** (`ArenaConstants`, `BoardConstants`).
* `ES3` save + `NewsResponse` + 14 localization tables: polish items for later, not for MVP.
* The game has a built-in **debug console** (Quantum Console) and **debug board events** — the devs
  can inspect any battle. Our `/dev` route with an event inspector is the same idea.

---

## 5. What this means for Forgotten Heroes (decisions)

1. **Same core loop, same numbers to start:** 10 gold, cost 3, roll 1, 5 slots, odd-turn tiers,
   5 lives / 10 wins. Tuning happens *after* the simulator exists, not before.
2. **Theme swap:** pets → heroes & monsters from the Time Fantasy sets; food → relics; faint → *fall*.
3. **Architecture copied from the real game:** Action → Resolver → Mutator → Event; data-driven
   abilities; deterministic seeded RNG; server computes, client replays.
4. **Asynchronous Arena only for MVP.** Versus (8-player sync with timers) needs websockets or
   polling plus lobbies — out of scope; noted as a stretch goal.
5. **Define our own tie-breaking** so battles are fully deterministic and explainable:
   attack desc → health desc → *owner's side* (the side that is "home" on odd turns, "away" on even)
   → position (front first). The exact rule matters less than it being written down and tested.
6. **Add a round cap** (e.g. 300 clashes) that ends the battle as a draw — defensive engineering SAP
   does not expose.
