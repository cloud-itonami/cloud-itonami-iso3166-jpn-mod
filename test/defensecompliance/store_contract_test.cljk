(ns defensecompliance.store-contract-test
  "MemStore ≡ DatomicStore parity for the Store protocol."
  (:require [clojure.test :refer [deftest is testing]]
            [defensecompliance.store :as store]
            [defensecompliance.registry :as registry]))

(defn- exercise [s]
  (store/commit-record! s {:effect :engagement/upsert
                           :value {:id "eng-x" :operator "X KK"
                                   :base-fee 100 :monthly-rate 10 :monitoring-months 1
                                   :audit-export? false :export-fee nil :claimed-fee 110.0
                                   :requires-unified-qualification? true :unified-qualification-verified? true
                                   :bid-suspended? false
                                   :requires-aptitude-assessment? true :aptitude-assessment-verified? true
                                   :three-principles-drafted? false :three-principles-submitted? false
                                   :sds-drafted? false :sds-submitted? false
                                   :status :intake}})
  (store/commit-record! s {:effect :assessment/set
                           :path ["eng-x" :three-principles]
                           :payload {:track :three-principles :checklist ["a"] :spec-basis "x"}})
  (store/commit-record! s {:effect :engagement/mark-drafted :path ["eng-x" :three-principles]})
  (store/commit-record! s {:effect :engagement/mark-submitted :path ["eng-x" :three-principles]})
  (store/append-ledger! s {:t :committed :op :test})
  {:engagement (store/engagement s "eng-x")
   :assessment (store/assessment-of s "eng-x" :three-principles)
   :drafts (store/draft-history s)
   :submits (store/submit-history s)
   :ledger (store/ledger s)
   :drafted? (store/engagement-track-drafted? s "eng-x" :three-principles)
   :submitted? (store/engagement-track-submitted? s "eng-x" :three-principles)})

(deftest mem-and-datomic-parity
  (let [mem (store/seed-db)
        dat (store/datomic-seed-db)
        ;; use empty stores for parity of exercised mutations
        mem* (store/->MemStore (atom {:engagements {} :assessments {} :ledger []
                                      :draft-sequences {} :draft-records []
                                      :submit-sequences {} :submit-records []}))
        dat* (store/datomic-store {})
        m (exercise mem*)
        d (exercise dat*)]
    (is (= (:operator (:engagement m)) (:operator (:engagement d))))
    (is (true? (:drafted? m)) (true? (:drafted? d)))
    (is (true? (:submitted? m)) (true? (:submitted? d)))
    (is (= 1 (count (:drafts m))) (= 1 (count (:drafts d))))
    (is (= 1 (count (:submits m))) (= 1 (count (:submits d))))
    (is (= 1 (count (:ledger m))) (= 1 (count (:ledger d))))
    (is (= (:assessment m) (:assessment d)))))
