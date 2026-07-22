(ns defensecompliance.defensecompliancellm
  "DefenseCompliance-LLM client -- the *contained intelligence node* for
  the JPN-MOD (Ministry of Defense) compliance actor.

  It normalizes engagement intake, drafts a per-track (`:three-
  principles` / `:specially-designated-secrets`) compliance evidence
  checklist, drafts the filing-draft action, and drafts the
  filing-submit action. CRITICAL: it is a smart-but-untrusted advisor.
  It returns a *proposal* (with a rationale + the fields it cited),
  never a committed record or a real MOD filing. Every output is
  censored downstream by `defensecompliance.governor` before anything
  touches the SSoT, and `:filing/draft`/`:filing/submit` proposals
  NEVER auto-commit at any phase -- see README Actuation. This actor
  stays strictly at the compliance-PROCESS level -- it never reasons
  about or surfaces classified/operational content, only paperwork /
  eligibility / clearance-status facts.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end."
  (:require [defensecompliance.facts :as facts]
            [defensecompliance.store :as store]))

(defn- normalize-intake
  [_db {:keys [patch]}]
  {:summary    (str "engagement intake record updated: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :engagement/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-track
  "Per-track (`:three-principles` / `:specially-designated-secrets`)
  compliance evidence checklist draft. `:no-spec?` injects the failure
  mode we must defend against: proposing a checklist for a track with
  NO official spec-basis."
  [_db {:keys [track no-spec?]}]
  (let [track (if no-spec? :unknown-track track)
        sb (facts/spec-basis track)]
    (if (nil? sb)
      {:summary    (str (name track) " の公式spec-basisが見つかりません")
       :rationale  "defensecompliance.facts に未登録のトラック。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:track track :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str (name track) " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-draft
  "Draft the actual FILING-DRAFT action for `track`. ALWAYS `:stake
  :actuation/draft-filing`."
  [db {:keys [subject track]}]
  (let [e (store/engagement db subject)]
    {:summary    (str subject "/" (name track) " 向け提出ドラフト提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "track=" (name track) " portal=" (:portal e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-drafted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/draft-filing
     :confidence (if e 0.9 0.3)}))

(defn- propose-submit
  "Draft the actual FILING-SUBMIT action for `track`. ALWAYS `:stake
  :actuation/submit-filing` -- real-world MOD filing submission.
  Reflects readiness across ALL three engagement-level/track gates the
  governor independently re-verifies: 全省庁統一資格, 指名停止, and (for
  `:specially-designated-secrets`) 適性評価."
  [db {:keys [subject track]}]
  (let [e (store/engagement db subject)
        unified-qualification-ok? (or (not (:requires-unified-qualification? e))
                                       (:unified-qualification-verified? e))
        not-suspended? (not (:bid-suspended? e))
        aptitude-assessment-ok? (or (not= track :specially-designated-secrets)
                                     (not (:requires-aptitude-assessment? e))
                                     (:aptitude-assessment-verified? e))]
    {:summary    (str subject "/" (name track) " 向け提出提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "unified-qualification-verified?=" (:unified-qualification-verified? e)
                        " bid-suspended?=" (:bid-suspended? e)
                        " aptitude-assessment-verified?=" (:aptitude-assessment-verified? e)
                        " claimed-fee=" (:claimed-fee e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-submitted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/submit-filing
     :confidence (if (and e unified-qualification-ok? not-suspended? aptitude-assessment-ok?)
                   0.9 0.3)}))

(defprotocol Advisor
  (-advise [this db request] "Return a proposal map for `request`."))

(defrecord MockAdvisor []
  Advisor
  (-advise [_ db {:keys [op] :as request}]
    (case op
      :engagement/intake   (normalize-intake db request)
      :compliance/assess   (assess-track db request)
      :filing/draft        (propose-draft db request)
      :filing/submit       (propose-submit db request)
      {:summary "unknown op" :rationale "unsupported" :cites []
       :effect :noop :value {} :stake nil :confidence 0.0})))

(defn mock-advisor [] (->MockAdvisor))

(defn trace [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :track (:track request)
   :summary (:summary proposal)
   :confidence (:confidence proposal)
   :stake (:stake proposal)})
