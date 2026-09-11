# cloud-itonami-isco-5153

Open Occupation Blueprint for **ISCO-08 5153**: Building Caretakers.

This repository designs a forkable OSS business for an independent building caretaker: a building-inspection robot performs common-area monitoring and maintenance-support tasks under a governor-gated actor, so the caretaker keeps their own maintenance and safety records instead of renting a closed property-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a building-inspection robot performs common-area monitoring, minor maintenance-support and utility-reading tasks under an actor that proposes
actions and an independent **Building Caretaker Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near tenants, electrical systems or elevators) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
building plan + maintenance schedule + tenant request
        |
        v
Caretaker Advisor -> Building Caretaker Governor -> maintain/repair-support, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `5153`). Required capabilities:

- :robotics
- :forms
- :telemetry
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/building_caretaking/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `building-caretaking.store` — `Store` protocol + `MemStore`:
  registered buildings, maintenance tasks, incident reports. A task or
  incident report can only be recorded against a registered building
  (building provenance).
- `building-caretaking.governor` — `BuildingCaretakingGovernor`: `assess`
  gates a proposal against the building env. Hard invariants force
  `:hold` (no building, direct-write instead of `:propose`, or a
  `:structural`/`:electrical` task below `:high` safety-class); a
  structural or electrical task always requires `:high`+ safety-class
  and thus `:human-approval` — it can never be auto-approved;
  low-confidence proposals also escalate.

```bash
kbb -M:test   # 7 tests, 12 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 19th `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210`, `-5223`, `-7231`, `-8121`, `-9111`,
`-2512`, `-1120`, `-4110` and `-3213` (ADR-2607012000).

## License

AGPL-3.0-or-later.
