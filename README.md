# Village Quests

A Minecraft Fabric mod. Villages are communities, not content hubs.

**Mod ID:** `village-quests-justfatlard`. Note the suffix: an integration guarding on the bare `village-quests` silently never fires.

## What This Mod Does

Villages become places where:
- Life happens whether you're there or not
- Villagers have concerns beyond trading
- Your presence is required for belonging
- Trust is earned through restraint, not efficiency
- Some requests must be refused

## Features

- **Contextual Dialogue**: Villagers respond to time of day, weather, your reputation, and recent events. Dialogue is profession-aware, reputation-gated, and presence-sensitive. Right-click a villager to talk; sneak + right-click to go straight to vanilla trading.
- **Reputation as Trust**: An 8-band reputation system (Shunned through Elder Friend) tracked per-village, per-player. Reputation is earned through presence, restraint, and behavior, not grinding. Never displayed as a number.
- **One real conversation a day**: small talk with a given villager is offered once per day, because those replies carry reputation and re-opening the screen would let you earn standing by clicking. After that they still trade, still take a finished quest, still hand out a new one; only the chat that pays is spent.
- **Three Commitments**: You can carry three quests, not thirty. Enough that a villager pointing you at their neighbour costs you nothing, few enough that you still have to choose. A quest nobody touches for half an hour goes quiet on its own, no reputation lost: letting a promise go cold is not the same as refusing it to someone's face. Quest types: fetch/gather, creation/repair, misnomer ethical tests, deep conversation, mystery/investigation, dialogue-based errands, time-sensitive, village development, and plot purchase.
- **Redirects belong to strangers**: A villager with nothing to offer will point you at someone who does, and that is how a newcomer meets the town. It is common when nobody knows you and almost gone once they do.
- **Misnomer Quests**: Ethical tests where a villager asks you to do something harmful out of fear or anger. Refusal is the right choice. Recognition comes days later, if at all.
- **Deep Quests**: Conversations disguised as quests. No clear objectives, minimal reputation shift, heavy emotional content. 13 variants (5 memory-driven, 8 contextual) covering trauma, existential doubt, and reconciliation.
- **Messages are things you carry**: A delivery or message quest hands you a named item — "Message for Rowena", "Apology for Tomas" — and handing it over is a button you press in her dialogue, not something that happens because you walked close enough. The reply comes back as an item too, so the walk home is carrying something and the quest ends with a second handover rather than a proximity check nobody sees.
- **Delayed Recognition**: Actions have consequences days later, delivered through the mail system or changed dialogue tone.
- **Village Presence Tracking**: The mod tracks your time in villages, social behaviors, and overnight stays. Presence is the foundation of trust.
- **Villager Gatherings**: Invisible events you are rarely invited to. Life happens without you.
- **Lore and Ontological Friction**: Villagers occasionally sense that something about their world is off. Lore is fragmentary, contradictory, and never fully explained.
- **Village Boss Bar**: Shows the village name when you are nearby. Decays for deeply trusted players; they don't need the reminder.
- **Plot System**: At high reputation (75+), villages set aside residential plots. Land is earned through trust, not currency.
- **Behavioral Reputation Events**: Breaking beds, destroying job blocks, opening a village's own chests, hitting villagers, and killing iron golems all affect reputation. Building beds, placing job blocks, crafting golems, and spending time in the village improve it. A change you cause shows as a small green or red burst, the only feedback there is: reputation is never a number.
- **Village chests are marked**: A chest counts as the village's because the village generated it, read from its loot table at chunk load and remembered from then on. A chest you place is yours, wherever you place it. The village's own are drawn with a villager's nose where the clasp would be, so the rule can be seen before it is broken; a plot you were granted stays plain for you and still warns a guest. Needs Pandorical on the client, which is what draws it. A chest looted before this existed cannot be recovered: its loot table is gone and nothing else tells it apart.
- **Villager Names**: Every villager gets a persistent name.

### Mod Integration API

External mods extend Village Quests through two registries in `justfatlard.village_quests.api`: `QuestRegistry` for profession-specific or universal quest generators, and `DialogueRegistry` for custom dialogue options and response handlers. Usage is on the Javadoc of each.

### Optional: `village-mail` Companion Mod

Village Quests integrates with the separate [village-mail](https://github.com/justfatlard/village-mail) mod for delivering letters. When installed, players receive:
- Thank-you letters after misnomer quest refusals (delayed 1-3 days)
- Quest completion aftermath letters
- Gathering invitations
- Occasional villager letters

Without `village-mail`, these degrade to chat whispers: no errors, no warnings. The integration uses reflection; there is no hard dependency.

## Pandorical

Village Quests runs server-side, and Pandorical is a hard dependency (`fabric.mod.json`): the server will not load this mod without it. Every villager dialogue interaction (conversation text, response options, quest presentation, work requests) is built and driven as a Pandorical screen.

Clients are the optional half only in the sense that no Village Quests jar is needed on one. Pandorical itself is not optional: a player without it cannot talk to a villager through this mod at all, and falls through to vanilla trading.

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

## Commands

- **`/quest`**: Shows your current quest progress and description. No active quest? It tells you.
- **`/quest abandon`**: Begin abandoning your active quest. The villager will remember.
- **`/quest abandon confirm`**: Confirm the abandonment. There's no undo.

## Configuration

The mod creates `config/village-quests.properties` on first run with commented defaults. 16 tunable values covering interaction cooldowns, quest rarity, gathering frequency, reputation decay, tick intervals, and mail limits. Delete the file to regenerate defaults.

## Building

Village Quests builds against Pandorical's live source, not a published artifact: `settings.gradle` includes `../pandorical`. Check both out side by side or the build fails before it starts.

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## License

MIT, see [LICENSE](LICENSE).

## The Core Truth

**Villages are communities, not content hubs.**

**The player is a participant, not a hero.**

For the complete philosophy, read [ETHOS.md](ETHOS.md).
