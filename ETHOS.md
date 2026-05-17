# The Ethos of Village Quests
*A Socially Coherent Village Simulation for Vanilla Minecraft*

## The Sacred Boundary

**This document is the soul of the project. Code can be rewritten, features can be added or removed, but if this ethos is violated, the project has failed.**

---

## 1. Core Truth

**Villages are communities, not content hubs.**

**The player is a participant, not a manager, hero, or center of gravity.**

This mod exists to restore a sense of social continuity, lived-in space, and moral texture to Minecraft villages—without replacing vanilla systems or creating a parallel simulation.

The goal is not to make villagers smarter, more efficient, or more rewarding, but to make them feel like people who exist whether or not the player is present.

---

## 2. The Four Pillars

### 2.1 Non-Player-Centric World

Villages act independently of player presence.

Events, gatherings, moods, and damage occur whether the player engages or not.

The player is sometimes invited, sometimes ignored, sometimes too late.

**Failure mode:**
If the village only changes because the player acts, the system has failed.

### 2.2 Restraint Over Optimization

Systems should reward judgment, not efficiency.

Sometimes the correct action is refusal, delay, or inaction.

Silence, absence, and missed moments are valid outcomes.

**Failure mode:**
If players can identify a "best" or "optimal" behavior loop, the system has failed.

### 2.3 Emotional Continuity Over Mechanical Progression

Reputation reflects trust, reliability, restraint, and presence—not XP or grind.

Villagers remember how things happened, not just outcomes.

Aftermath matters more than completion.

**Failure mode:**
If reputation feels like a currency or score, the system has failed.

### 2.4 Rare > Loud

Meaningful events must remain rare even in long-lived worlds.

Frequency is a narrative tool.

Quiet periods are intentional.

**Failure mode:**
If players feel overwhelmed, habituated, or "trained" by the system, it has failed.

---

## 3. Design Philosophy

### 3.1 Social Authenticity

Villagers:
- Get tired
- Avoid topics
- Speak differently in crowds vs private
- Change tone based on time, weather, and recent events
- Sometimes say nothing at all

They are not lore terminals.

### 3.2 Ambiguity Is a Feature

Lore is suggestive, fragmentary, and contradictory.

Ethical situations do not always resolve cleanly.

Letters, dialogue, and memories often express doubt.

The system should never explain the world fully.

### 3.3 Memory Is Contextual, Not Absolute

Villagers have limited, probabilistic memory.

Past events influence tone occasionally, not constantly.

Memories fade, resurface, and are sometimes suppressed.

**Failure mode:**
If villagers constantly reference the same trauma or event, the world feels written.

### 3.4 Belonging Is Earned Through Presence

Belonging is signaled through:
- Being invited (mail)
- Being trusted privately
- Being remembered after absence
- Being given responsibility without reward

Not through:
- Power
- Authority
- Ownership alone

---

## 4. Quest Philosophy

**Quests are reasons people talk to you — not tasks to be completed.**

### 4.1 Quest Types Reflect Human Needs

- Fetch/Gather → survival
- Dialogue (message, apology, mediation) → trust and connection
- Creation/Repair (build home, light town, fix door, replace beds) → continuity and contribution
- Mystery (missing item, missing animal, vandalism, strange sounds) → curiosity and care
- Misnomers (violence, sabotage, theft, panic, weapon request) → restraint
- Deep quests (loss, trauma, existential doubt, reconciliation) → reflection
- Time-sensitive (fish hat, deliver hay) → presence and natural urgency
- Plot purchase → belonging
- Village development → stewardship

Quests exist because life is messy, not because content is needed.

### 4.2 Single Active Quest

- Prevents hoarding
- Forces intentionality
- Makes abandonment socially meaningful

### 4.3 Failure Is Often Just "Too Late"

- Time-sensitive quests expire quietly
- Missed opportunities change tone, not stats
- No timers, no alerts, no punishment loops

### 4.4 Misnomer Quests: The Ethical Core

Some requests should not be fulfilled.

The villager asks out of fear, anger, or desperation—not manipulation.

Refusal is explicit, respected, and sometimes later acknowledged.

Recognition is delayed, uncertain, and subtle.

**This is not a trick. This is the deepest teaching of the mod: Sometimes the correct action is to refuse action.**

---

## 5. Systems That Don't Exist

### 5.1 Gatherings as Absence

Gatherings are not visible events the player is excluded from.

They are **invisible** events the player is rarely included in.

The power is in thinking "this must happen all the time and I just never knew."

No particles, no sounds, no villager pathing—unless invited.

The invitation itself is the entire feature.

### 5.2 Mail as Reflection, Not Notification

Mail exists to:
- Deliver delayed emotional aftermath
- Communicate absence and thoughtfulness
- Reinforce off-screen life

