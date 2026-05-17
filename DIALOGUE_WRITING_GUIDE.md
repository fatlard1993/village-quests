# Dialogue Writing Guide for Village Quests

> See also: [DIALOGUE_EXAMPLES.md](DIALOGUE_EXAMPLES.md) for a quick-reference cheat sheet.

## Important: How Dialogue Works

All dialogue is currently **hardcoded in Java** inside `DialogueContent.java` (and individual quest classes like `MisnomerQuest.java`, `DeepQuestDialogues.java`, etc.). There is no external data file system. To add or modify dialogue, you must edit Java source and recompile the mod.

Dialogue entries are registered in `DialogueContent.registerAll()` as `Dialogue` objects with reputation range thresholds and response options.

## Core Principle: Villagers Are People, Not Content Dispensers

Every line of dialogue should feel like it comes from someone who exists beyond their function. They have moods, memories, and things they'd rather not talk about.

## Reputation Bands

The mod uses an **8-band reputation system**. Dialogue should be written to match these thresholds, which are defined in `ReputationBand.java`:

| Band | Range | Color | Flavor |
|---|---|---|---|
| **SHUNNED** | below -100 | Dark Red | Villagers flee on sight |
| **HOSTILE** | -100 to -50 | Red | Villagers avoid eye contact |
| **DISTRUSTED** | -50 to -10 | Gold | Cold glances from doorways |
| **NEUTRAL** | -10 to 10 | White | A stranger's face in the crowd |
| **TRUSTED** | 10 to 50 | Green | A nod of recognition in passing |
| **ESTEEMED** | 50 to 100 | Aqua | Warm greetings from across the square |
| **FAMILIAR** | 100 to 200 | Light Purple | Recognized by all |
| **ELDER_FRIEND** | 200+ | Gold | Part of the fabric of this place |

Note: The current `DialogueManager.java` still uses a simplified 5-band mapping for child greetings (Excellent 50+, Good 10-49, Neutral -10 to 9, Poor -49 to -11, Very Poor -50 or less). New dialogue should target the full 8-band system above.

## Dialogue Categories & Examples

### 1. Greetings (First Contact)

#### Child Villagers
```java
// ESTEEMED or higher (50+)
"I named a chicken after you! Don't tell her."
"The golem waved at you. He never waves at anyone."

// TRUSTED (10-49)
"Hi! Wanna see what I found?"
"You're nice! Do you have any stories?"

// NEUTRAL (-10 to 9)
"Hello mister!"
"Are you new here?"

// DISTRUSTED (-49 to -11)
"My mom says I shouldn't talk to you..."
"*hides behind something*"

// HOSTILE or lower (-50 or less)
"*runs away crying*"
"HELP! SCARY PERSON!"
```

**Key Elements:**
- Children are direct and emotional
- They don't filter their thoughts
- Fear is immediate and visible
- Joy is pure and uncomplicated
- Children notice what adults have learned to ignore

#### Adult Villagers
```java
// Context-aware greetings that avoid "hero" language
MORNING: "Rooster was late again."
RAINING: "This weather... good for the crops at least."
NIGHT: "Shouldn't you be in bed? I should too..."
AFTER_TRAGEDY: "Oh... hello. Sorry, I'm not myself today."

// Profession-subtle hints (not "I AM THE BLACKSMITH")
FARMER: "Dirt under my nails again. Always dirt."
LIBRARIAN: "Sorry, lost in thought. Old habit."
BUTCHER: "Busy morning. The whole village is hungry."
TOOLSMITH: "Heat it. Bend it. Hit it. Heat it again."
```

### 2. Work Inquiries (Natural Labor Exchange)

```java
// GOOD: Acknowledges economic reality
"Looking for work? Times are tough for everyone."
"I might have something. Can't pay much though."
"Honest work if you want it. Nothing glamorous."

// BAD: Quest dispenser mode
"Ah, an adventurer! I have three tasks!"
"Hero! Perfect timing! Quest time!"
```

### 3. Misnomer Quests (Ethical Tests)

**Critical:** Villagers asking for wrong things should sound conflicted, not manipulative. These people are in pain, and pain makes people reach for bad solutions.

