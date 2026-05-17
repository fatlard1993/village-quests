# Quest Chains: Seeds That Bloom

*Small quests that turn out to have been the first beat of something the village was already living through.*

---

## Design Principles

- Beat 1 is **indistinguishable** from any normal quest of its type. No special UI, no chain indicator, no "Part 1 of 3." The player does not know they are in a chain.
- The connection between beats is carried by **villager dialogue and memory**, not by quest metadata. A villager says something that references a past event. The player connects the dots themselves.
- The payoff is **understanding**, not loot. The player realizes what they were part of.
- Beats are separated by **real time** (days to weeks of play). The VillagerMemory system's decay and resurfacing mechanics are the clock.
- Each beat uses a **different quest type** so the chain doesn't feel like a repeating pattern.

## New Infrastructure Needed

All six chains below require one shared addition:

**`SeedMemory`**: A lightweight extension to VillagerMemory for tracking chain seeds. Unlike high-impact memories, seed memories are:
- **Low-strength** (start at 0.5, not 1.0) — they're small events
- **Medium decay** (60-day max) — long enough to bridge the gap, short enough to expire naturally if the player doesn't return
- **Cross-villager**: Some seeds are stored on the *village*, not the villager, because the consequence shows up in a different villager's dialogue
- **No dialogue modifier on their own** — they never surface as greeting callbacks. They only matter when checked during quest generation.

New MemoryTypes needed:
```
BREAD_DELIVERED_TO_HUNGRY    // Fed a struggling family (low impact, 60-day decay)
FENCE_REPAIRED_NEAR_ANIMALS  // Fixed fencing near animal pens (low impact, 60-day decay)
HONEY_DELIVERED_FOR_SICK     // Brought medicine-adjacent item for someone's spouse (medium impact, 90-day)
SAPLING_PLANTED_FOR_STRANGER // Planted a tree a stranger asked for (low impact, 60-day)
NOTE_DELIVERED_UNSENT        // Carried a message someone couldn't send themselves (medium impact, 90-day)
TOOL_LENT_TO_CHILD           // Gave tools to a kid who was trying to learn (low impact, 60-day)
```

These are recorded silently during normal quest completion — the existing `onComplete()` method already has hooks for memory recording. The player sees nothing different.

**Village-level seed tracking**: A new `Map<String, Long>` in the `Village` class, `villageSeeds`, keyed by seed name and timestamped. This allows Beat 2 to fire from a *different villager* than Beat 1, which is critical — the chain should feel like the village is alive, not like one NPC has a scripted arc.

---

## Chain 1: The Bread That Saved a Family

*A fetch quest feeds someone. Weeks later, that someone's child is standing on their own two feet.*

### Beat 1 — Fetch Quest (completely normal)

**Quest type**: FETCH
**Trigger**: Any farmer or butcher, reputation >= 25
**What the villager says**:
> Maren: "The family by the south wall — they haven't come to the well in two days. Bring them bread. Four loaves. They won't ask."

**What the player sees**: A standard profession-appropriate fetch quest. Bring 4 bread to a location near the village edge. Identical to a dozen other fetch quests they've done.

**On completion**: Record `BREAD_DELIVERED_TO_HUNGRY` on the *receiving family's* parent villager (the one living near the south wall). Also record a village-level seed: `"fed_family:<receiverUUID>"` with timestamp.

**Dialogue on delivery** (from the receiver, not the quest giver):
> *takes the bread without looking at you*
> "...thank you."

Nothing more. No fanfare.

### The Connection (14-28 real days later)

**What triggers Beat 2**: During quest generation for any villager in the same village, check village seeds for `"fed_family:<uuid>"` where the timestamp is 14-28 days old. If the referenced villager is still alive and the player's reputation >= 50, Beat 2 can fire. 15% chance per eligible quest generation roll.

### Beat 2 — Dialogue Quest (the growing)

**Quest type**: DIALOGUE (deliver_message)
**What the villager says** (a *different* villager than Beat 1 — a neighbor, a shopkeeper):
> Soren: "The kid from that family — you know the one, by the south wall? She's been asking about you. Says she wants to learn to farm. Can you tell her mother I said she can work my fields in the mornings? I don't want to overstep."

