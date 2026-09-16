# Conversational Fidelity Review

Four quests traced screen by screen through the code as it stands after the
one-voice work, the castles rewrite, and this pass. Each tree is what the
player sees: the villager's line, then the buttons in the order the screen
sorts them, then where each button lands. `▸` is a button; the last button on
a screen is the exit. Names are stand-ins.

The question for each tree is the one the guide asks: does the villager
answer what the player said, does the villager speak last, and does the
player leave last. Where the answer was no, the fix is noted and, where it
was small, done.

---

## A. The wall by the gate (a castle errand, TRUSTED)

A mason in a village that generated with a keep. The player has met her,
holds no quest, and has not asked her for work today.

> **Maren (mason)**
> Long day. Bees are late this year. Noticed that?
>
> ▸ Thought I'd see what you have. *(trade)*
> ▸ Anything need doing around here? *(work)*
> ▸ Hadn't, actually. → "City ears. You'll learn to hear the small stuff. The bees usually beat the blossom by a week. Not this year." ▸ I'll listen for them.
> ▸ I'll let you get back to it. *(exit)*

"Long day." is a manner beat: one, before a light greeting, never before
anything heavy. The old build could have opened this same screen with a
recognition line, a mood prefix, a presence prefix and a tone suffix at once.

> **Maren** *(after "Anything need doing around here?", the castle errand won its roll)*
> *thinks for a second* Actually, yeah. The wall's down a course by the gate. Stone bricks. Eight would see it right.
> » Bring: 8x Stone Bricks
>
> ▸ I'll take care of it.
> ▸ What's in it for me? *(one time in four)* → "work_payment" beat
> ▸ Not today. *(exit)*

Before the castles rewrite this screen read: "Storm weakened the north wall.
The eastern wall section is crumbling. Need stone bricks to shore it up. I
could use some stone bricks if you come across any." Three sentences, one
ask, said twice, in a village that may not have had a wall.

> **Maren** *(a later visit, no bricks yet)*
> Oh, you're back. Got the stuff? No? Alright. I'll be here.
>
> ▸ I can't get this done. Sorry. *(let it go)*
> ▸ Some other time. *(exit)*

> **Maren** *(with the bricks)*
> You got it? Let me see.
>
> ▸ Here - the 8 Stone Bricks you asked for. *(pinned, with the icon)*
> ▸ I can't finish this one. Sorry.
> ▸ I should go. *(exit)*

> **Maren** *(the handover)*
> That's done then. This helps.
>
> ▸ *nod* *(exit)*

