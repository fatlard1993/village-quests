# Village Quests

A Minecraft Fabric mod. Villages are communities, not content hubs.

**Mod ID:** `village-quests-justfatlard`
**Minecraft:** 1.21.11 | **Fabric Loader:** 0.18.0+ | **Java:** 21+

## What This Mod Does

Villages become places where:
- Life happens whether you're there or not
- Villagers have concerns beyond trading
- Your presence is required for belonging
- Trust is earned through restraint, not efficiency
- Some requests must be refused

## Features

- **Contextual Dialogue** — Villagers respond to time of day, weather, your reputation, and recent events. Dialogue is profession-aware, reputation-gated, and presence-sensitive. Right-click a villager to talk; sneak + right-click to go straight to vanilla trading.
- **Reputation as Trust** — An 8-band reputation system (Shunned through Elder Friend) tracked per-village, per-player. Reputation is earned through presence, restraint, and behavior — not grinding. Never displayed as a number.
- **Single Active Quest** — One commitment at a time. Prevents hoarding, forces intentionality. Quest types: fetch/gather, creation/repair, misnomer ethical tests, deep conversation, mystery/investigation, dialogue-based errands, time-sensitive, village development, and plot purchase.
- **Misnomer Quests** — Ethical tests where a villager asks you to do something harmful out of fear or anger. Refusal is the right choice. Recognition comes days later, if at all.
- **Deep Quests** — Conversations disguised as quests. No clear objectives, minimal reputation shift, heavy emotional content. 13 variants (5 memory-driven, 8 contextual) covering trauma, existential doubt, and reconciliation.
- **Delayed Recognition** — Actions have consequences days later, delivered through the mail system or changed dialogue tone.
- **Village Presence Tracking** — The mod tracks your time in villages, social behaviors, and overnight stays. Presence is the foundation of trust.
- **Villager Gatherings** — Invisible events you are rarely invited to. Life happens without you.
- **Lore and Ontological Friction** — Villagers occasionally sense that something about their world is off. Lore is fragmentary, contradictory, and never fully explained.
- **Village Boss Bar** — Shows the village name when you are nearby. Decays for deeply trusted players — they don't need the reminder.
- **Plot System** — At high reputation (75+), villages set aside residential plots. Land is earned through trust, not currency.
- **Behavioral Reputation Events** — Breaking beds, destroying job blocks, opening village chests, hitting villagers, and killing iron golems all affect reputation. Building beds, placing job blocks, crafting golems, and spending time in the village improve it.
- **Villager Names** — Every villager gets a persistent name.

### Architecture

The mod is organized around these systems:

- **DialogueManager** (~1,276 lines) — Dialogue flow, reputation-based response filtering, quest presentation, work request coordination, first-meeting persistence
- **DialogueContent** (~2,963 lines) — All dialogue registration including trade text, work inquiry text, weather greetings, crowd/privacy prefixes, first-meeting greetings, gossip (hardcoded in Java, no external data files)
- **ReputationManager** — 8-band system with percentage-based scaling. High trust is fragile; redemption paths stay open.
- **VillageManager** — Village discovery via POI bed clusters, center tracking, caching
- **Quest system** — Abstract `VillagerQuest` with 9 concrete types. External mods can register generators via `QuestRegistry`.
- **DialogueRegistry** — API for external mods to add custom dialogue response options and handlers
- **PresenceTracker** — Per-player, per-village presence density and behavior tracking
- **ContextualLoreManager** — Item-triggered, biome-aware, reputation-gated lore delivery
- **VillageQuestsConfig** — Configuration system (16 tunable values) loaded from `config/village-quests.properties`
- **VillageQuestsCommands** — Player commands (`/quest`)
- **VillagerPersonality** — Per-villager personality traits
- **FirstEncounterTracker** — First-time player guidance
- **ScheduledMessages** — Delayed message delivery
- **RandomKindnessHandler** — Rare villager gift events (children at 25+ rep, adults at 75+)
- **ZombieDoorBreakMixin** — Zombie door break detection (7th mixin alongside VillagerEntity, VillagerMovement, VillagerDamage, BlockBreak, ContainerAccess, and FireUse)