**The player's moment**: "Wait — the family by the south wall. I brought them bread weeks ago." The quest is still just a message delivery. Walk to the mother, deliver the message.

**When the player delivers the message, the mother says**:
> *looks at you for a long time*
> "She stopped crying at night. After you came. I don't know if it was the bread or just knowing someone noticed."
> "Tell Soren yes. She can work his fields."

**On completion**: Record `FED_THE_HUNGRY` (already exists) on the mother's UUID if not already present. The memory system takes it from here — future greeting callbacks will reference the feeding. The chain's narrative work is done.

### Beat 3 — The Bloom (emergent, not scripted)

There is no Beat 3 *quest*. The bloom is what happens naturally:
- The mother now has a `FED_THE_HUNGRY` memory. Her greetings will occasionally reference it: *"I eat now. I don't steal. Because of what you did."* or the fading variant: *"You were kind to me once. I think about that."*
- If the child villager grows up (baby -> adult), and the player has high reputation, the existing apprentice quest system can fire from that child's parent. The apprentice chain was *seeded* by bread.
- If the player is absent for weeks and returns, the mother's `FED_THE_HUNGRY` memory may have faded to the softer dialogue. Time passed. Life went on. But the bread mattered.

### Implementation Notes

- Beat 1 reuses `generateBasicQuest` or `generateHarderFetchQuest` — just add a weighted variant in the farmer/butcher profession pool that targets "a struggling family" with bread. The quest itself is mechanically identical to existing fetch quests.
- Beat 2 hooks into `generateDialogueQuest` with a pre-check for village seeds. If the seed exists and conditions match, it overrides the normal dialogue generation with this specific variant.
- No new quest classes needed. Both beats use existing `FetchQuest` and `DialogueQuest` classes. The only new code is the seed check during generation and the specific dialogue strings.

---

## Chain 2: The Fence You Fixed

*A creation quest repairs something broken. Weeks later, a mystery reveals what the fence was holding back.*

### Beat 1 — Creation Quest (completely normal)

**Quest type**: CREATION (existing repair variant, or a new "mend fence" sub-type of the repair family)
**Trigger**: Any villager, reputation >= 25, animal pens detected near village
**What the villager says**:
> Thatch: "The fence behind my place — two sections down. Probably the wind. Can you patch it? I've got no spare wood."

**What the player sees**: Place 6 fence gates/fences near a gap in existing fencing. Standard creation quest. They fix it and move on.

**On completion**: Record `FENCE_REPAIRED_NEAR_ANIMALS` on the quest-giving villager. Village seed: `"fence_repaired:<villagerUUID>:<blockPos>"` with timestamp.

### The Connection (7-21 real days later)

**What triggers Beat 2**: During mystery quest generation (`MysteryQuest.generateMysteryQuest`), if the village has a `"fence_repaired"` seed that's 7-21 days old, there's a 20% chance the mystery is specifically about animals that would have escaped through that fence. The mystery's backstory references the repair.

### Beat 2 — Mystery Quest (the growing)