**What this pass changed here.** The handover used to show a generic thanks
from the reputation-banded pool ("It's done? Good. This helps more than you
know.") while the quest's own line ("That's done then. This helps.") sat in
the villager's queue and opened the *next* conversation, a day later, as
though she had only just noticed. Now the queued line is the close of the
moment that produced it, and the pool is the fallback for quests with nothing
of their own to say.

**Afterwards.** For five minutes, roughly a third of the other villagers open
with "I heard you helped Maren. That was good of you." Answer any one of them
("They asked." → "Plenty get asked. Fewer turn up.") and the rest stop
mentioning it. The old build had every villager say it, unanswered, in front
of whatever else they were about to say.

**Verdict.** Complete on every branch. The villager speaks last and the
player leaves last on all six screens. The one thing that still reads as
software is the pinned label's shape, "Here - the 8 Stone Bricks you asked
for", which is a form filled in rather than a sentence said. It is honest
about what the click does, which the guide ranks above elegance, so it stays.

---

## B. Dirt soup (a theft misnomer, ESTEEMED)

The ethical core. A villager at reputation forty or above asks for something
that should be refused.

> **Tomas (farmer's neighbour)** *(after asking for work)*
> *looking down* My kids ate dirt soup yesterday. I watched them pretend it tasted good. The farmer has a chest full of food. A full chest.
>
> ▸ They won't even notice. *(accept)*
> ▸ No. Find another way. *(refuse)*
> ▸ I don't help thieves. *(refuse coldly)*
> ▸ I need to think about that. → "Don't think too long." ▸ *nod* *(nothing decided; they may ask again)*

> **Tomas** *(after "No. Find another way.")*
> Easy for you to say. You're not starving.
>
> ▸ *say nothing* *(exit)*

**What this pass changed here.** That second screen did not exist. The
player clicked "No." and the screen closed; Tomas's answer was queued and
came out the next time the player spoke to him, glued in front of a greeting
about the weather. The refusal is the whole teaching of the mod, and the mod
was hanging up on it. It now plays as the close, and the player leaves last.

> **Tomas** *(a later visit, the player carrying two bread, the quest still open)*
> *(whatever opening won its roll)*
>
> ▸ What if I just... brought you food instead? (Bread) *(hand it over)*
> ▸ ...

> **Tomas** *(after the bread)*
> *(his own line for it, if the quest queued one; otherwise the screen closes as it did before)*
>
> ▸ Take care. *(exit)*

Then, one to three days later, one of two things:

- With village-mail: a letter. *"I know why you refused. I'm glad you did. Tomas"*
- Without it: the next time the player speaks to Tomas, he opens with it.
  *"*quiet* I earned some today. Honestly. First time in a while."* ▸ Right.

**What this pass changed here.** Without the mail mod this used to arrive as
a green italic chat whisper, which is the one channel the voice queue's own
contract says a villager never uses. It now waits in his mouth, like
everything else he has to say to you.

And weeks later, at thirty percent per conversation while the memory holds:

> **Tomas**
> I asked you to do something wrong. And you said no.
>
> ▸ You'd have regretted it. → "I'd have regretted it. I regret asking. That's smaller. I can carry that." ▸ It's alright.
> ▸ *say nothing* → "Thank you for not making me say more." ▸ *nod*
> ▸ Leave it there. *(exit)*

Before the one-voice work that memory line fired on *every* conversation with
Tomas, in front of whatever greeting came next, with no reply behind it.

**Verdict.** Now complete end to end, and the one exchange in the corpus
that carries the ethos in its own voice. The three snags the first pass
found here were all closed in the second:

1. **Delay is an answer.** "I need to think about that." sits beside accept
   and refuse. Nothing is recorded, and they may ask again.
2. **The stage directions the model cannot do** are gone from the offers and
   the reactions in `MisnomerQuest` and from the registry's copies. "*stomach
   audibly growling*" became nothing; the dirt soup carries it. "*grabbing
   your arm*" became "*steps in close*", which the model can do.
3. **The last em-dash** went with the rest of them.

---

## C. The broken fence (an apology carried, NEUTRAL)

A message quest from a stranger's standing: the player barely knows Aldric.

> **Aldric** *(after asking for work)*
> My cow got loose and damaged their garden. Please tell Petra I'm sorry about the broken fence.
> » Receive: Apology for Petra
>
> ▸ Consider it done.
> ▸ Maybe another time. *(exit)*

The quest book: *find Petra, farmer: Aldric needs to make things right.*

> **Petra (farmer)**
> Aldric sent you? ...Go on then.
>
> ▸ Pass on the apology. *(pinned, with the icon)*
> ▸ I should go. *(exit)*

> **Petra** *(after handing it over)*
> *long pause* ...Tell them I heard them.
>
> ▸ Take care. *(exit)*

The player now carries *Petra's reply*, a named piece of paper.

> **Aldric** *(back again)*
> You talked to them? What'd they say?
>
> ▸ Here's what they said. *(pinned, with the paper's icon)*
> ▸ I can't finish this one. Sorry.
> ▸ Never mind. *(exit)*

> **Aldric** *(the handover)*
> "*long pause* ...Tell them I heard them." *quiet for a second* ...Right. Good to know where I stand.
>
> ▸ I'll leave you to it. *(exit)*

**What this pass changed here.** Three things on this one quest.

- The pinned button read *"Here - the Paper you asked for."* The generic
  turn-in label was built from the submission item, and the reply token is
  paper. It now uses the quest's own words.
- Aldric's completion line began *"What did they say?... "*, which is the
  question the previous screen had already asked him. He now repeats what
  the player told him and reacts to it, which is what people do.
- The same line used to be shown a day late and a generic thanks shown in
  its place, as in tree A.
- The quest book used em-dashes as separators. Plain glyphs now.

**Verdict.** The best-shaped quest in the mod: every step is a decision the
player makes with a button that says what it does, and the walk back is
carrying something. Two snags: the exit label on the return screen can draw
"Never mind." from the old cancel pool, which is exactly the trail-off the
guide bans, and it is fixed in the pool now; and Petra's "*long pause*" is
the ellipsis budget spent on a stage direction. Small.

---

## D. What you took (the accusation, after a chest)

The player opened a village chest an hour ago. Every villager knows.

> **Rowena (librarian)**
> *cold* We know what you took from Oakhollow.
>
> ▸ I'll put it back. → "Then put it back. What I know can change after that. Not before." ▸ Fair.
> ▸ I didn't take anything. → "*looks at your hands, then at your face* Alright." ▸ Think what you want.
> ▸ It was just sitting there. → "So is my door, at night. I still expect it shut come morning." ▸ Point taken.
> ▸ *go* *(exit)*

Owning it pays one point and quiets the rest of the village. Denying it costs
two and the chorus keeps going, door to door, until the memory fades or the
player owns it somewhere.

**What this pass changed here.** This exchange did not exist. The accusation
was a prefix glued onto a greeting, and the buttons under it were "What's
fresh?", "Any reading that needs doing?" and "Never mind." The player was
being made to ignore the most direct thing anyone had said to them.

**Verdict.** Complete, and the first place in the mod where the reputation
system is something you can *talk to*. The one gap the first pass left, that
"I'll put it back" was a promise nothing checked, is closed: owning the
theft records the promise, putting anything into a village chest within a
day keeps it, and the next villager who brings it up says so.

> **Rowena** *(later, after the return)*
> *nods* You put it back. That's the end of it, then, as far as I'm concerned.
>
> ▸ Said I would. → "People say. Fewer do. I'll remember which you were." ▸ Well.
> ▸ *say nothing* → "*nods again* Good. Then neither will I." ▸ *nod*
> ▸ Well. *(exit)*

A promise nobody acts on lapses quietly after a day, which is the mod's rule
for everything that goes cold.

---

## E. The bar (a registered confession, for contrast)

For reference, what a good exchange in the registry looks like. This one was
rebuilt in the earlier sweep and untouched since.

> **the butcher** *(at night, TRUSTED)*
> *lowers voice* Sometimes I name them. Makes it harder. But more honest, maybe.
>
> ▸ Why make it harder on yourself? → "First year, it got easy. Easy scared me worse than the sound ever did. The names keep it from getting easy." ▸ It's late.
> ▸ What was the last one called? → "Gerald. Big brown eyes. Terrible temper. ...Tuesday." ▸ Goodnight.
> ▸ Say nothing. → "...Thanks for not making it a thing." ▸ *nod*

And the next time, instead of the confession: *"Named the new calf this
morning. Told you I would. ...Marigold."* ▸ Marigold. Goodnight.

Trees A through D now meet this bar in shape. They do not all meet it in
specificity: the generated lines are necessarily general, because they are
written for any villager in any village. That is the right trade for lines a
system produces, and the wrong one for lines a writer produces, which is why
the registry and the generated openers are kept apart.

---

## The second pass: the channel

Tracing the trees above surfaced a rule the code stated and then broke in
ninety places. The voice queue's own contract says a villager is heard in
conversation, read on paper, or not at all. Every quest class had lines going
to chat anyway: the deep-quest confessions arrived as four grey system
messages three seconds apart while the screen closed behind them; the
witness's clue in a mystery printed in aqua; a gift's thanks, a kept secret's
"the moment passes", the abandon reaction, the "you had it a second ago"
refusal, all chat.

Now:

- **Speech is held, and played where it was earned.** `VillagerVoice` holds
  lines and sequences of beats. The manager plays them as the close of the
  moment that produced them (hand-in, refusal, accept, abandon, gift, secret,
  clue), one screen per beat, with "*wait*" / "*say nothing*" / "Go on." to
  stay for the next beat and a walk-away on every screen. If the player is
  not there, the same lines open the next conversation.
- **The pause is the player's.** A confession's beats used to be a timer.
  Now the pause between them is the player choosing to stay, which is what a
  pause in a confession is.
- **Chat is for the world.** Smoke on the wind, a sulfur spring found, the
  plot edges lit, a villager's death: those still print. Nothing a living
  villager says does.
- **Delay is an answer.** The misnomer screen has "I need to think about
  that." beside accept and refuse. Nothing is recorded; they may ask again.
- **Bulk hygiene.** Every em-dash in the source is gone, by rule and then by
  ear: cut-off speech kept its cut (as an ellipsis), objectives took a colon,
  everything else a full stop or a comma. The misnomer offers and reactions
  lost the stage directions the model cannot perform; what could be carried
  by the words is carried by the words.

The third pass took the rest: the quest classes' stage directions (87
rewritten to what the model can do, or cut), the first-meeting pools' hand
work (25), and a check of the ellipsis budget, which turned out to be met:
three lines in the mod carry two, and each earns it.

