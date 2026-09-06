# Exchange Exemplars

Calibration drafts for the third-turn sweep. Four of the current corpus's worst
structural offenders, rebuilt as complete exchanges. Nothing here is registered
yet; this document exists so the voice and the shape can be judged before ~750
options get the same treatment.

## The rules these demonstrate

The existing guides govern the **line**. These govern the **exchange**, which is
where the corpus is currently broken:

1. **The third turn.** The villager closes the conversation, not the screen.
   Every option lands somewhere, even if somewhere is one line and a walk-away.
2. **Options are moves, not verdicts.** An option is something the player says
   *to get something*: ask, offer, press, stay, leave. The test for every
   option: *what does the villager say back?* If that question has no answer,
   you wrote a rating, not a line.
3. **A question on a button has a node behind it.** If the villager won't
   answer, the refusal is played on screen. Withholding is something the
   villager *does*, not the absence of a node. Mystery lives in what they say,
   never in whether they respond.
4. **The walk-away button.** Every terminal node offers exactly one button.
   It closes the screen and never triggers another villager line. It exists so
   the player signals they've read the close, not so they can keep talking:
   the villager speaks last, the player *leaves* last. The label reads as
   closure, and closure is usually insinuated, the way real leave-taking is:
   "Goodnight.", "Anyway.", "It's late." Sometimes it's pulled from the scene
   ("Leave it buried."); an explicit departure ("Leave.") is one shape among
   several, saved for when the scene earns its bluntness. What the label never
   does is invite a response. No single shape becomes the formula: the pool
   rule applies here like everywhere else.
5. **Rep lands where the scene lands - and only on trust events.** Reputation
   attaches to the node the conversation reaches, not the sentence the player
   picked, and most nodes carry none: a secret kept, a boundary respected,
   accountability owned, presence through grief. Curiosity and warmth pay
   nothing; the good answer is the reward. Negative costs always stay.
   (Implemented: `.arrival(n)` on `Dialogue`; options carry 0; a startup
   validator enforces it.)
6. **Stage directions are bounded by the model.** A villager on screen can
   shake its head, nod, turn away, walk off, work its station. It cannot wipe
   a block, thread an awl, lean in, or half-smile. If the model can't perform
   it, the direction is narration pretending to be a scene: cut it and let the
   words or an ellipsis carry the beat. Budget: most lines get no direction at
   all; one per exchange is plenty.

   **The ellipsis inherits that same budget.** It is doing the work the cut
   stage directions used to fake, so it is spent, not sprinkled: most lines
   none, at most one on any path a player actually hears in a single
   conversation. The current corpus runs ~1 line in 20 overall and ~1 in 7 in
   the lore pools, which is why every villager sounds like they're... pausing
   for the camera. The sweep trims existing ellipses under the same rule it
   trims asterisks.
7. **Vary the option axes.** The {warm +2 / neutral +1 / cold -1} triplet is
   the pool rule failing one level up. If the three options are the same
   question asked at three temperatures, rewrite along different axes:
   ask about the thing / ask about the person / stay silent / leave.
8. **The floor is a turn, not a scene.** Small talk gets small closes. One line
   and a walk-away, sometimes shared between options, is a complete exchange.
   The sin was never the short conversation; it was the conversation that
   stops mid-exchange.

Annotations: `⟳ +2` marks arrival reputation on a node. `▸` marks player
buttons; the last button in every branch is the walk-away.

---

## 1. The whispering armor (was: `leatherworker_night_secret`)

**The offense:** the villager confesses something impossible, the best-paid
option is "What does it say?" (+2), and clicking it hangs up the call. The
system rewards curiosity with a dead line.

**Rebuilt:**

> **leatherworker_night_secret** (NIGHT_SPECIAL, rep 30+)
> "Found scales once. Not fish. Not lizard. Made one piece of armor. It
> whispers sometimes."
>
> ▸ What does it say? → `leather_scales_what`
> ▸ Can I see it? → `leather_scales_see`
> ▸ Why tell me? → `leather_scales_why`

> **leather_scales_what** ⟳ +2
> "Names. Never ones I know. ...Last month it said one I did."
>
> ▸ Whose? → `leather_scales_whose`
> ▸ That's enough for one night. *(walk-away)*

> **leather_scales_whose**
> "*shakes head* No. That one stays mine. Ask me anything else and I'll
> probably tell you. Not that."
>
> ▸ Leave it be. *(walk-away)*

> **leather_scales_see** ⟳ +1
> "Buried it under the floor two winters back. Some nights I think about
> digging it up. That's why it stays buried."
>
> ▸ Leave it buried. *(walk-away)*

> **leather_scales_why**
> "Because you leave. Come spring you'll be three villages away, and it goes
> with you. Cheaper than the cleric."
>
> ▸ Goodnight. *(walk-away)*

**What changed:** the hook pays off without explaining anything. "What does it
say?" gets an answer that deepens the mystery instead of ending the scene. The
hard refusal ("Not that.") is the most intimate line in the tree, and it is a
*played turn*, not a dropped call. The one stage direction left is the head
shake, which the villager model actually performs. "That sounds dangerous"
(a verdict) died; "Why tell me?" (a move) replaced it, and its answer quietly
carries the whole mod's thesis in the leatherworker's own register.

---

## 2. The bread question (was: `adult_reality_bread`)

**The offense:** the villager asks the player a direct question. The player
answers it. The asker vanishes. A question the asker never follows up is the
purest form of the not-a-conversation feeling.