**Quest type**: MYSTERY (missing_animal)
**What the villager says** (a *different* villager — the animal's owner):
> Elara: "Three of my sheep are gone. But the pen is closed. Every gate. Every fence. I don't understand."

**Standard mystery investigation proceeds.** The player searches for clues. But when they find the clue, the resolution text is different:

**Clue discovery dialogue**:
> "The tracks lead to the back fence. The section that was repaired — recently, by the look of it. Before the repair... there's a gap. Old tracks. Weeks old. Something was coming through here *before* someone fixed it."

**Resolution (confrontation phase)**:
> Elara: "You're telling me the fence was down for days? And something was getting in?"
> Elara: "Wait — someone fixed that fence. Thatch told me. Was that you?"
> *She stares at you.*
> Elara: "If you hadn't fixed it when you did... that gap goes right to the forest. Wolves. I would have lost all of them."
> Elara: "I lost three. But I have nine because of you. I didn't even know I was in danger."

**On completion**: Record `ANIMAL_RESCUED` (already exists) on Elara. The mystery resolves as "the animals that escaped got out through the old gap *before* the repair, not after." The repair saved the rest.

### Beat 3 — The Bloom

- Elara's `ANIMAL_RESCUED` memory generates greeting callbacks: *"You brought them back. I didn't think anyone would."*
- But the deeper bloom is in Thatch's dialogue. He never asked you to save anyone's sheep. He just had a broken fence. Months later, if Thatch gets a deep quest about the village, his dialogue might reference: *"I asked you to fix a fence. Just a fence. Turns out it was more than that."* This hooks into the existing `getDialogueModifier` for THANKS context.

### Implementation Notes

- Beat 1 is a minor variant of the existing creation/repair quest family. The `RepairDoorQuest` pattern works — create a `RepairFenceQuest` that checks for fence gaps near animal pens.
- Beat 2 hooks into `MysteryQuest.generateMysteryQuest`. When `mysteryType == MISSING_ANIMAL`, check village seeds for a recent fence repair. If found, use the chain-aware backstory and resolution text instead of the default.
- The "wait, that was YOU?" moment in the mystery resolution is the entire payload. It's carried by dialogue, not mechanics.

---

## Chain 3: The Honey and the Recovery

*A fetch quest brings medicine. Months later, the person you saved does something no villager has ever done — they help YOU.*

### Beat 1 — Fetch Quest (completely normal)

**Quest type**: FETCH
**Trigger**: Any villager, reputation >= 50 (they trust you enough to ask about family)
**What the villager says**:
> Grim: "My wife hasn't left the house in a week. The cleric says honey might help — something about the throat. I can't leave her alone long enough to find any. Can you bring some? A bottle."

**What the player sees**: Bring 1 honey bottle. Standard fetch. They might have one already.

**On completion**: Record `HONEY_DELIVERED_FOR_SICK` on Grim's UUID. Village seed: `"healed_spouse:<grimUUID>:<wifeUUID>"` with timestamp.

**Dialogue on delivery**:
> Grim: "Thank you. I'll bring it to her now."
> *He turns away quickly. You can't tell if he's hopeful or afraid.*

### The Connection (21-45 real days later)

This chain has the longest gap. The illness takes time to resolve. During quest generation for the wife's UUID (or any villager in the same village), check for `"healed_spouse"` seed where:
- The seed is 21-45 days old
- The wife villager is still alive
- The player's reputation >= 50
- 10% chance per eligible roll (this should be rare)

### Beat 2 — No Quest At All (the bloom)

This is the rule-breaker. There is no Beat 2 quest. Instead, the wife villager gains a unique one-time dialogue that fires when the player talks to her. Not a quest offer — just words.

**When the player talks to the wife** (instead of a quest offer, 10% chance, once ever):
> *She's standing outside. First time you've seen her outside.*
> "You don't know me. But I know you brought the honey."
> *She holds something out. A cookie.*
> "I baked this morning. First time in weeks. Grim said you might come by."
> *She gives you 1 cookie.*
> "That's not payment. I don't owe you anything. I just wanted to."

**The player receives 1 cookie.** No reputation change. No quest completion. No fanfare. A villager gave them something because she wanted to. In a system where the player is always the one giving, this reversal is the entire point.

**On interaction**: Record `CARETAKING_RECEIVED` (already exists) on the wife's UUID. The wife's future greetings may reference this moment through the existing memory-dialogue system.

### Implementation Notes

- Beat 1 is a standard fetch quest — add "honey for a sick spouse" to the profession-specific personal request pools that already exist. The honey bottle is the item. No new quest class.
- Beat 2 requires a new check in the dialogue/greeting system. When a villager would normally offer a quest or give a greeting, check village seeds for `"healed_spouse"` where this villager is the spouse. If conditions match, deliver the one-time gift dialogue instead of a quest. Mark the seed as consumed so it never fires again.
- The cookie is given via `player.giveItemStack(new ItemStack(Items.COOKIE, 1))`. One cookie. The smallest possible gift. The restraint is the point.
- This chain is the hardest to implement because it breaks the player-gives/villager-receives pattern. But that's exactly why it matters. The ethos says "the player is a participant, not a hero." A villager giving the player a cookie because she felt like it is the purest expression of that.

---

## Chain 4: The Sapling and the Grief

*A creation quest plants a tree. Weeks later, a deep quest reveals it was planted on a grave.*

### Beat 1 — Creation Quest (completely normal)

**Quest type**: CREATION (existing `PlantFlowersQuest` variant — or a new sapling variant)
**Trigger**: Any villager, reputation >= 25, clear weather
**What the villager says**:
> Wren: "There's a bare patch on the hill. East side. Always bothered me. Can you plant a sapling there? Oak, I think. Something that'll last."

**What the player sees**: Plant 1 oak sapling at a specific location. Trivially easy. They plant it and leave.

**On completion**: Record `SAPLING_PLANTED_FOR_STRANGER` on Wren's UUID. Village seed: `"tree_planted:<wrenUUID>:<blockPos>"` with timestamp.

**Dialogue on completion**:
> Wren: "Good. That's good. Thank you."
> *She walks toward the hill but doesn't go all the way.*

### The Connection (14-30 real days later)

Beat 2 triggers from Wren specifically — she's the one who asked, and she's the one carrying the weight. During deep quest generation, if Wren has `SAPLING_PLANTED_FOR_STRANGER` as an active memory and reputation >= 50, there's a 15% chance her deep quest is about this.

### Beat 2 — Deep Quest (the bloom)

**Quest type**: DEEP
**What Wren says** (the deep quest description):
> Wren: "The tree you planted. On the hill. Can you bring me a flower? Any flower. I need to visit."

**What the player sees**: Bring 1 flower (any) to Wren. Standard deep quest delivery.

**When the player delivers the flower, the sequenced dialogue**:

> Wren: "My daughter used to play on that hill."
> *3 second pause*
> Wren: "She was small. Too small. The fever took her in the autumn."
> *3 second pause*
> Wren: "I couldn't plant anything there. For a long time. It felt wrong. Like covering her up."
> *3 second pause*
> Wren: "But you did it. You didn't know. And the tree is growing."

**Acknowledgment beat**: *Neither of you says anything for a while.*

**The realization**: The player planted a tree on a dead child's favorite spot. They didn't know. Wren couldn't do it herself — the grief was too heavy. She asked a stranger to do the thing she couldn't. And the stranger did it because it was just a quest.

**On completion**: No new memory needed. The deep quest completion itself is the payoff. Wren's existing dialogue modifiers from the deep quest system handle the aftermath.

### Implementation Notes

- Beat 1 is a variant of the existing `PlantFlowersQuest` or a new `PlantSaplingQuest` that targets a specific BlockPos and requires 1 sapling. Mechanically simple.
- Beat 2 is a new DeepQuest subclass in `DeepQuestDialogues.java` — `GriefTreeQuest`. It checks for `SAPLING_PLANTED_FOR_STRANGER` memory during `checkForDeepQuest`. The deep quest requires any flower (check for any item tagged as `minecraft:flowers`).
- The dialogue is the only complex part. It follows the existing `sendSequencedDialogue` pattern with 4 messages.
- The chain only works if the sapling was planted by the player. If the tree has grown (oak sapling -> oak tree over MC days), the deep quest dialogue can reference that: *"The tree is taller than I expected."* If it's still a sapling: *"It's still small. Like she was."* Check `world.getBlockState(pos)` for sapling vs. log.

---

## Chain 5: The Note You Carried

*A dialogue quest delivers a message. Weeks later, you learn the message was a goodbye.*

### Beat 1 — Dialogue Quest (completely normal)

**Quest type**: DIALOGUE (deliver_message)
**Trigger**: Any villager, reputation >= 25
**What the villager says**:
> Aldric: "I wrote something for Petra. At the other end of the village. I'd bring it myself but — just take it, please. Tell her I'm sorry I didn't come in person."

**What the player sees**: Standard message delivery. Walk to Petra, deliver the message. The "I'm sorry I didn't come in person" registers as social awkwardness, not urgency. Every other dialogue quest has similar flavor.

**When the player delivers to Petra**:
> Petra: *reads silently*
> "...Tell him I understand."
> *She folds the note carefully.*
> "Thank you for bringing this."

**On completion**: Record `NOTE_DELIVERED_UNSENT` on Aldric's UUID. Village seed: `"note_delivered:<aldricUUID>:<petraUUID>"` with timestamp.

### The Connection (7-21 real days later)

During quest generation, if the village has a `"note_delivered"` seed and Aldric's villager entity is no longer alive (died to zombie, raid, or despawned), Beat 2 fires from Petra.

**The critical condition**: Aldric must be dead. If Aldric is still alive, the chain never advances — it was just a normal message delivery. The chain only blooms in the presence of loss.

If Aldric IS dead and the seed is 7+ days old and Petra is still alive, Beat 2 has a 25% chance to fire when Petra would generate a quest.

### Beat 2 — Deep Quest (the bloom)

**Quest type**: DEEP
**What Petra says** (instead of her normal quest):
> Petra: "Aldric is gone. You knew him. You carried his last words to me and you didn't even know."
> "Bring me a book. I want to write down what he said. Before I forget the exact words."

**What the player sees**: Bring 1 book (or book and quill) to Petra. When delivered:

> Petra: "The note said he was sorry. For all the years he was angry about the land dispute."
> *3 second pause*
> Petra: "We argued for a decade. About a fence line. A fence line."
> *3 second pause*
> Petra: "He sent you because he couldn't face me. And then he was gone before I could tell him it didn't matter."
> *3 second pause*
> Petra: "You were the last person to stand between us. You didn't know that either."

**Acknowledgment beat**: *You don't leave right away.*

### Implementation Notes

- Beat 1 is a standard `DialogueQuest` with `DELIVER_MESSAGE` type. The only addition is recording the seed on completion. The dialogue variant "I'm sorry I didn't come in person" is added to the existing message pool.
- Beat 2 requires a death check. The `VillagerMemory` system already handles UUID tracking. During quest generation, check `world.getEntity(aldricUUID)` — if null and the seed exists, Aldric is gone.
- **Critically, this chain only fires when a villager dies naturally.** The player cannot trigger it intentionally. It emerges from the intersection of a mundane quest and unpredictable village life. Most of the time, Aldric lives, and the note was just a note. The rare time he doesn't — the player realizes they carried a dying man's reconciliation and didn't know.
- New DeepQuest subclass: `LastWordsQuest` in `DeepQuestDialogues.java`.

---

## Chain 6: The Tools You Gave a Child

*A fetch quest hands tools to a kid who's trying to learn. Months later, the kid is an apprentice — and they remember who believed in them first.*

### Beat 1 — Fetch Quest (completely normal)

**Quest type**: FETCH
**Trigger**: Any villager with a baby villager nearby, reputation >= 25
**What the villager says**:
> Corwin: "My kid keeps picking up sticks and pretending they're tools. Won't stop. Could you bring a wooden pickaxe and a wooden shovel? I know they'll break in a day but — let the kid feel like they're helping."

**What the player sees**: Bring 1 wooden pickaxe and 1 wooden shovel. Cheap, trivial. The quest is about humoring a child.

**On completion**: Record `TOOL_LENT_TO_CHILD` on Corwin's UUID. Village seed: `"child_tools:<corwinUUID>:<childUUID>"` with timestamp.

**Dialogue on delivery**:
> Corwin: "Watch this."
> *He hands the tools to the child. The child holds the pickaxe upside down.*
> Corwin: "They'll figure it out."

### The Connection (30-60 real days later)

This is the longest chain. It requires:
- The child villager grows up (baby -> adult, which happens naturally over MC time)
- The `"child_tools"` seed is 30-60 days old
- The grown child is still alive
- 10% chance per eligible roll

When the grown child (now an adult villager) would generate a quest, check village seeds. If they were the child who received tools, their quest is different.

### Beat 2 — Apprentice Chain Kickoff (the growing)

**Quest type**: The existing apprentice quest system (APPRENTICE_STARTED path)
**What the grown child says** (first time the player talks to them as an adult):

Before offering the apprentice quest, they say:
> "You probably don't remember. You brought me a pickaxe when I was small. Wooden. It broke the same day."
> "But I remember holding it. And thinking: someone thinks I can do this."
> "I want to learn properly now. Will you help me?"

Then the standard apprentice quest chain begins (Phase 1 -> Phase 2 -> Phase 3, already implemented via `APPRENTICE_STARTED` / `APPRENTICE_PRACTICING` / `APPRENTICE_GRADUATED` memories).

### Beat 3 — The Bloom (graduation)

When the apprentice chain completes (Phase 3, `APPRENTICE_GRADUATED`), the graduation dialogue has an additional line that references the wooden tools:

> "My first tool was wooden. You brought it. It broke before sundown."
> "This one won't break."
> *They hold up the item they crafted — their first real creation.*

### Implementation Notes

- Beat 1 is a standard fetch quest. Add "wooden tools for a child" to the personal request pool when baby villagers are detected nearby. The detection logic for baby villagers already exists in `findNearbyVillagerTarget` (the `!v.isBaby()` filter implies baby detection is possible).
- Beat 2 hooks into the existing apprentice quest generation. Before generating an apprentice quest, check if the apprentice villager's UUID matches any `"child_tools"` village seed. If so, prepend the memory dialogue.
- Beat 3 hooks into the existing `APPRENTICE_GRADUATED` completion handler. Check the same seed. If present, add the wooden-tool callback line to the graduation dialogue.
- This chain has the highest emotional payoff because it spans the most time. A player who gave wooden tools to a baby months ago and then sees that baby grow up, remember them, and become a craftsperson — that's the kind of moment the ethos describes: *"They tell someone: 'There's this villager in my world...'"*

---

## Anti-Patterns to Avoid

These chains must NOT:

1. **Show any chain indicator.** No "Part 1 of 3." No quest chain UI. No breadcrumbs. The player connects the dots themselves or not at all.

2. **Force the chain to complete.** If conditions aren't met (villager died, player didn't return, memory decayed), the chain silently expires. Most seeds never bloom. That's correct. The rare ones that do bloom feel miraculous *because* they weren't guaranteed.

