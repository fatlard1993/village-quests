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
- **Three Commitments**: You can carry three quests, not thirty. Enough that a villager pointing you at their neighbour costs you nothing, few enough that you still have to choose. A quest nobody touches for half an hour goes quiet on its own, no reputation lost: letting a promise go cold is not the same as refusing it to someone's face. Quest types: fetch/gather, creation/repair, misnomer ethical tests, deep conversation, mystery/investigation, dialogue-based errands, time-sensitive, trouble with mobs, village development, and plot purchase.
- **Redirects belong to strangers**: A villager with nothing to offer will point you at someone who does, and that is how a newcomer meets the town. It is common when nobody knows you and almost gone once they do.
- **Misnomer Quests**: Ethical tests where a villager asks you to do something harmful out of fear or anger. Refusal is the right choice, and the villager answers it to your face. So is delay: "I need to think about that" closes the screen with nothing decided, and they may ask again. Recognition comes days later, if at all.
- **The village answers what it says**: When a villager opens with something that is between you (what you took, what you fixed, what they remember, the thing they asked last week), the buttons answer that, not the weather. Own a theft to someone's face and the rest of the village stops raising it; put something back in a village chest within a day and the next one to mention it says so. Deny it and the chorus keeps going.
- **Heard, never read off the screen edge**: A villager is heard in conversation, read on paper, or not at all. A confession plays one screen per beat, and the pause between beats is you choosing to stay for the next one.
- **Deep Quests**: Conversations disguised as quests. No clear objectives, minimal reputation shift, heavy emotional content. Over thirty variants, some brought on by what a villager remembers and the rest by who they are and what is around them, covering trauma, existential doubt, and reconciliation.
- **Messages are things you carry**: A delivery or message quest hands you a named item ("Message for Rowena", "Apology for Tomas"), and handing it over is a button you press in her dialogue, not something that happens because you walked close enough. The reply comes back as an item too, so the walk home is carrying something and the quest ends with a second handover rather than a proximity check nobody sees.
- **Delayed Recognition**: Actions have consequences days later, delivered through the mail system or changed dialogue tone.
- **Puzzles the village keeps**: Some quests leave something standing, so a village ends up wearing what was learned in it. A stretch of ground that spawns things at night gets lit and stays lit, which teaches the rule nobody can see: mobs need a block light of exactly zero, which is why torches that look sufficient are not. A village with no flag gets one, made at a loom, and where it flies is wherever the player thought it should.
- **Lamp posts, not torches**: A villager hands over six lanterns and six posts and asks for street lighting. The materials are the instruction: being given the parts says what shape the answer is, which "go and light the place" does not. Any fence or wall counts, by tag, so the prettier posts from other mods are just as correct.
- **Once per village, not once per player**: The flag is checked for before it is asked for, so the moment somebody raises one the quest stops existing for everyone. A record is only a record if there is one of it.
- **Village Presence Tracking**: The mod tracks your time in villages, social behaviors, and overnight stays. Presence is the foundation of trust.
- **Villager Gatherings**: Invisible events you are rarely invited to. Life happens without you.
- **Lore and Ontological Friction**: Villagers occasionally sense that something about their world is off. Lore is fragmentary, contradictory, and never fully explained.
- **Village Boss Bar**: Shows the village name when you are nearby. Decays for deeply trusted players; they don't need the reminder.
- **Plot System**: At high reputation (75+), villages set aside residential plots. When one is free, a villager offers it as a quest to a player the village holds at 50 or more: a contribution of 32 emeralds to the common fund, brought back and handed over like any other promise. Land is still earned through trust; the emeralds only follow it.
- **Behavioral Reputation Events**: Breaking beds, destroying job blocks, taking from a village's own chests (opening one to look is free), hitting villagers, and killing iron golems all affect reputation. Building beds, placing job blocks, crafting golems, and spending time in the village improve it. A change you cause shows as a small green or red burst, the only feedback there is: reputation is never a number.
- **Village chests are marked**: A chest counts as the village's because the village generated it, read from its loot table at chunk load and remembered from then on. A chest you place is yours, wherever you place it. The village's own are drawn with a villager's nose where the clasp would be, so the rule can be seen before it is broken; a plot you were granted stays plain for you and still warns a guest. Needs Pandorical on the client, which is what draws it. A chest looted before this existed cannot be recovered: its loot table is gone and nothing else tells it apart.
- **Villager Names**: Every villager gets a persistent name.
- **Advancements**: "Somebody's Errand", for finishing something a villager actually asked of you.

