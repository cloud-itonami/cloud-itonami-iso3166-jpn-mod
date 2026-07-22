(ns defensecompliance.store
  "SSoT for the JPN-MOD (Ministry of Defense) compliance actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior cloud-itonami actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store, using `langchain-store.core` for the
                        shared EDN-blob codec + event-log helpers
                        instead of a hand-rolled `enc`/`dec*`
                        (ADR-2607141600).

  Both implement the same protocol and pass the same contract
  (test/defensecompliance/store_contract_test.clj).

  The primary entity here is an `engagement` -- one operator's
  compliance engagement carrying BOTH filing tracks:

    :three-principles             -- 防衛装備移転三原則 (Three
                                      Principles on Transfer of Defense
                                      Equipment and Technology)
                                      classification
    :specially-designated-secrets -- 特定秘密取扱者の適性評価 +
                                      MOD固有の装備品等秘密

  plus two engagement-level (not per-track) gating facts grounded in
  `defensecompliance.facts`' `:procurement-qualification` entry:
  `:unified-qualification-verified?` (全省庁統一資格) and
  `:bid-suspended?` (指名停止).

  filing-draft and filing-submit actuation events apply per-TRACK to
  the SAME engagement record (draft first, submit later, independently
  for each track). Dedicated double-actuation-guard booleans per track
  (`:three-principles-drafted?`/`:three-principles-submitted?`/
  `:sds-drafted?`/`:sds-submitted?`, never a single `:status` value).

  The ledger stays append-only on every backend."
  (:require [defensecompliance.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (assessment-of [s engagement-id track] "committed track assessment, or nil")
  (ledger [s])
  (draft-history [s] "the append-only filing-draft history")
  (submit-history [s] "the append-only filing-submit history")
  (next-draft-sequence [s track])
  (next-submit-sequence [s track])
  (engagement-track-drafted? [s engagement-id track])
  (engagement-track-submitted? [s engagement-id track])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory"))

;; ----------------------- track-scoped field mapping -----------------------
;; No dynamic keyword construction -- each track's drafted?/submitted?/
;; draft-number/submit-number fields are explicit, named keys (mirrors
;; the rest of this fleet's explicit-boolean-field style). `:sds` field
;; prefix is shorthand for the `:specially-designated-secrets` track.

(def ^:private track-fields
  {:three-principles              {:drafted? :three-principles-drafted?   :draft-number :three-principles-draft-number
                                    :submitted? :three-principles-submitted? :submit-number :three-principles-submit-number}
   :specially-designated-secrets  {:drafted? :sds-drafted?   :draft-number :sds-draft-number
                                    :submitted? :sds-submitted? :submit-number :sds-submit-number}})

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering both actuation
  lifecycles (draft, submit) across both filing tracks, plus the
  governor's own dossier-grounded checks: a clean case (eng-1, includes
  the compliance-audit export package revenue line), an
  unregistered-track fabrication-defense case (eng-2), a fee-mismatch
  case (eng-3), a missing-aptitude-assessment (clearance) case (eng-4,
  `:specially-designated-secrets` track), a missing-unified-
  qualification case (eng-5), and a currently-suspended-operator case
  (eng-6, `指名停止`)."
  []
  {:engagements
   {"eng-1" {:id "eng-1" :operator "Kita Defense Systems KK" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? true :export-fee 150000 :claimed-fee 1550000.0
             :requires-unified-qualification? true :unified-qualification-verified? true
             :bid-suspended? false
             :requires-aptitude-assessment? true :aptitude-assessment-verified? true
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}
    "eng-2" {:id "eng-2" :operator "Atlantis Defense Partners LLC" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? true :export-fee 150000 :claimed-fee 1550000.0
             :requires-unified-qualification? true :unified-qualification-verified? true
             :bid-suspended? false
             :requires-aptitude-assessment? true :aptitude-assessment-verified? true
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}
    "eng-3" {:id "eng-3" :operator "Minami Defense Systems KK" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1800000.0
             :requires-unified-qualification? true :unified-qualification-verified? true
             :bid-suspended? false
             :requires-aptitude-assessment? true :aptitude-assessment-verified? true
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}
    "eng-4" {:id "eng-4" :operator "Higashi Defense Technologies KK" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :requires-unified-qualification? true :unified-qualification-verified? true
             :bid-suspended? false
             :requires-aptitude-assessment? true :aptitude-assessment-verified? false
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}
    "eng-5" {:id "eng-5" :operator "Nishi Defense Logistics KK" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :requires-unified-qualification? true :unified-qualification-verified? false
             :bid-suspended? false
             :requires-aptitude-assessment? true :aptitude-assessment-verified? true
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}
    "eng-6" {:id "eng-6" :operator "Chuo Defense Consulting KK" :portal "MOD procurement / unified-qualification"
             :base-fee 800000 :monthly-rate 50000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1400000.0
             :requires-unified-qualification? true :unified-qualification-verified? true
             :bid-suspended? true
             :requires-aptitude-assessment? true :aptitude-assessment-verified? true
             :three-principles-drafted? false :three-principles-submitted? false
             :sds-drafted? false :sds-submitted? false
             :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- draft-filing!
  [s engagement-id track]
  (let [seq-n (next-draft-sequence s track)
        result (registry/register-draft engagement-id track seq-n)
        {:keys [drafted? draft-number]} (get track-fields track)]
    {:result result
     :engagement-patch {drafted? true
                        draft-number (get result "draft_number")}}))

(defn- submit-filing!
  [s engagement-id track]
  (let [seq-n (next-submit-sequence s track)
        result (registry/register-submit engagement-id track seq-n)
        {:keys [submitted? submit-number]} (get track-fields track)]
    {:result result
     :engagement-patch {submitted? true
                        submit-number (get result "submit_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (assessment-of [_ engagement-id track] (get-in @a [:assessments engagement-id track]))
  (ledger [_] (:ledger @a))
  (draft-history [_] (:draft-records @a))
  (submit-history [_] (:submit-records @a))
  (next-draft-sequence [_ track] (get-in @a [:draft-sequences track] 0))
  (next-submit-sequence [_ track] (get-in @a [:submit-sequences track] 0))
  (engagement-track-drafted? [_ engagement-id track]
    (boolean (get-in @a [:engagements engagement-id (:drafted? (get track-fields track))])))
  (engagement-track-submitted? [_ engagement-id track]
    (boolean (get-in @a [:engagements engagement-id (:submitted? (get track-fields track))])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :assessment/set
      (let [[engagement-id track] path]
        (swap! a assoc-in [:assessments engagement-id track] payload))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (draft-filing! s engagement-id track)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:draft-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :draft-records registry/append result))))
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (submit-filing! s engagement-id track)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:submit-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :submit-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :draft-sequences {} :draft-records []
                           :submit-sequences {} :submit-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  {:engagement/id                   {:db/unique :db.unique/identity}
   :assessment/key                  {:db/unique :db.unique/identity}
   :ledger/seq                      {:db/unique :db.unique/identity}
   :draft-record/seq                {:db/unique :db.unique/identity}
   :submit-record/seq               {:db/unique :db.unique/identity}
   :draft-sequence/track            {:db/unique :db.unique/identity}
   :submit-sequence/track           {:db/unique :db.unique/identity}})

(defn- engagement->tx [{:keys [id operator portal base-fee monthly-rate monitoring-months
                               audit-export? export-fee claimed-fee
                               requires-unified-qualification? unified-qualification-verified?
                               bid-suspended?
                               requires-aptitude-assessment? aptitude-assessment-verified?
                               three-principles-drafted? three-principles-draft-number
                               three-principles-submitted? three-principles-submit-number
                               sds-drafted? sds-draft-number sds-submitted? sds-submit-number
                               status]}]
  (cond-> {:engagement/id id}
    operator                              (assoc :engagement/operator operator)
    portal                                (assoc :engagement/portal portal)
    base-fee                              (assoc :engagement/base-fee base-fee)
    monthly-rate                          (assoc :engagement/monthly-rate monthly-rate)
    monitoring-months                     (assoc :engagement/monitoring-months monitoring-months)
    (some? audit-export?)                 (assoc :engagement/audit-export? audit-export?)
    export-fee                            (assoc :engagement/export-fee export-fee)
    claimed-fee                           (assoc :engagement/claimed-fee claimed-fee)
    (some? requires-unified-qualification?) (assoc :engagement/requires-unified-qualification? requires-unified-qualification?)
    (some? unified-qualification-verified?) (assoc :engagement/unified-qualification-verified? unified-qualification-verified?)
    (some? bid-suspended?)                (assoc :engagement/bid-suspended? bid-suspended?)
    (some? requires-aptitude-assessment?) (assoc :engagement/requires-aptitude-assessment? requires-aptitude-assessment?)
    (some? aptitude-assessment-verified?) (assoc :engagement/aptitude-assessment-verified? aptitude-assessment-verified?)
    (some? three-principles-drafted?)     (assoc :engagement/three-principles-drafted? three-principles-drafted?)
    three-principles-draft-number         (assoc :engagement/three-principles-draft-number three-principles-draft-number)
    (some? three-principles-submitted?)   (assoc :engagement/three-principles-submitted? three-principles-submitted?)
    three-principles-submit-number        (assoc :engagement/three-principles-submit-number three-principles-submit-number)
    (some? sds-drafted?)                  (assoc :engagement/sds-drafted? sds-drafted?)
    sds-draft-number                      (assoc :engagement/sds-draft-number sds-draft-number)
    (some? sds-submitted?)                (assoc :engagement/sds-submitted? sds-submitted?)
    sds-submit-number                     (assoc :engagement/sds-submit-number sds-submit-number)
    status                                (assoc :engagement/status status)))

(def ^:private engagement-pull
  [:engagement/id :engagement/operator :engagement/portal :engagement/base-fee :engagement/monthly-rate
   :engagement/monitoring-months :engagement/audit-export? :engagement/export-fee :engagement/claimed-fee
   :engagement/requires-unified-qualification? :engagement/unified-qualification-verified?
   :engagement/bid-suspended?
   :engagement/requires-aptitude-assessment? :engagement/aptitude-assessment-verified?
   :engagement/three-principles-drafted? :engagement/three-principles-draft-number
   :engagement/three-principles-submitted? :engagement/three-principles-submit-number
   :engagement/sds-drafted? :engagement/sds-draft-number
   :engagement/sds-submitted? :engagement/sds-submit-number
   :engagement/status])

(defn- pull->engagement [m]
  (when (:engagement/id m)
    {:id (:engagement/id m) :operator (:engagement/operator m) :portal (:engagement/portal m)
     :base-fee (:engagement/base-fee m) :monthly-rate (:engagement/monthly-rate m)
     :monitoring-months (:engagement/monitoring-months m)
     :audit-export? (boolean (:engagement/audit-export? m)) :export-fee (:engagement/export-fee m)
     :claimed-fee (:engagement/claimed-fee m)
     :requires-unified-qualification? (boolean (:engagement/requires-unified-qualification? m))
     :unified-qualification-verified? (boolean (:engagement/unified-qualification-verified? m))
     :bid-suspended? (boolean (:engagement/bid-suspended? m))
     :requires-aptitude-assessment? (boolean (:engagement/requires-aptitude-assessment? m))
     :aptitude-assessment-verified? (boolean (:engagement/aptitude-assessment-verified? m))
     :three-principles-drafted? (boolean (:engagement/three-principles-drafted? m))
     :three-principles-draft-number (:engagement/three-principles-draft-number m)
     :three-principles-submitted? (boolean (:engagement/three-principles-submitted? m))
     :three-principles-submit-number (:engagement/three-principles-submit-number m)
     :sds-drafted? (boolean (:engagement/sds-drafted? m))
     :sds-draft-number (:engagement/sds-draft-number m)
     :sds-submitted? (boolean (:engagement/sds-submitted? m))
     :sds-submit-number (:engagement/sds-submit-number m)
     :status (:engagement/status m)}))

(defn- assessment-key [engagement-id track] (str engagement-id "::" (name track)))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id %])))
         (sort-by :id)))
  (assessment-of [_ engagement-id track]
    (ls/dec* (d/q '[:find ?p . :in $ ?k
                   :where [?a :assessment/key ?k] [?a :assessment/payload ?p]]
                 (d/db conn) (assessment-key engagement-id track))))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (draft-history [_] (ls/read-stream conn :draft-record/seq :draft-record/record))
  (submit-history [_] (ls/read-stream conn :submit-record/seq :submit-record/record))
  (next-draft-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :draft-sequence/track ?t] [?e :draft-sequence/next ?n]]
            (d/db conn) track)
        0))
  (next-submit-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :submit-sequence/track ?t] [?e :submit-sequence/next ?n]]
            (d/db conn) track)
        0))
  (engagement-track-drafted? [s engagement-id track]
    (boolean (get (engagement s engagement-id) (:drafted? (get track-fields track)))))
  (engagement-track-submitted? [s engagement-id track]
    (boolean (get (engagement s engagement-id) (:submitted? (get track-fields track)))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(engagement->tx value)])

      :assessment/set
      (let [[engagement-id track] path]
        (d/transact! conn [{:assessment/key (assessment-key engagement-id track)
                            :assessment/payload (ls/enc payload)}]))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (draft-filing! s engagement-id track)
            next-n (inc (next-draft-sequence s track))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:draft-sequence/track track :draft-sequence/next next-n}
                      {:draft-record/seq (count (draft-history s)) :draft-record/record (ls/enc (get result "record"))}])
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (submit-filing! s engagement-id track)
            next-n (inc (next-submit-sequence s track))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:submit-sequence/track track :submit-sequence/next next-n}
                      {:submit-record/seq (count (submit-history s)) :submit-record/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv engagement->tx (vals engagements)))) s))

(defn datomic-store
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  []
  (datomic-store (demo-data)))