Mail must:
- Be rare
- Be non-transactional
- Never include reputation numbers
- Never demand response

### 5.3 Reputation as Trust Under Uncertainty

- Scales with current trust (percentage-based)
- High trust is fragile
- Redemption is always possible
- Small actions accumulate; large actions echo

**Important Non-Goals:**
- No reputation farming
- No reputation immunity
- No permanent status (ELDER_FRIEND decays at 1% above 100 per MC day — high trust requires sustained presence)

---

## 6. The Language of Restraint

### What This Mod Is Explicitly NOT

❌ A village management simulator

❌ A progression system

❌ A content firehose

❌ A lore encyclopedia

❌ A replacement for vanilla mechanics

### Words We Never Use

- "Hero"
- "Quest giver"
- "Congratulations"
- "Mission"
- "Reward"
- "Level up"
- "Achievement"

### Words We Use Carefully

- "Help" (implies need, not heroism)
- "Work" (implies labor, not adventure)
- "Trust" (implies relationship, not points)
- "Remember" (implies time passing, not tracking)

---

## 7. Lore & Ontological Friction

### The Crack in Reality

Villagers must never know they are in a game.

They may only sense that something is off.

Examples:
- "Sometimes I forget what I was about to say. Like the words just... leave."
- "Do you ever feel like you're repeating the same day?"
- "Bread never spoils. Milk never sours. Nothing rots. Ever wonder why?"
- "Why do I know your name before you tell me?"

### Three Purposes of Lore

1. Explain survival practices
2. Hint at unseen history
3. Introduce uncertainty about reality

The rarest lore touches:
- Repetition
- Observation
- Incompleteness

---

## 8. Implementation Heuristics

When reviewing any feature, dialogue, or system, ask:

1. **Would this still feel right if the player never noticed it?**
2. **Does this increase certainty or preserve ambiguity?**
3. **Does this reward action over judgment?**
4. **Does this make villagers more available, or more human?**
5. **Does this scale emotionally over 100+ hours?**

If the answer to #3 or #4 is wrong, reject the feature.

For code-level enforcement of these heuristics, including pseudocode patterns and common pitfalls, see [VISION_ENFORCEMENT.md](VISION_ENFORCEMENT.md).

---

## 9. Examples of Rejected Features

### "Village Happiness Meter"
**Why rejected:** Makes village state too transparent, enables optimization

### "Quest Board"
**Why rejected:** Centralizes content, makes quests feel like jobs not relationships

### "Fast Travel Between Villages"
**Why rejected:** Removes journey, presence, and time passing

### "Villager Skill Trees"
**Why rejected:** Gamifies relationships into progression systems

### "Raid Defense Mechanics"
**Why rejected:** Makes player the hero/protector rather than participant

### "Marriage System"
**Why rejected:** Reduces complex relationships to mechanical transactions

### "Village Expansion Planning"
**Why rejected:** Makes player a manager/architect rather than resident

### "Automated Resource Collection"
**Why rejected:** Removes the intentional friction of physical presence

---

## 10. The Test Suite

These tests are the definitive checklist. For code-level testing guidance, see the "Testing for Vision Alignment" section in [VISION_ENFORCEMENT.md](VISION_ENFORCEMENT.md).

### The Stranger Test
Would this interaction make sense with a stranger in real life?

### The Child Test
Would a child understand these are people with feelings?

### The Silence Test
Remove all UI. Does the village still feel alive?

### The Absence Test
Don't interact for 10 days. Does it feel like life continued?

### The 100-Hour Test
After 100 hours, are players still discovering subtleties?

### The Optimization Test
Can players identify an optimal strategy? If yes, we've failed.

### The Explanation Test
Can the system be fully explained? If yes, ambiguity has died.

---

## 11. Sacred Violations

**These are the unforgivable sins:**

1. Making villagers exist for the player
2. Explaining the mystery
3. Rewarding optimization over presence
4. Making silence feel empty rather than full
5. Centering the player in village life
6. Creating false urgency
7. Quantifying relationships

---

## 12. The Final Statement

**This mod is about what almost happens — and what people think about afterward.**

If reality ever drifts toward:
- loud
- frequent
- optimal
- explained
- player-centered

...then intent has been lost.

---

## 13. A Living Document

This ethos is not law but compass.

When lost in implementation details, return here.

When tempted by feature creep, return here.

When players demand "more content," return here.

The village doesn't need you to save it.

The village doesn't need you at all.

And that's what makes your presence there meaningful.

---

*"The moment it sounds like a video game, start over."*

*"The moment every villager sounds like a fortune cookie, start over."*

*"Moonfaced prose — cryptically poetic, weirdly terse, stripped of specificity until only atmosphere remains — is the subtler failure mode. It doesn't sound like a video game. It sounds like a film trailer. Both are wrong. Villagers are specific people with observations, not oracles with atmospherics."*