```java
// VIOLENCE - Pain speaking, not calculation
"*voice shaking* Every night I dream about what they did..."
"I know it's wrong but... they need to understand pain."
"*clenching fists* I can't stop thinking about making them pay."

// SABOTAGE - Jealousy eating through restraint
"*wringing hands* If their workstation just... broke..."
"They get all the trade. Every day I watch them."
"One little accident. That's all it would take."

// THEFT - Survival instinct fighting morality
"*looking down* My children need food. They have so much..."
"We're hungry and... I know it's wrong but..."
"They overcharge us anyway. Taking back what's ours..."

// PANIC - Fear overriding reason
"*breathing heavily* No time to explain! Just do it!"
"Something terrible is coming! We have to act first!"
"*trembling* I saw something... we can't wait!"

// WEAPON REQUEST - The reason is louder than the words
"Could you... get me something sharp? Don't ask why."
"I need iron. Sharp iron. For... protection."
"*avoiding eye contact* Just something with an edge. Please."
```

**Response Options (Always refuse-weighted):**

Give the player language to refuse without sounding like a philosopher. These are hard moments — the responses should feel hard, not wise.

```java
"I... no. I'm not doing that."
"What would that actually fix?"
"Slow down. Tell me what's really going on."
"I can't give you that. But I'm here."
"That's not help. That's just passing the pain along."
"No. But what do you actually need?"
```

### 4. Idle Chat (Life Texture)

The danger zone for cryptic-sounding dialogue. Idle chat is where writers reach for profundity because there's no quest structure to lean on. Resist. Ground it in the specific life of a specific person.

```java
// WEATHER OBSERVATIONS (their life, not atmosphere)
"Storm coming. I can feel it in my knees."
"This drought... the well's getting low. Third year."
"First snow soon. Need to patch the roof before it hits."

// VILLAGE GOSSIP (fragments, not exposition)
"Saw the blacksmith heading to the library. Third time this week."
"Something's different about the baker lately."
"The fisherman's cat hasn't been home in days."

// PERSONAL MOMENTS (specific, not weighted)
"My daughter asked where birds go when they die."
"Saw the baker crying yesterday. Didn't ask."
"My son tried to build a boat. In the middle of the field. No water for miles."

// THINGS LEFT UNSAID (vary the shape — not every unsaid thing is a trail-off)
"I used to think about leaving. Still do, some mornings."
"*stares at the empty house across the road*"
"*opens mouth, closes it, goes back to work*"
"You ever notice how quiet it gets right before dawn?"
```

**The trail-off trap:** `"I should tell you... never mind"` is one valid shape for something unsaid. But if every villager uses it, you've replaced mystery with a formula. Other shapes: changing the subject, going quiet, getting busy with their hands, staring at something specific, laughing at nothing. The unsaid thing is felt through what the person does *instead* of saying it.

### 5. Quest Completion Dialogue

```java
// IMMEDIATE (focus on impact, not reward)
"You did it. Thank you."
"This helps more than you know."
"I... I didn't think you'd actually come through."

// DELAYED (mail, 1-3 days later, reflection)
"That wheat you harvested? We made bread yesterday. First in weeks."
"Been thinking about what you did. The children are still talking about it."
"I was wrong to doubt you. Actions speak."
```

### 6. Reputation-Influenced Variations

Write dialogue with the 8-band system in mind. The shift from NEUTRAL to TRUSTED is subtle. The shift from ESTEEMED to FAMILIAR is intimate. ELDER_FRIEND is rare and should feel earned, not congratulatory.

```java
// FAMILIAR / ELDER_FRIEND (specificity, shared history)
"Your garden's looking healthy. That new fertilizer working?"
"Heard you helped Martha yesterday. She needed that."
"My son wants to be like you. I'm not sure how I feel about that."

// TRUSTED / ESTEEMED (warmth, casual comfort)
"Good to see you around."
"Stay for dinner? There's enough."
"Quiet morning. I almost didn't notice you. That's a compliment."

// DISTRUSTED / HOSTILE (distance and defensiveness)
"What do you want now?"
"We're fine. Don't need anything from you."
"Just... keep walking."

// REPUTATION RECOVERY (acknowledgment without forgetting)
"I see you're trying. That's... something."
"My neighbor says you've changed. Maybe."
"One good deed doesn't erase the past. But it's a start."
```

