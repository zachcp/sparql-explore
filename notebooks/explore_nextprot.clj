^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-nextprot
  (:require [sparql-explore.query :as sq]
            [nextjournal.clerk :as clerk]))


^{::clerk/visibility :hide
  ::clerk/viewer clerk/hide-result}
(comment

  ;; This is how we start Clerk and tell it to serve this notebook at
  ;; `localhost:7777`. Assuming you've started your repl with the
  ;; `:dev` alias, evaluate these two forms, point your browser there,
  ;; then read on. :)
  (clerk/serve! {:watch-paths ["notebooks"]})
  (clerk/show! "notebooks/explore_nextprot.clj"))
;;   (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"
;;                                     "notebooks/explore_pubchem.clj"
;;                                     "notebooks/explore_chembl.clj"
;;                                     "notebooks/explore_metanetx.clj"]})
  




;; # Introduction
;; 

;; NextProt
;; - [Sparql](https://api.nextprot.org/sparql)
;;
;; 

;; # NextProt Sparql Examples
;; 
;; ## NXQ1 
;; Phospho-proteins in the cytoplasm

(sq/query
 :nextprot
 `{:select-distinct [?entry]
   :where [
          ;;  SL and GO values for cytoplasm
           [:values 
            {?cytoloc 
             [:np-terminology/SL-0086 :np-terminology/GO_0005737]}] 
           [?entry :np/isoform ?iso]
           ;;  phophorylation
           [?iso  (cat :np/keyword :np/term) :np-terminology/KW-0597] ;phosphorylated
           [?iso :np/cellularComponent ?loc]
           [?loc (cat :np/term :np/childOf)  ?cytoloc]
           ;No negative localization evidence
           [:filter (not (exists [[?loc :np/negativeEvidence ?negev]]))]]
   :limit 10})


;; ## NXQ2 
;; Protens in the nucleus AND the cytoplasm

(sq/query
 :nextprot
 `{:select-distinct [?entry]
   :where [ [:values
             {?cytoloc [:np-terminology/SL-0086 :np-terminology/GO_0005737]
              ?nucloc [:np-terminology/SL-0191 :np-terminology/GO_0005634]}]
           [?entry :np/isoform ?iso]
           [?iso  (cat :np/keyword :np/term) :np-terminology/KW-0597] ;phosphorylated
           [?iso :np/cellularComponent ?loc1]
           [?iso :np/cellularComponent ?loc2]
           [?loc1 (cat :np/term :np/childOf)  ?cytoloc]
           [?loc2 (cat :np/term :np/childOf)  ?nucloc]
           ;No negative localization evidence
           [:filter (not (exists [[?loc1 :np/negativeEvidence ?negev]]))]
           [:filter (not (exists [[?loc2 :np/negativeEvidence ?negev]]))]] 
   :limit 10})



;; ## NXQ3
;; 7-Transmembrane proteins

(sq/query
 :nextprot
 `{:select-distinct [?entry]
   :where [[?entry :np/isoform ?iso]
           [?iso :np/topology ?statement]
           [?statement :a :np/TransmembraneRegion]]
   :group-by [?entry ?iso]
   :having [(= 7 (count ?statement))]
   :limit 10})


;; ## NXQ4
;; Brain protien swith high IHC level but not expressed
;; in the testis

(sq/query
 :nextprot
 `{:select-distinct [?entry]
   :where [[?entry :np/isoform ?iso]
           [?iso :np/expression ?e1]
           ;:evidence/:expressionLevel :High.
           [?e1 (cat :np/term :np/childOf) :np-terminology/TS-0095]
           ; not expressed in testis
           [?iso :np/undetectedExpression ?e2]
           [?e2  :np/term :np-terminology/TS-1030]
           [:filter 
            (not (exists [[?iso (cat :np/detectedExpression :np/term :np/childOf) :np-terminology/TS-1030]]))]
           ]
   :limit 10})



;; ## NXQ5
;; Mitochondrial proteins without a transit peptide


(sq/query
 :nextprot
 `{:select-distinct [?entry]
   :where [
           ;SL and GO values for mitochondrion
           [:values {?mitoloc [:np-terminology/SL-0173 :np-terminology/GO_0005739]}]
           [?entry :np/isoform ?iso]
           [?iso :np/expression ?e1]
           ;:evidence/:expressionLevel :High.
           [?e1 (cat :np/term :np/childOf) :np-terminology/TS-0095]
           ; not expressed in testis
           [?iso :np/cellularComponent ?loc]
           [?loc (cat :np/term :np/childOf) ?mitoloc]
           [?e2  :np/term :np-terminology/TS-1030]
           ; No negative localization evidence}
           [:filter (not (exists [[?loc :np/negativeEvidence ?negev]]))]]
   :limit 10})