## The fourth pass: the lore

The ethos calls the ontological lines the rarest and most powerful in the
mod, and the guide's own list of what moonfaced sounds like could have been
transcribed from them: "Names are cages. Everything unnamed is free."
"Consciousness is heavy. Sleep is mercy." "Questions create the things they
ask about." "Some souls are anchored. Some souls drift. Yours... drifts."
Ninety-nine lines across the two lore files were rewritten toward what the
guide says works, which is a specific thing in the world that is actually
strange:

> "The next village over has the same well. Not the same kind. The same
> well. The crack runs the same way."
>
> "I counted the sheep at four. Counted again, six. Nothing went in or out.
> I don't count them anymore."
>
> "Every one of us goes in at the same moment. Not near the same moment. The
> same one. Watch the doors tonight."

The child lore was already this and was not touched. The contrarian's folk
nonsense ("Enchantments are just very small villagers living in your tools")
is a character, not a failure, and stays. The almost-said pool, which was
one shape ten times ("Be careful in the... no. You know."), now varies the
way the guide asks: changing the subject, getting busy, looking at something
specific, a laugh at nothing.

Also in this pass: the fortune-cookie closers that had crept into quest
completions and letters ("That matters more than it sounds"), one line that
was the guide's banned example verbatim ("The veil's thinnest at night"),
and the high-standing gift lines that delivered a thesis about belonging.