### 7. Time-Sensitive Dialogue

```java
// MORNING (routine, not poetry)
"Slept wrong. Neck's been killing me."
"Rooster was late. Again."
"The goats got out. Whole morning gone."

// AFTERNOON (fatigue, routine)
"Half the day gone already."
"Afternoon sun makes me drowsy."
"Same field. Same weeds. Same back."

// EVENING (winding down)
"Almost time to head home."
"Getting dark earlier these days."
"*stretching* Done enough for today."

// NIGHT (guard down, honesty closer to the surface)
"Shouldn't be out this late."
"The village looks different at night."
"Sometimes I can't sleep either."
```

### 8. Environmental Reactions

```java
// PLAYER ACTIONS OBSERVED
Breaking blocks: "That's village property..."
Opening chests: "Those aren't yours to open."
Near fire: "Careful with that flame."
Building: "Planning to stay a while?"

// INDIRECT ACKNOWLEDGMENT
"Noticed you've been busy near the square."
"That new structure... interesting choice."
"The hole you left is still there."
```

### 9. Ontological Observations

These are the rarest and most powerful lines. They work because the villager isn't trying to be deep — they're noticing something genuinely strange about their world. Keep them grounded in observation, not in performed unease.

```java
// GOOD: Specific to the world's actual strangeness
"Bread never spoils. Milk never sours. Nothing rots. Ever wonder why?"
"Every cow looks identical. Even their spots. Have you noticed?"
"Why do we all wake up at exactly the same time? Every morning. Exactly."

// GOOD: Children notice what adults ignore
"Why is everything squares? Even the sun."
"How come you can carry 64 logs but only 16 eggs?"

// AVOID: Generic existential weight
// "Do you ever wonder if any of this is real?" — too on-the-nose
// "Something feels wrong but I can't explain it." — open circuit, no specifics
```

## Dialogue Don'ts

### Never Use These Phrases:
- "Greetings, traveler/hero/adventurer"
- "I have a quest for you"
- "Well met!"
- "Congratulations!"
- "You've gained reputation!"
- "Mission complete!"
- "Thank you, brave hero!"

### Avoid These Patterns:
- Over-explaining backstory
- Perfect grammar (people speak in fragments)
- Always being available to talk
- Immediate gratitude
- Clear moral lessons
- Game terminology
- **The trail-off formula on repeat** — "I should tell you... never mind" is valid once. Used as a pattern, it becomes a writer's tic that replaces genuine mystery with performed mystery
- **Fortune-cookie refusals** — "Violence never brings understanding" sounds wise but not human. Refusals should stumble.
- **Moonfaced prose** — See below. This is the single most common failure mode in this mod's dialogue.

### The Moonfaced Test (CRITICAL)

"Moonfaced" dialogue is when a villager sounds like a fortune cookie, a film trailer, or an oracle instead of a person. It's cryptically poetic, weirdly terse, or unnaturally compressed in a way that sounds profound but says nothing a real person would say out loud.

**The test: "Would a real farmer/butcher/mason say this to someone standing in front of them?"** If the answer is "only in a movie" — rewrite it.

#### What moonfaced sounds like:
```java
// FORTUNE COOKIE — wisdom that sounds carved in stone
"That matters."
"Knowing is enough."
"That's not nothing."
"The silence between you holds."

// FILM TRAILER — dramatic fragments that sound like voiceover
"Just... different."
"Something brought you here."
"You're just... in it."

// ORACLE — the villager knows things they shouldn't
"Something about this hour feels thin."
"The world's half-asleep."
"Make a wish."

// POETICIZED MUNDANE — ordinary observations made mysteriously heavy
"The veil is thinnest at night."
"Time bends here."
"Everything else here matters too much."

// SELF-AWARE DISCLAIMER — praising the player while pretending not to
"Not because we need you. Just because you're part of it."
"That's not a compliment. It's just true."
```

#### What grounded sounds like:
```java
// SAME IDEAS, said by actual people:
"You've been here a while. Longer than some."
"Funny, I almost introduced you to someone as a local the other day."
"Dead quiet out there. Even the bugs shut up around now."
"Everything sounds louder at this hour. You ever notice that?"
"Saw the kids playing near your place. They don't do that by strangers' houses."
"Good morning. I don't know why I'm in a good mood. Don't ruin it."
```