### Mod Integration API

External mods can extend Village Quests through two registries:

- **`QuestRegistry`** — Register profession-specific or universal quest generators
- **`DialogueRegistry`** — Register custom dialogue options and response handlers

Both are in `justfatlard.village_quests.api`. See Javadoc on each class for usage.

### Optional: `village-mail` Companion Mod

Village Quests integrates with the separate [village-mail](https://github.com/justfatlard/village-mail) mod for delivering letters. When installed, players receive:
- Thank-you letters after misnomer quest refusals (delayed 1-3 days)
- Quest completion aftermath letters
- Gathering invitations
- Occasional villager letters

Without `village-mail`, these degrade to chat whispers — no errors, no warnings. The integration uses reflection; there is no hard dependency.

## For Contributors

**Before contributing, read [ETHOS.md](ETHOS.md).**

This mod has a specific vision that must be protected. Features that optimize, gamify, or center the player will be rejected.

Also review:
- [VISION_ENFORCEMENT.md](VISION_ENFORCEMENT.md) — Code patterns that preserve the vision
- [DIALOGUE_WRITING_GUIDE.md](DIALOGUE_WRITING_GUIDE.md) — How to write human dialogue
- [DIALOGUE_EXAMPLES.md](DIALOGUE_EXAMPLES.md) — Quick reference for dialogue
- [GARDEN_REVIEW.md](GARDEN_REVIEW.md) — Operational health: known issues, open items, and architectural debt

### Adding Dialogue

All dialogue is hardcoded in Java inside `DialogueContent.java` and individual quest classes. To add or modify dialogue, edit Java source and recompile. See the writing guide for tone, the 8-band reputation system, and what not to write.

### Key Files

| File | Responsibility |
|------|---------------|
| `VillageQuests.java` | Mod entry point, event registration, tick scheduling |
| `DialogueManager.java` | Dialogue flow and response handling |
| `DialogueContent.java` | All dialogue definitions |
| `ReputationManager.java` | Reputation persistence and events |
| `VillageManager.java` | Village discovery and tracking |
| `ActiveQuestManager.java` | Quest lifecycle |
| `VillagerQuest.java` | Quest base class and generation |
| `MisnomerQuest.java` | Ethical test quests |
| `VillageQuestsConfig.java` | Configuration (16 tunable values) |
| `VillageQuestsCommands.java` | `/quest` commands |
| `VillagerPersonality.java` | Per-villager personality traits |
| `FirstEncounterTracker.java` | First-time player guidance |
| `ScheduledMessages.java` | Delayed message delivery |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) (0.18.0+) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Place the mod jar in your `mods/` folder
3. Optionally install `village-mail` for the full mail experience
4. Launch Minecraft 1.21.11

### For Modpack Developers

- **Mod ID:** `village-quests-justfatlard`
- **Optional dependency:** `village-mail` (soft dependency, reflection-based)
- **No hard dependencies** beyond Fabric API

## Commands

- **`/quest`** — Shows your current quest progress and description. No active quest? It tells you.
- **`/quest abandon`** — Begin abandoning your active quest. The villager will remember.
- **`/quest abandon confirm`** — Confirm the abandonment. There's no undo.


## Configuration

The mod creates `config/village-quests.properties` on first run with commented defaults. 16 tunable values covering interaction cooldowns, quest rarity, gathering frequency, reputation decay, tick intervals, and mail limits. Delete the file to regenerate defaults.

## Building

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## License

MIT License — See LICENSE file for details.

## The Core Truth

**Villages are communities, not content hubs.**

**The player is a participant, not a hero.**

For the complete philosophy, read [ETHOS.md](ETHOS.md).
