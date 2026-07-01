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

## License

AGPL-3.0-or-later.
