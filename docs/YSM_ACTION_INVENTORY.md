# MoreAnimation action inventory for official YSM

The inventory follows Java trigger paths. Entries that merely exist in an animation JSON but are
never selected by project code are not counted as actions.

## Result

- 38 action names are selected by current Java code and have common bone animation data in
  `unknown.animation.json`.
- All 38 are registered in the YSM bridge and parse successfully.
- All 38 now reach both renderers through `MaidAnimationData.activeAction(maid)`.
- The bridge remains a soft YSM 2.6.5 integration. No YSM class appears in a common event or packet
  descriptor.

## Terminal and random pools

| State | Actions | Trigger path |
|---|---|---|
| Standing | `circledance`, `!??!` | terminal or configured random scheduler |
| Sitting | `come2`, `ha`, `tastetail` → `eattail` | terminal, configured random scheduler, or legacy held-item condition |
| Sleeping | `come`, `sleep2`, `situp` | terminal, configured random scheduler, or legacy held-item condition |
| Other terminal action | `injured_kneel` | terminal or damage threshold |

The random scheduler now publishes its selected action through `MaidAnimationData.start`. This is
the passive-path change that was missing when random `circledance` previously failed to appear on
YSM models. Probability, interval, enabled pools and state checks remain the existing MoreAnimation
rules.

## Existing MaidAnimationData event actions

`maid_bow`, `refuse`, `injured_kneel`, `death_fall`, `death_drown`, `death_burn`, `death_ranged`,
`fear_retreat_fall`, `pet_reaction`, `pet_reaction_hold`, `pet_other_head`,
`pet_other_head_raise`, and `hugtogether`.

These already used `MaidAnimationData.start`; the work here registers all of their clips in the
general YSM animation registry.

## Legacy conditions migrated to MaidAnimationData

| Actions | Existing condition retained |
|---|---|
| `come`, `come2`, `sleep2`, `tastetail`, `eattail` | owner held item and maid pose |
| `weidu` | sitting inside the berry-bush arrangement |
| `morebeg` | health below 30 percent |
| `catchbyhook` | fishing hook attached |
| `hurt` | five player hits within the existing attack window |
| `kowtow` | projectile hit |
| `drowning` | in water with no air |
| `pray` | shrine event |
| `watchtombstone` | nearby tombstone and existing cooldown |
| `tailpull`, `CLEANTAIL` | tail-pull event and accumulated pull count |
| `ear_pull_left`, `ear_pull_right` | ear click/hold event and chosen side |
| `hang`, `game_lost2` | leashed off ground/on ground |
| `tailcircle` | cake within two blocks |
| `dance1` | standing while owner holds an iron nugget |
| `circledance`, `!??!` | owner-held sugar/TNT as well as random pool |
| `lips` | player watches while holding food for the existing delay |

These conditions are evaluated by common MoreAnimation server code. They call the same
`MaidAnimationData.start/stop` methods as terminal and interaction actions, so YSM has no timer,
probability, cooldown, priority or packet implementation of its own.

## Animation data and bones

The shared parser supports rotation, position, scale, numeric keyframes, scalar scale values,
missing `animation_length`, looping, one-shot/held-last-frame clips, and multiple bones. Bone names
match exactly first and then case-insensitively; the verified `MRoot` to `MAllBody` fallback remains.
Bones absent from a particular YSM skin are skipped per model and reported once at action start in
debug logging.

`CLEANTAIL` contains a `timeline` assignment (`v.cleantial`) in addition to its bone animation. YSM
receives all `CLEANTAIL` bone transforms, but the bridge does not execute that model variable. The
omission is logged once when resources load. No other one of the 38 actions has an omitted feature.

The terminal's eight persistent expression choices are expression overlays, not actions, and keep
their existing `moreanimation_expression`/parallel-controller path. Wine Fox form visibility and
dismemberment entries are model-specific overlays rather than common MoreAnimation actions; they
are not applied to arbitrary YSM skins.