### Mod Integration API

External mods extend Village Quests through `justfatlard.village_quests.api`: `QuestRegistry` for profession-specific or universal quest generators, `DialogueRegistry` for custom dialogue options and response handlers, `LessonApi` for a trade a villager teaches over several lessons, and `VillageQuestsAPI` for reading a village's name, reading or changing a player's reputation there, and claiming a chest built after worldgen as the village's. Usage is on the Javadoc of each.

### Optional: `village-mail` Companion Mod

Village Quests integrates with the separate [village-mail](https://github.com/fatlard1993/village-mail) mod for delivering letters. When installed, players receive:
- Thank-you letters after misnomer quest refusals (delayed 1-3 days)
- Quest completion aftermath letters
- Gathering invitations
- Occasional villager letters
- Letters asking for help, from villagers who know you, once your standing with the village is 20 or more

Without `village-mail`, these letters are not sent: no errors, no warnings. A misnomer refusal is still recognised, said to your face the next time you speak to that villager; the rest simply do not happen. The integration uses reflection; there is no hard dependency.

### Other suite mods

- **block-tip**: looking at a village's own chest says "The village's", and block-tip's card stays out of the way of the village name bar.
- **village-builder**: builder villagers ask for 32 cobblestone toward the next building.
- **herobrine**: some villagers have stories about him, told once they know you well enough.

## Pandorical

Village Quests runs server-side, and Pandorical is required: the server will not load this mod without it. Every villager dialogue interaction (conversation text, response options, quest presentation, work requests) is built and driven as a Pandorical screen.

Clients are the optional half only in the sense that no Village Quests jar is needed on one. Pandorical itself is not optional: a player without it cannot talk to a villager through this mod at all. A plain right-click on a villager opens nothing for them; sneak + right-click still opens vanilla trading. `/quest` works either way, since the quest book is vanilla's own book reader.

## Commands

- **`/quest`**: Opens your quests as a book, one to a page. Same thing the "Quest Status" keybind opens, because a controller has no way to type a command. No quests? The book says so.
- **`/quest abandon`**: Begin abandoning the oldest quest you are carrying. The villager will remember.
- **`/quest abandon confirm`**: Confirm the abandonment. There's no undo.

Any player can use these; none need op.

## Configuration

The mod creates `config/village-quests.properties` on first run with commented defaults. 16 tunable values covering interaction cooldowns, quest rarity, gathering frequency, reputation decay, tick intervals, and mail limits. Delete the file to regenerate defaults. The ones that shape play are on the Village Quests page of the mod menu, for ops, and saved back to the file; the cooldowns, the decay rate and the tick intervals stay in the file. The two mail limits only show in the menu when village-mail is installed.

| Key | Default | Mod menu |
|-----|---------|----------|
| `interaction_cooldown_ms` | 500 | |
| `chest_penalty_cooldown_ms` | 60000 | |
| `block_placement_cooldown_ms` | 300000 | |
| `misnomer_chance` | 0.03 | Misnamed quests, percent (0-50) |
| `deep_quest_chance` | 0.05 | Deep quests, percent (0-100) |
| `deep_quest_cooldown_days` | 3 (real days) | Deep quest cooldown, days (0-30) |
| `gathering_chance` | 0.15 | Gathering quests, percent (0-100) |
| `gathering_min_days` | 3 | Gathering, fewest days (1-30) |
| `gathering_max_days` | 7 | Gathering, most days (1-60) |
| `reputation_decay_threshold` | 100 | Reputation decays above (0-1000) |
| `reputation_decay_rate` | 0.01 | |
| `presence_tracking_interval_ticks` | 200 | |
| `boss_bar_update_interval_ticks` | 20 | |
| `mail_per_player_per_day` | 2 | Mail per player a day (0-20) |
| `mail_base_per_village` | 5 | Mail per village a day (0-50) |

## Development

Installing, building and adding to it are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).

## The Core Truth

**Villages are communities, not content hubs.**

**The player is a participant, not a hero.**

For the complete philosophy, read [ETHOS.md](ETHOS.md).
