# Vision Enforcement Guide for Village Quests

This document ensures the ethos from [ETHOS.md](ETHOS.md) carries through every line of code and dialogue. Read ETHOS.md first for the philosophy; this document covers code-level patterns and enforcement.

> **Note:** The code examples below are **conceptual pseudocode**, not literal API calls. They illustrate the intended patterns and anti-patterns. For actual class and method names, refer to the source code in `src/`.

## Core Truth: Villages Are Communities, Not Content Hubs

### ✅ GOOD Implementation Patterns

```java
// Villagers act independently
if (world.getTime() % GATHERING_CHECK_INTERVAL == 0) {
    checkForGathering(); // Happens whether player is near or not
}

// Aftermath matters more than action
sendDelayedMail(player, "The seeds you planted are starting to sprout. The children have been watching them every day.");

// Restraint over efficiency
if (questRequestCount > DAILY_LIMIT) {
    return "I already gave you work today. Check back tomorrow.";
}
```

### ❌ BAD Implementation Patterns

```java
// Player-centric timing
if (playerIsNearby()) {
    startVillageEvent(); // WRONG: Villages only animate for player
}

// Transactional reputation
reputation += QUEST_COMPLETE_POINTS; // WRONG: Too mechanical

// Over-explaining
dialogue.add("You gained 5 reputation!"); // WRONG: Numbers break immersion
```

## Dialogue Principles

### Tone Variations by Context

#### Time-Aware Dialogue
```java
// GOOD: Natural time awareness
if (isMorning) {
    options.add("Fresh harvest this morning!");
} else if (isNight) {
    options.add("Dark times require holy items...");
}

// BAD: Generic greetings
options.add("Hello adventurer!"); // Too game-y
```

#### Emotional Continuity
```java
// GOOD: Memory affects tone
if (recentTragedyInVillage()) {
    dialogue = dialogue.replace("!", "."); // Subdued tone
    dialogue = "Sorry, I'm... not myself today.";
}

// BAD: Always chipper
dialogue = "Welcome hero! Quest time!"; // No emotional range
```

### Misnomer Quest Dialogue

The villager should sound **conflicted**, not clever:

```java
// GOOD: Shame and hesitation
"*looks around nervously* I... I shouldn't even be saying this..."
"*voice breaking* They ruined everything. I just want them to understand..."
"*breathing heavily* There's no time to explain!"

// BAD: Conspiratorial or manipulative
"Psst, I have a secret job for you..." // Too scheming
"If you do this, no one will know..." // Too calculated
"Trust me, this is for the greater good." // Too justified
```

## Quest Philosophy Enforcement

### Single Active Quest Pattern
```java
public boolean canAcceptQuest(ServerPlayerEntity player) {
    // FORCE intentionality
    if (ActiveQuestManager.hasActiveQuest(player)) {
        sendMessage("You already have your hands full. Finish that first.");
        return false;
    }
    return true;
}
```

### Failure as "Too Late"
```java
// GOOD: Quiet expiration
if (currentTime > questExpiry) {
    // No notification, no punishment
    quest.silentlyExpire();
    // Maybe acknowledged later
    schedulePossibleMail("I managed to handle that thing myself. Thanks anyway.");
}

// BAD: Loud failure
player.sendMessage("QUEST FAILED! -10 REPUTATION!"); // Too game-y
```

### Ethical Refusal Recognition
```java
// Delayed recognition for refusing misnomer quests
private void handleDelayedRecognition(player, villager) {
    long daysSinceRefusal = (currentTime - refusalTime) / 24000;

    if (daysSinceRefusal >= 1 && daysSinceRefusal <= 3) {
        // Variable delay: 1-3 days
        sendMail(player,
            "I've been thinking about what you said. " +
            "You were right to refuse. Thank you for being the voice of reason."
        );

        // Small, percentage-based reputation gain
        int currentRep = getReputation(player);
        int gain = Math.max(1, currentRep / 20); // 5% of current
        modifyReputation(player, gain);
    }
}
```

## Reputation System Integrity

### Percentage-Based Scaling
```java
public int calculateReputationChange(int currentRep, ReputationEvent event) {
    // High reputation = fragile trust
    if (currentRep > 50) {
        return event.baseChange * currentRep / 100; // Scaled by current
    }
    return event.baseChange;
}
```