3. **Make Beat 1 feel special.** The moment Beat 1 feels different from a normal quest, the design has failed. No special dialogue tone, no lingering camera, no hint music. It's bread. It's a fence. It's a sapling.

4. **Reward the chain mechanically.** No special loot, no reputation bonus beyond what the individual quests normally give, no achievement. The reward is *understanding*.

5. **Reference the chain in the quest log or objective text.** The objective for Beat 2 never says "remember when you did X." The *villager* says it, in dialogue, because they're a person who remembers things.

---

## Probability and Pacing

Expected chain completion rates (rough estimates):

| Chain | Beat 1 Frequency | Beat 2 Fires If... | Expected Bloom Rate |
|---|---|---|---|
| Bread/Family | Common fetch | Player returns in 14-28 days, rep >= 50, 15% roll | ~5% of seeds bloom |
| Fence/Mystery | Uncommon creation | Mystery generates in 7-21 days, 20% roll | ~8% of seeds bloom |
| Honey/Recovery | Uncommon fetch | Player returns in 21-45 days, wife alive, 10% roll | ~3% of seeds bloom |
| Sapling/Grief | Uncommon creation | Player returns in 14-30 days, rep >= 50, 15% roll | ~6% of seeds bloom |
| Note/Goodbye | Common dialogue | Aldric *dies* in 7-21 days, 25% roll | ~2% of seeds bloom (death-dependent) |
| Tools/Child | Rare fetch | Child grows up in 30-60 days, 10% roll | ~1% of seeds bloom |

