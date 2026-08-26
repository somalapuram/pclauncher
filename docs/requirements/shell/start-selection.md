# Shell — No Caret Until the Keyboard Is Used

Status: **Implemented** (2026-08-24)

The Start menu highlights a row only once the user has engaged the keyboard. Amends
[`start-menu.md`](start-menu.md), which specified keyboard navigation without saying when the
selection begins to exist.

## Context

Opening the Start menu shows the first app already highlighted. It is the keyboard caret, sitting at
index 0 because the selection is an `Int` initialised to zero and there is no way to express "no
selection yet". Nothing the user has done put it there.

It reads as though the first app is hovered or focused, and it is worse now that hover has a
treatment of its own: two different states, one of which the user caused and one of which they did
not, drawn in similar ways on the same grid.

**A caret is a claim about where the keyboard will act.** Showing one before any key has been
pressed makes a promise the user has not asked for — and on a pointer-first shell (SRS §6.1
principle 3) the pointer is the more likely input, so the claim is usually wrong as well as
unrequested.

**Typing is different.** Once a query is being typed, the top hit *should* be selected: Enter has to
launch something, and the whole point of the search field is that the result you want is at the top.
That is the user engaging the keyboard, and the caret is then exactly right.

## Requirement

1. **No row is selected when the menu opens** with an empty query.
2. **The first navigation key selects the first row** rather than moving from an assumed one.
3. **Typing a query selects the top result**, so Enter launches it.
4. **Clearing the query clears the selection** — back to the state the menu opened in.
5. **Enter with nothing selected does nothing** rather than launching an arbitrary app.
6. **A selection that outlives its row is clamped**, not left dangling, when filtering shrinks the
   list.
7. **Selection and hover remain visually distinct** ([`../design/hover-feedback.md`](../design/hover-feedback.md)).
8. **The rule is a pure function** of the current selection, the key pressed and the result count.

## Acceptance criteria

- [x] Opening the menu highlights nothing — checked on device, Calendar is no longer washed.
- [ ] Pressing Down selects the first app. **Not verified on device:** hardware key injection reaches
      the IME rather than the menu's key handler here; covered by `StartSelectionTest`.
- [ ] Typing a query highlights the top result; Enter launches it — same limitation.
- [x] Clearing the query removes the highlight again (`selectionAfterQuery` returns null on blank).
- [x] Enter with no selection launches nothing — the launch is behind `selected?.let`.
- [x] Filtering to fewer results leaves the selection on a real row, and does not invent one where
      there was none.
- [x] The selection rule is unit-tested, including the first keypress and the empty-query case.
- [x] `./gradlew test lint assembleDebug` green.

## Notes

- **Why not simply hide the highlight at index 0.** The selection would still exist, so Enter would
  still launch the first app before any key was pressed — the visible half of the bug fixed and the
  behavioural half left in.
- **Not in this slice:** remembering the selection across openings, or a selection that follows the
  pointer.