### Never Show Numbers
```java
// GOOD: Descriptive feedback
player.sendMessage("The villagers seem to trust you more.");
player.sendMessage("You sense a shift in how they look at you.");

// BAD: Numerical feedback
player.sendMessage("Reputation: 45/100"); // Breaks immersion
player.sendMessage("+5 reputation!"); // Too mechanical
```

## Mail System as Reflection

### Delayed Emotional Processing
```java
// Mail arrives 1-3 days after events
public void scheduleAftermath(QuestCompleteEvent event) {
    long delay = 24000 + random.nextInt(48000); // 1-3 MC days

    scheduleMail(event.player, delay, () -> {
        return generateAftermathMessage(event);
    });
}

private String generateAftermathMessage(event) {
    // Reflection, not notification
    return switch(event.type) {
        case FARMING -> "The wheat you helped harvest fed the whole village yesterday. The children were especially grateful.";
        case RESCUE -> "I still have nightmares about being lost. Thank you for finding me.";
        case REFUSED_VIOLENCE -> "I was so angry that day. Your refusal... it saved me from myself.";
    };
}
```

### Non-Transactional Mail
```java
// GOOD: Human moments
"Saw a butterfly today. Reminded me of that story you told."
"My daughter asked about you yesterday."
"The sunrise looked different this morning. Thought you should know."

// BAD: Game notifications
"New quest available!"
"You've unlocked a new trader!"
"Daily bonus ready!"
```

## Gathering System

### Life Without Player
```java
public void processGathering() {
    // No rewards, no tracking, no purpose beyond atmosphere
    moveVillagersToGatheringPoint();

    // Player might glimpse it
    if (playerNearby && random.nextFloat() < 0.1) {
        showSubtleIndication(); // Particles, not text
    }

    // No explanation
    // No "Join the gathering!" prompts
    // Just life happening
}
```

## Lore Integration

### Ontological Friction
```java
// Villagers sense something wrong but can't articulate it
private String getLoreDialogue() {
    return switch(random.nextInt(5)) {
        case 0 -> "Sometimes I forget what I was about to say. Like the words just... leave.";
        case 1 -> "Do you ever feel like you're repeating the same day?";
        case 2 -> "I had the strangest dream. We were all... squares?";
        case 3 -> "The sun sets so quickly. Too quickly.";
        case 4 -> "Why do I know your name before you tell me?";
    };
}
```

### Never Explain Fully
```java
// GOOD: Suggestive fragments
"The old texts mention this before..."
"My grandmother whispered about such things..."
"Perhaps it's nothing, but..."

// BAD: Lore dumps
"Let me tell you the complete history of our village..."
"The ancient prophecy states that..."
```

## Code Review Checklist

See the five Implementation Heuristics in [ETHOS.md, Section 8](ETHOS.md#8-implementation-heuristics). Apply them to every dialogue line, quest definition, and UI element before committing.

## Common Pitfalls to Avoid

### The Helper Syndrome
```java
// BAD: Villager exists to dispense content
"Hello! I have 3 quests for you today!"

// GOOD: Villager has their own concerns
"Oh, it's you. I'm rather busy with the harvest..."
```

### The Number Creep
```java
// BAD: Gradually adding more UI elements
showReputationBar();
showQuestCounter();
showVillageHappiness();

// GOOD: Less information, more inference
// Let players figure things out through observation
```

### The Efficiency Trap
```java
// BAD: Optimizing village interactions
"Fast travel to quest giver"
"Auto-complete similar quests"
"Reputation multiplier events"

// GOOD: Intentional friction
"Walk through the village"
"One quest at a time"
"Small, meaningful changes"
```

## Testing for Vision Alignment

See the complete Test Suite in [ETHOS.md, Section 10](ETHOS.md#10-the-test-suite) for the definitive list (Stranger, Child, Silence, Absence, 100-Hour, Optimization, and Explanation tests). Apply these tests to every new feature before merging.

## Final Reminder

**This mod is about what almost happens, what happens off-screen, and what people think about afterward.**

Every feature should support this. Every line of dialogue should breathe it. Every system should enforce it.

The moment it becomes loud, frequent, optimal, explained, or player-centered, the vision is lost.