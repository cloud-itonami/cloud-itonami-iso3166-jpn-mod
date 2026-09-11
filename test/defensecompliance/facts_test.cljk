(ns defensecompliance.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [defensecompliance.facts :as facts]))

(deftest three-principles-has-spec-basis
  (let [sb (facts/spec-basis :three-principles)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= "2014-04-01" (:established sb)))))

(deftest specially-designated-secrets-has-spec-basis
  (let [sb (facts/spec-basis :specially-designated-secrets)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (true? (:aptitude-assessment-is-prerequisite? sb)))
    (is (= "2014-12-10" (:effective sb)))))

(deftest procurement-qualification-has-spec-basis
  (let [sb (facts/spec-basis :procurement-qualification)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (true? (:unified-qualification-required? sb)))))

(deftest unknown-track-has-no-spec-basis
  (is (nil? (facts/spec-basis :unknown-track)))
  (is (nil? (facts/spec-basis :zzz))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis :three-principles)
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? :three-principles all)))
    (is (not (facts/required-evidence-satisfied? :three-principles (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? :unknown-track all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage [:three-principles :specially-designated-secrets :unknown-track])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["unknown-track"] (:missing-tracks c)))))

(deftest aptitude-assessment-prerequisite-track-is-sds-only
  (is (true? (facts/aptitude-assessment-prerequisite-track? :specially-designated-secrets)))
  (is (false? (facts/aptitude-assessment-prerequisite-track? :three-principles)))
  (is (false? (facts/aptitude-assessment-prerequisite-track? :procurement-qualification))))