**Rebuilt:**

> **adult_reality_bread** (IDLE_CHAT, rep 30+)
> "Bread never spoils. Milk never sours. Nothing rots. Ever wonder why?"
>
> ▸ Honestly? Never thought about it. → `reality_bread_never`
> ▸ Convenient, though. → `reality_bread_convenient`
> ▸ Have you asked the others? → `reality_bread_others`
> ▸ Better not to pick at it. → `reality_bread_drop`

> **reality_bread_never** ⟳ +1
> "Nobody has. That's the part that gets me. I stopped bringing it up at
> dinner. My wife looks at me like I asked why water's wet."
>
> ▸ Huh. Anyway. *(walk-away)*

> **reality_bread_convenient**
> "Convenient. Sure. That's one way to sleep at night."
>
> ▸ Suppose it is. *(walk-away)*

> **reality_bread_others** ⟳ +2
> "Tried the mason once. He said the exact words back to me. Not agreed with
> me. Said them. Word for word, like he was waiting his turn. That scared me
> worse than the bread does."
>
> ▸ Right. Well. *(walk-away)*

> **reality_bread_drop** ⟳ -1
> "*turns back to the oven* You're probably right. Forget I said anything."
>
> ▸ Leave. *(walk-away)*

**What changed:** every answer gets acknowledged in the baker's voice before
the scene closes. The shut-down option still exists and still costs, but the
cost lands on the *outcome node*: you watched them turn away, and that's what
the -1 is. The turn-away is the model doing something it can actually do. The
fourth option earns its slot by opening the corpus's best lore seam
(repetition) through a move, not a lore dump.

---

## 3. The named animals (was: `butcher_night_confession`)

**The offense:** the verdict triplet at its worst. "That must be difficult +2",
"Respect for the sacrifice +3". Nobody standing in a dark doorway says
"Respect for the sacrifice." Those are morality-slider labels wearing player
clothes, and none of them get answered anyway.

**Rebuilt:**

> **butcher_night_confession** (NIGHT_SPECIAL, rep 30+)
> "*lowers voice* Sometimes I name them. Makes it harder. But more honest,
> maybe."
>
> ▸ Why make it harder on yourself? → `butcher_names_why`
> ▸ What was the last one called? → `butcher_names_last`
> ▸ Say nothing. → `butcher_names_stay`

> **butcher_names_why** ⟳ +1
> "First year, it got easy. Easy scared me worse than the sound ever did.
> The names keep it from getting easy."
>
> ▸ It's late. *(walk-away)*

> **butcher_names_last** ⟳ +2
> "Gerald. Big brown eyes. Terrible temper. ...Tuesday."
>
> ▸ Goodnight. *(walk-away)*

> **butcher_names_stay** ⟳ +1
> "...Thanks for not making it a thing."
>
> ▸ *nod* *(walk-away)*

**What changed:** three moves on three different axes: ask about the practice,
ask about the animal, stay. "What was the last one called?" is the exchange the
old node was reaching for and couldn't produce: the player's move invites
specificity, and the answer is four words, a name, and a weekday. The ellipsis
before "Tuesday" does the work the smile choreography was doing, without
asking the model to act. The silent option shows presence as a first-class
move with its own close, which is this mod's whole belief system in one button.

---

## 4. The floor (was: `butcher_idle_philosophy`)

**The offense:** none, almost. "Everything dies. I just make it useful." is a
good line and small talk is allowed to stay small. The only failure is that
the player's reply hangs. This exemplar sets the *minimum* bar so the sweep
doesn't gold-plate 400 pieces of small talk.

**Rebuilt:**

> **butcher_idle_philosophy** (IDLE_CHAT, any rep)
> "Everything dies. I just make it useful."
>
> ▸ Fair enough. → close: "Mm."
> ▸ That's blunt. → close: "So's the cleaver."
> ▸ Someone has to. → close: "That's what I tell the pigs. They never agree."
>
> Every close is followed by the shared walk-away: ▸ Back to it.

**What changed:** three one-line closes and one shared walk-away, no new
nodes, no rep anywhere. Total cost: four short strings. This is the floor, and
most of the corpus needs exactly this and nothing more. Depth is for the
confessions and the hooks; the floor is for everything else, and the floor is
still a complete conversation: the villager gets the last word, and the player
gets the last step.

---

## What the sweep does with these, per category

- **Hooks and confessions** (night whispers, ontological, emotional, gossip
  with a question in it): exemplar 1 and 3 treatment. New answer/refusal nodes,
  depth 2, occasionally 3. This is the expensive, human part of the sweep.
- **Questions the villager asks** the player: exemplar 2 treatment. Each
  answer acknowledged before close.
- **Small talk and verdict-triplets on mundane lines**: exemplar 4 treatment.
  One-line closes, options rewritten to moves only where the current ones are
  slider labels.
- **Stage directions:** sweep-wide trim to model-performable actions only
  (head shake, nod, turn away, walk off, work the station). Everything else
  becomes words or an ellipsis.
- **Walk-away labels:** insinuated closure by default ("Goodnight.",
  "Anyway.", "It's late."), contextual pulls where the scene offers one
  ("Leave it buried."), explicit departures rarely and only where earned.
  Varied per register, same pool-rule discipline as every other pool.
- **Rep:** all option-level values go to 0 in the same pass; arrival values go
  onto the nodes the fiction says earned them. The visible-to-player behavior
  contract (never show numbers) is untouched.
