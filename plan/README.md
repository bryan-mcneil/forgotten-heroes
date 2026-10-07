# plan — the step instructions, one file per phase

## What lives here

`phase_0_foundations.md` through `phase_6_launch.md`. Each step inside them follows one template:
Goal → You will have → Sensei notes (What / Why / How / Where) → Do this → Verify → Commit → Check
your understanding. `PLAN.md` at the repo root is the index: one row per step, with the checkbox.

## What must NOT live here

- Status. A step is marked done only by flipping its checkbox in `PLAN.md`, in the same PR that finishes it.
- Silent changes. Re-ordering, splitting or marking a step optional is allowed inside a phase, but every
  change is logged in `PLAN.md` → "Changelog of the plan".
- AWS console walk-throughs written early. The click-by-click instructions for Steps 06, 34, 61, 62, 66,
  67 and 68 are written when each step arrives, because the console UI drifts (Q31).

## How do I use it

Find the first unchecked box in `PLAN.md`, open its phase file, read only that step. Never start the
next step before the current PR is merged.
