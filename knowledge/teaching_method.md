# Teaching method v2 (research-based)

> Written 2026-10-16, after Bryan said twice (Step 26 and the start of Step 28) that he could not follow the
> lessons. This file is what the research says about learning from text alone, and the loop every concept is
> taught with from Step 28 on. It replaces "dictate → type → quiz". `project_management.md` §7 keeps the
> What / Why / How / Where headings; this file says how those parts are delivered.

## 1. What the research says

| Finding | What it means for us |
|---|---|
| Novices have no mental model yet, so facts have nowhere to go; too many facts early reinforce a wrong model (Wilson, *Teaching Tech Together*) | Every concept starts with a picture or analogy of how the thing works, before any name or file |
| Working memory holds a few items; new material must be chunked and paced by the learner (Sweller's cognitive load theory; Mayer's segmenting and pre-training principles) | One chunk per message; the words a chunk needs are defined before the chunk, at most five |
| Worked examples beat "figure it out" for novices; subgoal labels on the example make the structure visible; the example is faded as skill grows and gets in the way once the skill is there (Sweller; Margulieux & Catrambone 2016; Renkl; the expertise-reversal effect) | The first file of a kind is shown complete, with a numbered label on each part; the second is a skeleton with blanks; the third is labels only |
| Predicting before running turns copying into learning (PRIMM: Predict, Run, Investigate, Modify, Make; Brown & Wilson "have learners make predictions") | No command runs before Bryan has written one line saying what it will print |
| Retrieval practice and spacing are the two highest-utility study techniques; re-reading and highlighting are the lowest (Dunlosky et al. 2013) | Each session opens with three closed-book questions on the last one, never with "re-read the journal" |
| Self-explanation works when the learner produces it (Chi; Dunlosky) | The one sentence in Bryan's words is the sign-off, and it is written after the experiment, not after the reading |
| Naming the common misconception next to the correct idea improves learning (Wilson) | Each concept says "you might think X; it is actually Y" |
| A tutorial that stops to lecture fails; doing and explaining are separate kinds of text (Diátaxis) | The chunk does one thing; background goes to the journal or the glossary, linked, not inlined |
| Tired brains do not learn; novices who can trace code can write it (Brown & Wilson; Lister) | Sessions of about 45 minutes; the output of every run is read together, line by line, before moving on |

## 2. The loop for one concept

```text
0. MAP      where we are, in four lines, with a picture when the thing has parts
1. MODEL    the idea in plain words + one analogy to something Bryan knows (SQL, folders, a vending machine)
            at most five new words, each defined on the spot and added to the glossary
            "you might think X; it is actually Y"
2. EXAMPLE  the first file of its kind, complete, with numbered labels; Bryan types it
            then one question: "which line does X?"
3. PREDICT  one line from Bryan: what will this command print?  →  RUN  →  compare, read the output together
            a wrong prediction is the lesson; it goes in the journal
4. FADE     the second file of the same kind is a skeleton with the labels and blanks; the third is labels only
            for ideas Bryan already has (SQL-shaped ones), skip straight to this step
5. BREAK    change one thing (a key, a flag), predict, run, put it back
6. SAY IT   one sentence in Bryan's words; Claude checks it against the model and corrects gently
            that sentence is the "What I learned" of the commit
```

## 3. The shape of a session

1. **Recall** (2 minutes): three closed-book questions on the previous session, short definite answers;
   Claude gives the answers after Bryan has tried.
2. **Chunks**: one per message. A chunk is at most ~120 words of prose, one code block of at most ~25 lines,
   and exactly one thing for Bryan to do or answer. Claude waits for the reply before the next chunk.
3. **Stop** after about 45–60 minutes or at the end of a chunk, whichever comes first.
4. **Journal**: Claude writes the session paragraph in Bryan's words, including every wrong prediction and what
   replaced it.

## 4. Message rules (the medium is text only)

* Lead with the one thing to do.
* One new term per paragraph, defined in the same sentence.
* No history or asides (why a library renamed something) unless it blocks the chunk; it goes to the journal.
* A file longer than ~25 lines arrives in labelled parts, one part per message.
* Numbers (ports, versions) live in the command, not in the prose.
* A question and its answer never share a message: ask, wait, then answer.
* When an idea does not land after one explanation and one experiment, draw it (a visual explainer artifact)
  rather than write a second paragraph; anchor it in the layer below (Step 25's auto-configuration page worked).

## 5. What changes in the plan files

* "Check your understanding" stays as the *goal* of a step, but it is answered by doing (predict, run, break),
  not by recall after reading. The sentence in the PR is still the exit ticket.
* A step that carries more than one new idea is split before it starts (Step 28 → 28-a, 28-b).
* At the end of each step Bryan answers one question: *which part of the loop helped, which did not?* The
  answer goes in the journal and the loop is adjusted.

## 6. Sources

* Brown & Wilson, *Ten quick tips for teaching programming*, PLOS Comput Biol 2018 —
  <https://pmc.ncbi.nlm.nih.gov/articles/PMC5886386/>
* Wilson, *Teaching Tech Together* (mental models, misconceptions) — <https://third-bit.com/t3/models.html>
* Sentance & Waite, PRIMM — <https://static.teachcomputing.org/pedagogy/QR11-PRIMM.pdf>
* Margulieux & Catrambone, subgoal labels in text and worked examples (2016) —
  <https://static.raspberrypi.org/files/curriculum/quickreads/QR_31_Subgoal_Labels_29_11_2026.pdf>
* Dunlosky et al., *Improving students' learning with effective learning techniques* (2013), summary —
  <https://www.bellarmine.edu/docs/default-source/faculty-development-docs/08-effective-learning-techniquesc948fb96fcf16c298b1dff00007f963e.pdf?sfvrsn=2>
* Mayer, multimedia learning principles (segmenting, pre-training, coherence) —
  <https://ugc.futurelearn.com/uploads/files/7d/d6/7dd6188d-c343-4311-b064-ac98d2c95abc/Multimedia_Principles._R._E._Mayer__2020.pdf>
* Worked-example effect, fading and the expertise-reversal effect —
  <https://en.wikipedia.org/wiki/Worked-example_effect>
* Cognitive load theory applied to programming (Raspberry Pi Foundation pedagogy review) —
  <https://www.raspberrypi.org/app/uploads/2021/11/Teaching-programming-in-schools-pedagogy-review-Raspberry-Pi-Foundation.pdf>
* Diátaxis (tutorial vs explanation) — <https://diataxis.fr/>
