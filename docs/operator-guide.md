# Operator Guide

Implementation: `src/defensecompliance/` (see README.md's
Implementation section for the module map). "the advisor" below is
`defensecompliance.defensecompliancellm`; "the Defense-Transfer
Compliance Governor" is `defensecompliance.governor`; the Three
Principles classification and Specially-Designated-Secrets clearance
tracks are separate `:track`s (`:three-principles` /
`:specially-designated-secrets`) on the same client engagement,
assessed and filed independently. 全省庁統一資格 (unified qualification)
verification and 指名停止 (bid-suspension) status are engagement-level
gates checked on every `:filing/submit`, regardless of track.

## First Deployment

1. Confirm the client already uses (or has completed the equivalent of)
   `cloud-itonami-iso3166-jpn` for general Japan market-entry; this repo is
   an agency-specific supplement, not a substitute.
2. Register the client's intake: business type, the specific
   MOD-regulated activity involved, prior filing/compliance
   history in Japan if any.
3. Run the advisor in read-only mode against Ministry of Defense's
   (防衛省) published guidance.
4. Compare the checklist against the client's current documentation.
5. Enable gated filing/compliance-draft assistance once the
   Defense-Transfer Compliance Governor contract is trusted; actual submission always
   requires human sign-off.

## Minimum Production Controls

- client-owned data store for compliance documents
- clear provenance (official MOD source citation) for every
  requirement surfaced
- approval workflow for any filing, registration, or compliance-program
  submission
- named referral relationship with Japan-licensed counsel or a registered
  agent for anything beyond checklist/draft assistance
- monthly audit export

## Certification

Certified operators must prove data provenance, audit traceability, that
automated actions cannot bypass the Defense-Transfer Compliance Governor, and a working
referral relationship with Japan-licensed counsel or a registered agent for
whatever licensed representation Japanese law requires for actual
MOD filings.
