# Quick Dialogue Reference Examples

> **Quick reference only.** For the full writing guide (reputation bands, technical implementation, writing process), see [DIALOGUE_WRITING_GUIDE.md](DIALOGUE_WRITING_GUIDE.md).

For when you need to write new dialogue quickly while maintaining the vision.

## The Golden Rule
**Every line should feel like it comes from someone with their own concerns.**

**Note:** These examples represent the target voice and tone. Some match lines already in `DialogueContent.java`; others are templates for new dialogue. All dialogue is hardcoded in Java source — see the writing guide for technical details.

## Quick Templates by Context

### Morning Greetings
```java
"*yawning* Oh, it's you."
"Rooster was late again."
"Slept wrong. Neck's been killing me."
"You're up before the chickens. That's new."
```

### Weather Comments (Not Small Talk)
```java
// RAIN
"This rain... the well needed it."
"Mud season again."
"*looking at leaking roof* Great."

// STORM
"Animals are restless. Storm's coming."
"Should board up the windows..."
"Lost three fenceposts last time. Three."
```

### Work Requests (Natural, Not Quest-Like)
```java
"*wiping sweat* The harvest is... overwhelming."
"Could use another pair of hands. Paying hands."
"Got strong arms? Got work."
"Help me today, I'll remember tomorrow."
```

### Quest Complete (Human Impact, Not Rewards)
```java
"It's done? Good. The children will eat."
"*exhales* That's one less worry."
"You actually did it. Huh."
"We made bread yesterday. First in weeks."
```

### Refusing Misnomer Quests (Player Responses)
```java
"I... no. No, I'm not doing that."
"What would that even fix?"
"Slow down. Tell me what's actually wrong."
"I can't give you that. But I can listen."
"That's not help. That's just a different kind of hurt."
```

### Idle Observations
```java
"My daughter asked where birds go when they die."
"The well's getting low. Third year now."
"Saw the baker crying yesterday. Didn't ask."
"That sound last night... you heard it too?"
"Bees are late this year. Noticed that?"
"The fisherman's cat hasn't been home in days."
```

### Reputation Shifts (Subtle)
```java
// GAINING TRUST
"You're still here."
"Starting to see why they talk about you."
"*leaves the door open when you approach*"

// LOSING TRUST
"*steps back slightly*"
"We were just starting to trust you."
"I thought you'd be different. Guess not."
```

### Children (Direct, Unfiltered)
```java
"You smell funny!"
"My mom says you're dangerous but you look normal."
"Wanna see a dead frog?"
"Why are adults always tired?"
"I named a chicken after you! Don't tell her."
"Why is everything squares? Even the sun."
```

### Silence and Refusal
```java
"..."
"*looks away*"
"*walks away without responding*"
null // Sometimes no response IS the response
```

### Time Passing References
```java
"Been three days since the last trader."
"Tomorrow marks a year since father..."
"Can't remember when this started feeling normal."
"Same view every morning. Still not tired of it."
```

### Things Left Unsaid
```java
"I used to think about leaving. Still do, some mornings."
"You ever notice how quiet it gets right before dawn?"
"*stares at the empty house across the road*"
"*opens mouth, closes it, goes back to work*"
```

## What NOT to Write

❌ "Greetings, traveler!"
❌ "Welcome, hero!"
❌ "I have a quest for you!"
❌ "Congratulations on completing the task!"
❌ "Your reputation has increased!"
❌ "Thank you for everything you've done!"
❌ "The village is safe thanks to you!"

Also avoid:
❌ "There's something I should... no, forget it." (the trail-off formula — once is human, twelve times is a writer's tic)
❌ "[Deep statement]. [Ellipsis]. Never mind." (every villager trailing off the same way kills the mystery it's reaching for)

### Moonfaced Prose (the biggest trap)

This is when dialogue sounds cryptically poetic instead of human. Villagers are NOT fortune cookies.

```java
// ❌ MOONFACED — sounds like a film trailer
"That matters."
"Knowing is enough."
"The silence between you holds."
"Just... different."
"You're just... in it."
"Something about this hour feels thin."

// ✅ GROUNDED — sounds like a person
"You've been here a while. Longer than some."
"Even the bugs shut up around now."
"Saw the kids playing near your place."
"Good morning. Don't know why I'm in a good mood. Don't ruin it."
```

The difference: moonfaced prose strips specificity until only atmosphere remains. Grounded prose keeps the **specific detail** that makes it real. "The silence holds" has no handhold. "Even the bugs shut up" has bugs. Bugs are real. See the full writing guide for detailed examples.

## Quick Checks Before Adding Dialogue

1. **The Stranger Test**: Would this make sense if said to a stranger on the street?
2. **The Child Test**: Would a child understand this person has feelings?
3. **The Silence Test**: Could silence work better here?
4. **The Center Test**: Is this about the villager or the player?
5. **The Variety Test**: Does this sound like the last three lines you wrote? If yes, find a different shape.

## Formatting Patterns

- `*action in asterisks*` for physical actions
- `...` for trailing off — **use sparingly, earns its weight through rarity**
- Short sentences. Fragments even.
- Interruptions that don't finish
- Subject changes that say more than the confession would have

## Remember

When in doubt:
- Make it shorter
- Make it about them, not the player
- Make it specific to their life, not generically weighted
- Consider saying nothing
- Think about what they're NOT saying — then find a way to show it that isn't always "... never mind"

The moment it sounds like a video game NPC, start over.
The moment every villager sounds the same kind of mysterious, start over.
