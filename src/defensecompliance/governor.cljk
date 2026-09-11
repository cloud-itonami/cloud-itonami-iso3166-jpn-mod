(ns defensecompliance.governor
  "Defense-Transfer Compliance Governor -- the independent compliance
  layer that earns the DefenseCompliance-LLM the right to commit. The
  LLM has no notion of what the Three Principles on Transfer of Defense
  Equipment and Technology (防衛装備移転三原則) or the Act on the
  Protection of Specially Designated Secrets actually require, whether
  全省庁統一資格 (Unified Qualification) is actually on file, whether an
  operator is currently under 指名停止 (bid suspension), whether staff
  slated to handle Specially Designated Secrets have actually completed
  適性評価 (aptitude assessment -- a genuine legal prerequisite before
  those duties may begin), whether a claimed engagement fee actually
  equals base + months x rate (+ optional export package), or when a
  draft stops being a draft and becomes a real-world MOD filing, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD.

  `:itonami.blueprint/governor` is `:defense-transfer-compliance-governor`
  (blueprint.edn).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual filing, registration, or compliance-program submission
  requires Defense-Transfer Compliance Governor clearance and always
  escalates to human sign-off'; 'a false or fabricated regulatory-
  requirement claim is a HARD hold that cannot be overridden by human
  approval alone') names exactly the checks below. This is an
  especially security-sensitive domain (national defense secrecy law)
  -- every check below stays at the compliance-PROCESS level
  (paperwork / eligibility / clearance-STATUS tracking); none of them
  model, store, or reason about classified or operational content.

  Nine checks, in priority order, ALL HARD violations except the
  confidence/actuation gate: a human approver CANNOT override the hard
  ones. The confidence/actuation gate is SOFT: it asks a human to look
  (low confidence / actuation), and the human may approve -- but see
  `defensecompliance.phase`: for `:stake :actuation/draft-filing`/
  `:actuation/submit-filing` NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                    -- did the compliance-track
                                         proposal cite an OFFICIAL
                                         source
                                         (`defensecompliance.facts`), or
                                         invent one?
    2. Evidence incomplete           -- for `:filing/draft`/
                                         `:filing/submit`, has the
                                         track actually been assessed
                                         with a full evidence checklist
                                         on file?
    3. Unified-qualification missing -- for `:filing/submit` (either
                                         track), when the engagement
                                         declares
                                         `:requires-unified-
                                         qualification? true`,
                                         INDEPENDENTLY verify
                                         `:unified-qualification-
                                         verified?` is true. 全省庁統一資格
                                         is a documented prerequisite to
                                         contract with MOD
                                         (予算決算及び会計令70条・71条,
                                         mod.go.jp) -- NOT track-scoped,
                                         it gates any filing/submit.
    4. Bid-suspended                 -- for `:filing/submit` (either
                                         track), UNCONDITIONALLY verify
                                         the engagement is not currently
                                         under 指名停止 (bid suspension).
                                         An operator under active
                                         suspension is not eligible to
                                         be awarded a new MOD contract
                                         during that period -- no
                                         'requires' flag, this always
                                         applies.
    5. Aptitude-assessment missing   -- for `:filing/submit` on the
                                         `:specially-designated-secrets`
                                         track, when the engagement
                                         declares `:requires-aptitude-
                                         assessment? true`, INDEPENDENTLY
                                         verify `:aptitude-assessment-
                                         verified?` is true. This is a
                                         genuine legal prerequisite --
                                         personnel may not be assigned to
                                         Specially-Designated-Secrets
                                         duties before 適性評価 is
                                         complete (japaneselawtranslation
                                         .go.jp, Act No. 108 of 2013).
    6. Engagement fee mismatch       -- for `:filing/submit`,
                                         INDEPENDENTLY recompute whether
                                         the engagement's own `:claimed-
                                         fee` equals `base-fee +
                                         monthly-rate x monitoring-
                                         months` (+ optional export-fee
                                         when `:audit-export?` is true)
                                         -- honest reapplication of the
                                         ground-truth-recompute
                                         discipline sibling actors use,
                                         matched against this repo's own
                                         three revenue lines
                                         (per-engagement compliance-
                                         review fee + recurring
                                         monitoring subscription +
                                         compliance-audit export
                                         package).
    7. Confidence floor / actuation
       gate                            -- LLM confidence below
                                         threshold, OR the op is
                                         `:filing/draft`/`:filing/submit`
                                         (REAL acts) -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off dedicated per-track `:three-principles-drafted?`/
  `:three-principles-submitted?`/`:sds-drafted?`/`:sds-submitted?`
  facts (never a `:status` value)."
  (:require [defensecompliance.facts :as facts]
            [defensecompliance.registry :as registry]
            [defensecompliance.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real MOD compliance filing package and submitting a real
  MOD filing are the two real-world actuation events this actor
  performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent MOD's Three-Principles or Specially-Designated-Secrets
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はコンプライアンス要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:filing/draft`/`:filing/submit`, the track's required
  evidence checklist must actually be satisfied."
  [{:keys [op subject track]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [assessment (store/assessment-of st subject track)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      track (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail (str subject "/" (name track) " の必要書類が充足していない状態での提案")}]))))

