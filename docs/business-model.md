# Business Model: Independent MOD Defense-Equipment Transfer & Security-Clearance Compliance Service — Japan (MOD)

## Classification

- Repository: `cloud-itonami-iso3166-jpn-mod`
- ISO 3166 (agency-level): `JPN-MOD`, parent `JPN`
- Ooyake cross-reference: `gov.jpn.mod` (Ministry of Defense / 防衛省)
- Activity: classification under the Three Principles on Transfer of Defense Equipment and Technology (防衛装備移転三原則) for a defense-related public contract, and security-clearance prerequisites for staff handling specially designated secrets (特定秘密保護法)
- Social impact: [:defense-transfer-clarity :security-clearance-access :public-spend-transparency]

## Customer

- an operator bidding on a MOD contract involving defense equipment or technology transfer
- an operator whose staff need security-clearance screening for a defense-related contract
- a foreign defense contractor confirming Japan's equipment-transfer rules before bidding

## Offer

- defense-equipment transfer classification walkthrough (防衛装備移転三原則)
- security-clearance prerequisite checklist for contract staff
- ongoing regulatory-change monitoring for MOD transfer-rule updates
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires Defense-Transfer Compliance Governor clearance and always escalates to human
  sign-off (`:filing/submit` is never automated at any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold that
  cannot be overridden by human approval alone — it must be corrected
  against a cited MOD source first
- this service does **not** provide legal or tax advice; characterization
  and filing on the client's behalf beyond checklist/draft assistance
  routes to Japan-licensed counsel or a registered agent
- every requirement cites the official MOD source or
  regulation, never invented

## Boundary with adjacent actors (read before forking)

- **`cloud-itonami-iso3166-jpn`**: the COUNTRY-level coordinator (general
  Japan public-sector market entry). This repo is a narrower, deeper
  AGENCY-level leaf — most operators need the country-level blueprint plus
  only the agency-level blueprints that actually apply to their contract.
- **`com-etzhayyim-ooyake`** (etzhayyim/root): read-only civic-wayfinding
  mirror of government structure, non-commercial, barred from acting as or
  for the government (G3 impersonation ban). This blueprint is commercial
  and never claims to be Ministry of Defense or an official channel.
- **`matsurigoto`** (etzhayyim/root): sovereign e-government statecraft —
  literally the government. This blueprint is an independent operator that
  engages with MOD under its public rules — never the
  agency itself.
- **`com-etzhayyim-toritsugi`** (etzhayyim/root): guides a consenting
  INDIVIDUAL citizen through their OWN procedure, non-profit,
  donation-only. This blueprint's client is a business operator, not an
  individual citizen, and it is commercial.
- **`cloud-itonami-M6910`**: helps a client BECOME a legal entity
  (incorporation, ISIC 6910) — a prior, different regulatory phase (company
  law). This blueprint assumes incorporation is already done and handles
  MOD-specific compliance (a different regulatory domain).