These rates are intentionally low. The ethos says: *"Rare > Loud."* A player who plays for 200 hours might see 2-3 of these chains complete. That's enough. The ones that complete will be remembered.

---

## Interaction with Existing Systems

- **Memory decay**: Seeds use the existing decay curve. A `BREAD_DELIVERED_TO_HUNGRY` memory at low impact decays over 30 days. If the player doesn't return within the window, the seed dies naturally. No cleanup code needed.
- **Memory resurfacing**: If a villager references a seed memory in greeting dialogue (e.g., `FED_THE_HUNGRY` after Chain 1 completes), the memory is resurfaced and the decay clock resets. The chain's aftermath can echo for months.
- **Single active quest**: Chains respect the single-quest constraint. Each beat is a separate quest accepted at a separate time. The player never holds two beats simultaneously.
- **Quest rarity manager**: Beat 2 quest generation hooks into `QuestRarityManager` to prevent spam. A village can only have one active chain-seed check per quest generation cycle.
- **Mail system**: After any chain completes, there's a 30% chance of a follow-up letter arriving days later via the mail system. The letter references the chain obliquely. For Chain 4 (sapling/grief), Wren might write: *"The tree has leaves now. I sat under it yesterday. First time."* This is not a new system — it uses the existing `MailSystemIntegration` hooks.

---

*"The moment it sounds like a video game, start over."*
