# MoreAnimation action inventory for the YSM bridge

This inventory is based on Java trigger paths, rather than every entry present in
`unknown.animation.json`. Durations are the action lifetime used by the mod. The
animation clip may be shorter and loop within that lifetime.

## Random actions and terminal actions

| Action | Source/state | Trigger | Clip loop | Lifetime | Movement lock | YSM bridge |
|---|---|---|---|---:|---|---|
| `circledance` | standing pool | random scheduler + terminal | yes | 90 ticks | no | verified in game |
| `!??!` | standing pool | random scheduler + terminal | no | 30 ticks | no | pending |
| `come2` | sitting pool | random scheduler + terminal | yes | 100 ticks | no | first expansion build |
| `ha` | sitting pool | random scheduler + terminal | yes | 120 ticks | no | pending |
| `tastetail` | sitting pool | random scheduler + terminal | yes | 145 ticks | no | pending |
| `eattail` | `tastetail` transition after 45 ticks | automatic transition | yes | 100 ticks | no | pending |
| `come` | sleeping pool | random scheduler + terminal | yes | 200 ticks | no | pending |
| `sleep2` | sleeping pool | random scheduler + terminal | no | 130 ticks | no | pending |
| `situp` | sleeping pool | random scheduler + terminal | no | 400 ticks | no | pending |

The current random scheduler stores its winner in `GameLostAnimation.SCHEDULED`
and the ordinary TLM animation mixins read that state directly. It does **not**
call `MaidAnimationData.start`, so it does not currently produce an
`activeAction`. This is the reason random/passive `circledance` did not reach the
YSM bridge in the earlier game test. The later passive-integration stage must
route this existing scheduler result through `MaidAnimationData`; it must not add
a YSM timer or probability system.

## Event and interaction actions

| Action | Source | Clip loop | Lifetime | Movement lock | YSM bridge |
|---|---|---|---:|---|---|
| `maid_bow` | owner enters bow range | no | 48 ticks | yes | first expansion build |
| `refuse` | owner approaches with blocked food | no | 20 ticks | yes | pending |
| `injured_kneel` | damage threshold or terminal | hold last frame | 60 ticks | yes | pending |
| `death_fall` | fall death | hold last frame | 28 ticks | yes | pending |
| `death_drown` | drowning death | hold last frame | 72 ticks | yes | pending |
| `death_burn` | fire death | hold last frame | 56 ticks | yes | pending |
| `death_ranged` | ranged death | hold last frame | 32 ticks | yes | pending |
| `fear_retreat_fall` | HandItem attack | hold last frame | 50 ticks | yes | pending |
| `pet_reaction` | pet target/reaction | no | 64 ticks | yes | pending |
| `pet_reaction_hold` | held pet reaction | yes | 30 ticks or session lifetime | yes | pending |
| `pet_other_head` | pet another entity | yes | 78 ticks | yes | pending |
| `pet_other_head_raise` | raised pet interaction | no | 16 ticks | yes | pending |
| `hugtogether` | hug interaction, both maids when applicable | no | 94 ticks | yes | pending |

All event and interaction actions above already call `MaidAnimationData.start`
and therefore already converge on `MaidAnimationData.activeAction`. They need no
YSM-specific triggering code.

## First expansion gate

The first expansion build deliberately contains three registered YSM clips:
`circledance`, `maid_bow`, and `come2`. `maid_bow` covers a one-shot, locked
interaction action. `come2` covers a short looping clip with a large optional
tail-bone set. Missing bones remain safe because the bridge applies only channels
whose bone names exist in the current YSM model.