(defn- unified-qualification-missing-violations
  "For `:filing/submit` on EITHER track, when the engagement declares
  `:requires-unified-qualification? true`, INDEPENDENTLY verify
  `:unified-qualification-verified?` is true -- 全省庁統一資格 is a
  documented prerequisite to contract with MOD, not track-scoped.
  CONDITIONAL on the engagement's own `:requires-unified-
  qualification?` ground truth."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-unified-qualification? e))
                 (not (true? (:unified-qualification-verified? e))))
        [{:rule :unified-qualification-missing
          :detail (str subject " は全省庁統一資格(資格審査結果通知書)が未確認 -- 提出提案は進められない")}]))))

(defn- bid-suspended-violations
  "For `:filing/submit` on EITHER track, UNCONDITIONALLY verify the
  engagement is not currently under 指名停止 (bid suspension). An
  operator under active suspension is not eligible to be awarded a new
  MOD contract during that period -- no `:requires-*` flag needed, this
  always applies."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when (true? (:bid-suspended? e))
        [{:rule :bid-suspended
          :detail (str subject " は指名停止中 -- 新規契約の相手方となる資格を欠くため提出提案は進められない")}]))))

(defn- aptitude-assessment-missing-violations
  "For `:filing/submit` on the `:specially-designated-secrets` track,
  when the engagement declares `:requires-aptitude-assessment? true`,
  INDEPENDENTLY verify `:aptitude-assessment-verified?` is true --
  personnel may not be assigned to Specially-Designated-Secrets duties
  before 適性評価 is complete. Genuine legal prerequisite, not a
  business-policy preference."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track :specially-designated-secrets))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-aptitude-assessment? e))
                 (not (true? (:aptitude-assessment-verified? e))))
        [{:rule :aptitude-assessment-missing
          :detail (str subject " は特定秘密取扱対象要員の適性評価が未完了 -- 提出提案は進められない")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate (+ optional
  export-fee)."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- already-drafted-violations
  "For `:filing/draft`, refuses to draft the SAME engagement/track
  twice."
  [{:keys [op subject track]} st]
  (when (= op :filing/draft)
    (when (store/engagement-track-drafted? st subject track)
      [{:rule :already-drafted
        :detail (str subject "/" (name track) " は既にドラフト済み")}])))

(defn- already-submitted-violations
  "For `:filing/submit`, refuses to submit the SAME engagement/track
  twice."
  [{:keys [op subject track]} st]
  (when (= op :filing/submit)
    (when (store/engagement-track-submitted? st subject track)
      [{:rule :already-submitted
        :detail (str subject "/" (name track) " は既に提出済み")}])))

(defn check
  "Censors a DefenseCompliance-LLM proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c
  :escalate? bool :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (unified-qualification-missing-violations request st)
                           (bid-suspended-violations request st)
                           (aptitude-assessment-missing-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :track      (:track request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
