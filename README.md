# cloud-itonami-iso3166-jpn-mod

Open ISO 3166 Agency Blueprint for **JPN-MOD**: Ministry of Defense
(防衛省, MOD) — a Japan-agency-level LEAF under
the `cloud-itonami-iso3166-jpn` country-level coordinator.

This repository designs a forkable OSS business for an independent
compliance consultant: an already-incorporated operator (typically one
already using `cloud-itonami-iso3166-jpn` for general Japan market entry)
gets a Compliance Advisor + independent **Defense-Transfer Compliance Governor** to
navigate classification under the Three Principles on Transfer of Defense Equipment and Technology (防衛装備移転三原則) for a defense-related public contract, and security-clearance prerequisites for staff handling specially designated secrets (特定秘密保護法).

## No robotics premise — digital/data service exemption

Agency-specific compliance navigation is a pure data/software service with
no physical-domain work — the same exemption class as `cloud-itonami-6310`
and `cloud-itonami-gtin-*`. `blueprint.edn` sets
`:itonami.blueprint/robotics false` and `:required-technologies` lists only
real capabilities (`:identity`, `:forms`, `:dmn`, `:bpmn`, `:audit-ledger`),
no `:robotics`.

## Core Contract

```text
operator intake + prior filing/compliance history
        |
        v
Compliance Advisor -> Defense-Transfer Compliance Governor -> compliance draft, or human sign-off
        |
        v
gated filing / registration / compliance-program submission + audit ledger
```

No automated proposal can submit a filing or registration the governor
refuses, suppress a compliance record, or claim a legal conclusion the
governor has not cleared. `:filing/submit` is never in any phase's `:auto`
set — it always requires human sign-off (mirrors `cloud-itonami-M6910`'s
`filing-submit-never-auto-at-any-phase` invariant).

## Implementation

`src/defensecompliance/` — a langgraph-clj StateGraph actor, same
containment shape as `cloud-itonami-iso3166-ago`'s `marketentry.*` /
`cloud-itonami-iso3166-jpn-digital`'s `digitalprocurement.*` (advisor
sealed to proposals-only, independent governor, append-only ledger,
`Store` protocol swap, phase gate):

- `facts.cljc` — the Three-Principles + Specially-Designated-Secrets +
  procurement-qualification catalog, the ONLY source of
  regulatory-requirement facts the actor may cite. Three tracks,
  `:three-principles`, `:specially-designated-secrets`, and
  `:procurement-qualification`, each with its own owner authority and
  legal basis.
- `governor.cljc` — the Defense-Transfer Compliance Governor: a
  spec-basis/no-fabrication HARD check, an evidence-incomplete check, a
  **全省庁統一資格 (unified qualification) missing** HARD check
  (`:filing/submit`, either track — a documented prerequisite to
  contract with MOD at all), a **指名停止 (bid suspension)** HARD check
  (`:filing/submit`, either track, unconditional — a suspended operator
  is not eligible for a new MOD contract during that period), an
  **適性評価 (aptitude assessment / clearance) missing** HARD check
  (`:specially-designated-secrets` track, `:filing/submit` — a genuine
  legal prerequisite before personnel may handle Specially Designated
  Secrets duties), an independently-recomputed engagement-fee-mismatch
  check (three revenue lines: base fee + monitoring subscription +
  optional audit-export package), a confidence-floor/actuation gate,
  and double-draft/double-submit guards, per track.
- `store.cljc` — `MemStore`/`DatomicStore` (via
  `kotoba-lang/langchain-store`, not a hand-rolled `enc`/`dec*`) for
  the `engagement` entity, which tracks the `:three-principles` and
  `:specially-designated-secrets` tracks' filing state independently,
  plus the engagement-level `:unified-qualification-verified?` and
  `:bid-suspended?` gates.
- `registry.cljc` — pure-function filing-draft/filing-submit record
  construction, one sequence per track.
- `defensecompliancellm.cljc` — the Compliance Advisor (mock LLM,
  proposals only).
- `operation.cljc` — the StateGraph: intake → advise → govern → decide
  → [request-approval →] commit/hold, `interrupt-before` on human
  approval.
- `phase.cljc` — phase 0→3 rollout; `:filing/draft`/`:filing/submit`
  are permanently absent from every phase's `:auto` set.

Ops: `:engagement/intake`, `:compliance/assess` (per-track evidence
checklist), `:filing/draft`, `:filing/submit` — the latter three take a
`:track` (`:three-principles` or `:specially-designated-secrets`) in
the request, since one engagement runs both filing tracks
independently. `:procurement-qualification` is a citation-only spec-
basis entry for the unified-qualification/bid-suspension checks, not a
track this actor drafts/submits filings for.

This is an especially security-sensitive domain (national defense
secrecy law): every module stays strictly at the compliance-PROCESS
level (paperwork / eligibility / clearance-status tracking) — nothing
here models, stores, or reasons about classified or operational
content.

## What this is NOT

- **Not Ministry of Defense (防衛省) itself, and not the
  government of Japan.** See [`docs/business-model.md`](docs/business-model.md)
  for the boundary with `com-etzhayyim-ooyake`, `matsurigoto`,
  `com-etzhayyim-toritsugi`, `legal-entity.etzhayyim.com`,
  `cloud-itonami-M6910`, and the country-level `cloud-itonami-iso3166-jpn`.
- **Not legal or tax advice.** Every regulatory claim must cite the
  official MOD source and route final filings to
  Japan-licensed counsel or a registered agent where the law requires
  licensed representation.

## Capability layer

Resolves via [`kotoba-lang/iso3166`](https://github.com/kotoba-lang/iso3166)
(code `JPN-MOD`, `:parent "JPN"`, cross-referenced to ooyake's
`gov.jpn.mod`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
