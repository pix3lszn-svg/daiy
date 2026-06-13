# Match-day guide

How to run a match with the EmeraldPerks nation/squad system + the Overworld
Cup datapack. Everything here is run by an admin (op) unless noted.

## The idea in one line

A **nation** is everyone who signed up (`/nation join`); a **squad** is the
≤12 from that nation who actually play, chosen by you = **reserved players +
a random fill**. Only the squad takes the field.

---

## One-time setup (do once, ever)

1. Create your nations:
   ```
   /nation create cherry
   /nation create desert
   ```
   (Use lowercase, and make the names match what you'll type in `cup:startmatch`.)

2. Make sure the datapack's `cup:setup` has been run once (creates the
   match_home / match_away teams):
   ```
   /function cup:setup
   ```

Players can now sign up any time with `/nation join cherry` (works in
adventure mode). They can `/nation leave` or switch by joining another.

---

## Before each match

Do this for **both** nations that are playing.

### 1. Add your reserved players (the guaranteed starters)

These people are locked into the squad every draw — set them once and they
stick until you remove them:
```
/nation reserve cherry Notch
/nation reserve cherry Dream
```
- Remove one with `/nation unreserve cherry Notch`.
- Reserved players don't even need to have joined the nation — they're added
  for sure. If a reserved player is offline when you draw, they still hold
  their slot and get tagged the moment they log in.

### 2. Draw the squad (reserved + random fill)

When your players are online and ready:
```
/nation draw cherry
```
This builds the ≤12 squad: your reserved players first, then a **random fill**
from the online members of that nation until it hits 12. It prints exactly who
was picked, e.g.:
```
Drew cherry's squad: 12/12 players (2 reserved + 10 random from 18 online members).
Squad: Notch, Dream, ...
```
- Run it again any time to **re-roll** the random spots (reserved players stay).
- `/nation squad cherry` — show the current squad.
- `/nation cleardraw cherry` — empty it.
- If fewer than 12 nation members are online, the squad is just smaller; it
  tells you.

Now do the same for the other side:
```
/nation draw desert
```

### 3. Start the match

```
/function cup:startmatch {home:"cherry",away:"desert"}
```
This puts **only the drawn squads** on the pitch (home in red, away in blue),
lines them up, and runs the countdown automatically. The ball spawns at kickoff.

---

## Quick reference

| Command | Who | What |
| --- | --- | --- |
| `/nation create <name>` | admin | Add a nation (one-time) |
| `/nation join <name>` | players | Sign up to a nation |
| `/nation reserve <nation> <player>` | admin | Lock a guaranteed squad spot |
| `/nation unreserve <nation> <player>` | admin | Remove a reserved spot |
| `/nation draw <nation>` | admin | Pick the squad (reserved + random) |
| `/nation squad <nation>` | anyone | Show the drawn squad |
| `/nation cleardraw <nation>` | admin | Empty the squad |
| `/function cup:startmatch {home:"a",away:"b"}` | admin | Field the two squads |

## Gotchas

- **Draw before you start.** `cup:startmatch` fields the `squad_` tag, so a
  nation with no drawn squad spawns empty.
- **Lowercase, consistent names.** `/nation create cherry` then
  `home:"cherry"` — tags are case-sensitive.
- **Random fill = online members only.** Draw when the players are actually
  on, right before the match. Reserved players are the exception (honored even
  if offline).
- **Squad size** is 12 by default; change `nations.squad-size` in
  `config.yml` if you want a different cap.
