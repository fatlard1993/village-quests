# Village Quests - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## For Contributors

**Before contributing, read [ETHOS.md](ETHOS.md).**

This mod has a specific vision that must be protected. Features that optimize, gamify, or center the player will be rejected.

Also review:
- [VISION_ENFORCEMENT.md](VISION_ENFORCEMENT.md): Code patterns that preserve the vision
- [DIALOGUE_WRITING_GUIDE.md](DIALOGUE_WRITING_GUIDE.md): How to write human dialogue
- [DIALOGUE_EXAMPLES.md](DIALOGUE_EXAMPLES.md): Quick reference for dialogue

### Adding Dialogue

All dialogue is hardcoded in Java inside `DialogueContent.java` and individual quest classes. To add or modify dialogue, edit Java source and recompile. See the writing guide for tone, the 8-band reputation system, and what not to write.

### Key Files

| File | Responsibility |
|------|---------------|
| `VillageQuests.java` | Entry point, event registration, tick scheduling |
| `DialogueManager.java` | Dialogue flow, reputation-based response filtering, quest presentation, work requests |
| `DialogueContent.java` | Every dialogue line, hardcoded in Java; no external data files |
| `ReputationManager.java` | The 8 bands, percentage scaling, persistence. High trust is fragile; redemption stays open |
| `VillageManager.java` | Village discovery via POI bed clusters, center tracking, caching |
| `ActiveQuestManager.java` | Quest lifecycle |
| `VillagerQuest.java` | Quest base class and generation; 9 concrete types |
| `MisnomerQuest.java` | Ethical test quests |
| `PresenceTracker.java` | Per-player, per-village presence density and behavior |
| `ContextualLoreManager.java` | Item-triggered, biome-aware, reputation-gated lore |
| `RandomKindnessHandler.java` | Rare villager gifts (children at 25+ rep, adults at 75+) |
| `VillagerPersonality.java` | Per-villager personality traits |
| `FirstEncounterTracker.java` | First-time player guidance |
| `ScheduledMessages.java` | Delayed message delivery |
| `VillageQuestsConfig.java` | The 16 tunable values in `config/village-quests.properties` |
| `VillageQuestsCommands.java` | `/quest` |

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## Building

Village Quests builds against Pandorical's live source, not a published artifact: `settings.gradle` includes `../pandorical`. Check both out side by side or the build fails before it starts.

```bash
./gradlew build
```

The built jar will be in `build/libs/`.