#### The difference:
Moonfaced prose **strips specificity until only atmosphere remains.** Grounded prose **keeps the specific detail that makes it real.** "The silence holds" has no handhold. "Even the bugs shut up" has bugs. Bugs are real.

#### Where this pattern hides:
- **Acknowledgment/witness beats** after emotional moments — the temptation to caption the silence
- **Time-awareness greetings** — the temptation to be profound about duration
- **High-reputation greetings** — the temptation to have the villager deliver a thesis about belonging
- **Overheard fragments** — the temptation to script vague atmosphere instead of specific gossip
- **Micro-intimacy greetings** — the temptation to have villagers perform vulnerability instead of showing it

#### The fix is always the same: add a specific detail.
- "I think I'm happy" → "I don't know why I'm in a good mood"
- "That matters" → (cut — the action speaks for itself)
- "Just... different" → "...been in a better mood since they started helping"
- "The world's half-asleep" → "Dead quiet out there"

## Technical Implementation Tips

Note: The code examples below are conceptual illustrations, not literal API calls.

### Dialogue Variation System
```java
private String getContextualDialogue(VillagerContext context) {
    List<String> options = new ArrayList<>();

    // Base personality
    options.addAll(getPersonalityDialogue(context.personality));

    // Layer in context
    if (context.recentTragedy) {
        options.replaceAll(s -> makeSubdued(s));
    }

    if (context.isRaining) {
        options.add(getRainThought());
    }

    if (context.playerHoldingFood && context.villagerHungry) {
        options.add("Is that... food? Sorry, I shouldn't stare.");
    }

    // Pick with weighted random
    return selectWeighted(options, context);
}
```

### Memory Fragments
```java
// 5% chance to reference past event
if (random.nextFloat() < 0.05 && hasMemoryOf(event)) {
    return getMemoryDialogue(event);
}

// But memory fades
if (daysSince(event) > 30) {
    return null; // Forgotten
}
```

### Silence as Dialogue
```java
// Sometimes, no response IS the response
if (villagerState.isGrieving()) {
    return "..."; // Just ellipsis
}

if (reputation < -30 && random.nextBoolean()) {
    return null; // Won't even acknowledge player
}
```

## Writing Process

1. **First Draft:** Write what a real person would say in that situation
2. **Body Pass:** Give them a body — are they sweating, squinting, fidgeting, leaning?
3. **Context Pass:** Adjust for time, weather, recent events, who's nearby
4. **Specificity Pass:** Replace anything generic with something from *this* person's life
5. **Variety Pass:** Does this sound like the last three lines you wrote? Find a different shape.

## Examples of Evolution

### Version 1 (Bad):
"Hello adventurer! I need 10 wheat! Will you accept this quest?"

### Version 2 (Better):
"The harvest is overwhelming. Could you help?"

### Version 3 (Good):
"*wiping sweat from forehead* The wheat won't harvest itself. You busy?"

### Version 4 (Best):
"*looking at the fields* It's too much for one person. *pause* ...you any good with a scythe?"

### What changed at each step:
- V1→V2: Removed game language
- V2→V3: Added body, added directness
- V3→V4: Added vulnerability (admitting it's too much), specific question (scythe, not generic "help")

## The Voice Test

Read your line out loud. Then read these:

- "Everything dies. I just make it useful."
- "Saw the baker crying yesterday. Didn't ask."
- "I know what they call me. Behind their workstations."
- "Heat it. Bend it. Hit it. Heat it again."
- "I named a chicken after you! Don't tell her."

Does your line belong in that company? If it sounds more *literary* than those, it's drifting. These lines are plain. They land because they're specific and embodied, not because they're poetic.

## Remember

Every line of dialogue is a chance to show that this villager:
- Existed before the player arrived
- Will exist after the player leaves
- Has concerns beyond the player's needs
- Sometimes doesn't want to talk
- Carries the weight of their own story

The moment dialogue feels like it's "for" the player, we've failed.

The moment every villager sounds the same kind of mysterious, we've failed.

The moment conversation feels like a transaction, we've failed.

**Make them human. Make them specific. Make them real.**
