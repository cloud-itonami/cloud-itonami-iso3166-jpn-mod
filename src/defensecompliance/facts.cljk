(ns defensecompliance.facts
  "Japan Ministry of Defense (防衛省, MOD) defense-equipment-transfer /
  specially-designated-secrets / procurement-qualification compliance
  catalog -- the ONLY source of regulatory-requirement facts this actor
  is allowed to cite (`defensecompliance.governor`'s spec-basis check
  enforces that every proposal touching `:compliance/assess`,
  `:filing/draft`, or `:filing/submit` cites this catalog and nothing
  invented).

  Every fact below was verified via web search against `mod.go.jp`,
  `mofa.go.jp`, `cas.go.jp`, and `meti.go.jp` government domains, plus
  Japan's official law-translation database
  (`japaneselawtranslation.go.jp`), during this repo's research pass
  (2026-07-22/23). Three tracks, each with its own owner authority and
  legal basis -- do NOT merge them into one undifferentiated 'MOD
  requirement':

    :three-principles              -- 防衛装備移転三原則 (Three
                                       Principles on Transfer of Defense
                                       Equipment and Technology).
                                       Established 2014-04-01 by a
                                       National Security Council
                                       decision, jointly overseen by the
                                       Cabinet Secretariat (内閣官房) and
                                       the Ministry of Foreign Affairs
                                       (外務省), with METI (経済産業省)
                                       publishing operational-guidance
                                       (運用指針) amendments.
    :specially-designated-secrets  -- 特定秘密の保護に関する法律 (Act on
                                       the Protection of Specially
                                       Designated Secrets, Act No. 108 of
                                       2013), plus the MOD-specific
                                       装備品等秘密 (equipment-related
                                       secrets) designation layer.
    :procurement-qualification     -- 全省庁統一資格 (Unified
                                       Qualification for Competitive
                                       Participation across All
                                       Ministries and Agencies, per
                                       予算決算及び会計令 Articles 70/71)
                                       and MOD's own 指名停止
                                       (suspension-from-designated-
                                       bidder-status) mechanism.

  What this catalog deliberately does NOT claim (see README/dossier --
  this is an especially security-sensitive domain, kept strictly at the
  compliance-PROCESS level):
    - no specific classification level, document-marking format, or any
      operational/classified detail of any kind (none was verified, and
      inventing any in a public OSS repo would be inappropriate
      regardless of accuracy);
    - no specific penalty amounts or prison terms for Specially
      Designated Secrets leaks (the Act establishes penalties; this
      catalog does not enumerate them);
    - no numeric threshold for when Three-Principles review applies
      (none was verified);
    - no e-procurement portal beyond the MOD general-competitive-
      bidding page cited below.")

(def catalog
  {:three-principles
   {:name "防衛装備移転三原則 -- 防衛装備品・技術の海外移転に関する原則"
    :name-en "Three Principles on Transfer of Defense Equipment and Technology"
    :owner-authority "内閣官房 / 外務省 (Cabinet Secretariat + Ministry of Foreign Affairs, jointly), 運用指針は経済産業省が公表"
    :legal-basis
    "国家安全保障会議決定 (平成26年4月1日, National Security Council decision, 2014-04-01) -- 旧武器輸出三原則を置き換え"
    :established "2014-04-01"
    :official-portal "https://www.mofa.go.jp/mofaj/fp/nsp/page1w_000097.html"
    :provenance "https://www.mofa.go.jp/mofaj/fp/nsp/page1w_000097.html"
    :provenance-secondary
    ["https://www.cas.go.jp/jp/gaiyou/jimu/bouei.html"
     "https://www.mod.go.jp/j/press/news/2026/04/21a.html"]
    :process-description
    "政府は移転案件について「厳格審査」(strict review/screening) と、移転後の「適正管理の確保」(ensuring proper management) を維持するとしている -- 具体的な審査基準の数値・閾値は本カタログでは扱わない (未検証)。"
    :required-evidence
    ["防衛装備移転三原則に基づく厳格審査(strict review/screening)対応記録"
     "移転後の適正管理の確保(proper management assurance)に関する体制確認記録"
     "経済産業省公表の運用指針(直近改正を含む)参照・整合確認記録"]}

   :specially-designated-secrets
   {:name "特定秘密の保護に関する法律 -- 特定秘密取扱者の適性評価 + MOD固有の装備品等秘密"
    :name-en "Act on the Protection of Specially Designated Secrets (Act No. 108 of 2013) -- aptitude assessment + MOD-specific equipment-related-secrets layer"
    :owner-authority "内閣 (Act No. 108 of 2013, 政府全体) / 防衛省 (MOD固有の装備品等秘密指定)"
    :legal-basis "特定秘密の保護に関する法律 (平成25年法律第108号) -- 2013-12-06制定・2013-12-13公布・2014-12-10施行"
    :enacted "2013-12-06"
    :promulgated "2013-12-13"
    :effective "2014-12-10"
    :official-portal "https://www.japaneselawtranslation.go.jp/ja/laws/view/2543"
    :provenance "https://www.japaneselawtranslation.go.jp/ja/laws/view/2543"
    :aptitude-assessment-is-prerequisite? true
    :aptitude-assessment-description
    "特定秘密を取り扱う業務に従事させようとする者に対しては、その業務を行わせる前に適性評価 (aptitude assessment / security-clearance screening) を実施することを法が要求する -- 適性評価が未実施のまま特定秘密関連業務に従事させることはできない。"
    :mod-specific-layer
    "防衛大臣は、装備品又は自衛隊の施設に関する情報であってその漏えいが防衛に支障を与えるおそれがあるものを「装備品等秘密」として指定できる。装備品等秘密は特定秘密に該当するものを明示的に除く別区分であり、MODと装備品等の研究開発・調達・供給・管理に関する契約を締結した事業者に提供され得る。"
    :required-evidence
    ["特定秘密取扱業務に従事する対象要員の適性評価(aptitude assessment)完了記録"
     "装備品等秘密(特定秘密に該当しないMOD固有区分)を扱う場合のMOD契約(研究開発・調達・供給・管理関連)締結記録"]}

   :procurement-qualification
   {:name "全省庁統一資格 + 指名停止 -- MOD調達契約の一般的前提資格"
    :name-en "Unified Qualification for Competitive Participation across All Ministries and Agencies + Suspension from Designated-Bidder Status"
    :owner-authority "防衛省 (調達担当部局) / 予算決算及び会計令に基づく全省庁共通制度"
    :legal-basis "予算決算及び会計令 第70条・第71条 (全省庁統一資格) / 装備品等及び役務の調達に係る指名停止等の要領 (MOD公表基準, 指名停止)"
    :official-portal "https://www.mod.go.jp/j/budget/chotatsu/naikyoku/nyuusatu_seifu/index.html"
    :provenance "https://www.mod.go.jp/j/budget/chotatsu/naikyoku/nyuusatu_seifu/index.html"
    :unified-qualification-required? true
    :process-description
    "MODと契約するには一般に全省庁統一資格(資格審査結果通知書で確認)が必要。MODは自ら公表する基準(装備品等及び役務の調達に係る指名停止等の要領)に基づき指名停止を課すことができ、指名停止中の事業者は当該期間中、新規契約の相手方となる資格を失う。"
    :required-evidence
    ["全省庁統一資格(予算決算及び会計令70条・71条)の資格審査結果通知書"
     "指名停止(装備品等及び役務の調達に係る指名停止等の要領)非該当の確認記録"]}})

(def valid-tracks (set (keys catalog)))

(defn spec-basis [track] (get catalog track))

(defn coverage
  ([] (coverage (keys catalog)))
  ([tracks]
   (let [have (filter catalog tracks) missing (remove catalog tracks)]
     {:requested (count tracks) :covered (count have)
      :covered-tracks (vec (sort (map name have)))
      :missing-tracks (vec (sort (map name missing)))
      :note "R0 catalog seed -- Three Principles + Specially Designated Secrets + procurement-qualification, JPN-MOD agency scope"})))

(defn required-evidence-satisfied? [track submitted]
  (when-let [{:keys [required-evidence]} (spec-basis track)]
    (= (count required-evidence) (count (filter (set submitted) required-evidence)))))

(defn evidence-checklist [track] (:required-evidence (spec-basis track) []))

(defn aptitude-assessment-prerequisite-track?
  "Is `track` one whose spec-basis names 適性評価 (aptitude assessment)
  as a documented legal prerequisite before handling the relevant
  duties? (Only `:specially-designated-secrets` today.)"
  [track]
  (boolean (:aptitude-assessment-is-prerequisite? (spec-basis track))))