## Verified on a server

The dev server was started headless three times on this build. Fabric mods
initialise before the EULA check, so the whole registry is constructed and
the startup validator runs and prints, and the server then exits on its own
without anyone accepting anything. Results:

- No errors, no exceptions, no mixin complaints.
- The validator reports nothing about the new nodes: every generated opener's
  routes resolve, every terminal has a walk-away, every significant moment
  has its echo.
- The 49 warnings the validator had before this session were all one kind:
  an authored option carrying reputation on a node whose click opens the trade
  screen (where the value never applied) or accepts work (where it quietly
  paid two points per accept, the click-for-standing the guide bans). All are
  zero now, and the validator is silent.

The same channel rule was carried into the sibling mods that write their
own quest classes: village-builder's three and poopsmith's three sent their
completion lines to chat and now hold them for the villager, so a finished
errand for the builder closes on the builder's own words like any other.

Not verified this way: the put-back slot mixin only applies when the slot
class loads, which is after the EULA check, and nothing here has been
clicked through by a person. The beat chains, the held closes, and the
promise all deserve a live run.

## What this pass fixed

- A refusal's reaction plays as the close of the refusal, not the opener of
  the next visit.
- A handover shows the quest's own line, once, now; the generic pool is the
  fallback.
- Handing a child fireworks instead of TNT, or a hungry neighbour bread
  instead of a plan, no longer closes the screen silently when the quest has
  something to say.
- Delayed recognition without village-mail goes through the villager, not
  chat.
- Held lines are a floor. They are too varied for any one set of replies.
- The message quest's turn-in label and doubled question, and its em-dashes.

## What still snags

- The misnomer offer screen has no way to not answer. See B.1.
- `MisnomerQuest` and the registry's misnomer nodes carry stage directions
  the model cannot perform, and one em-dash. A sweep of that file is the
  next content job.
- The lesson mods' teachers deliver multi-sentence lectures. That is their
  register, chosen on purpose, and it is still the longest speech any
  villager gives. It was not touched beyond the interrupt-dash.
