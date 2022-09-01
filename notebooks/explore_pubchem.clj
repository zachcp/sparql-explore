^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-pubchem
  (:require [sparql-explore.query :as sq]
            [mundaneum.query :as md]
            [com.yetanalytics.flint :as f]
            [nextjournal.clerk :as clerk]
            [tick.core :as tick]
            [nextjournal.clerk.viewer :as v]))


^{::clerk/visibility :hide
  ::clerk/viewer clerk/hide-result}
(comment

  ;; This is how we start Clerk and tell it to serve this notebook at
  ;; `localhost:7777`. Assuming you've started your repl with the
  ;; `:dev` alias, evaluate these two forms, point your browser there,
  ;; then read on. :)
  (clerk/serve! {:watch-paths ["notebooks"]})
  (clerk/show! "notebooks/explore_pubchem.clj")
  ;(clerk/build-static-app! {:paths ["notebooks/explore.clj"]})
  )



;; # Introduction
;; 
;; This notebook is an exploration of the UNIPROT SPARQL interface by way
;; of copying/translating the [SPARQL examples](https://sparql.uniprot.org/.well-known/sparql-examples/)
;; into [flint](https://github.com/yetanalytics/flint),  really nice clojure SPARQL DSL. I 
;; also borrowed a number of functions from [Mundaneum](https://github.com/jackrusher/mundaneum), Jack Rusher's 
;; excellent SPARQL DSL for wikidata.

;; ## Check the Lights
;; (sq/query-pubchem  `{:select * :where [[?s ?p ?o]] :limit 10})


;; # Pubchem Examples

;; ## E1: Donepezil Target
;; What protein targets does donepezil (CHEBI_53289) 
;; inhibit with an IC50 less than 10 µM?
;; 
;;  Inline writing explains what is going on. The main
;;  difficulty is knowing the various relationships between
;; everything. The english-version is not too bad....
^{::clerk/viewer clerk/table}
(sq/query-pubchem
   `{:select-distinct [?protname ?assay ?epname ?value ?sub]
     :where [; :obo/CHEBI_53289 = donepezil
             ; :obo/RO_0000056  = participates-in
             ; :obo/RO_0000057  = has-participant
             ; :obo/OBI_0000299 = has-specified-output
             ; :obo/BAO_0000190 = IC50
             ; :obo/IAO_0000136 = has-topic 
             [?sub :a              :obo/CHEBI_53289] ;  donepezil
             [?sub :obo/RO_0000056  ?mg]             ;  don participates in mg
             [?mg  :obo/RO_0000057  ?protein]        ;  mg  has-participant prot 
             [?protein :a :bp/Protein]               ; prot is a Protein
             [?mg  :obo/OBI_0000299 ?ep]             ; mg has a specified output
             [?ep :a  :bao/BAO_0000190]              ; output is IC50
             [?ep :obo/IAO_0000136 ?sub]             ; the has-topic of don
             [?ep :sio/has-value   ?value]           ; the IC 50 calue
             [:filter (< ?value 10)]
             ; added these for clarity
             [?mg       :dcterms/title ?assay]
             [?protein  :dcterms/title ?protname]
             [?ep       :rdfs/label ?epname]
             ]
     :limit 10})



;; ## E2: Pharmaco-roles
;; What pharmacological roles of SID46505803 are defined by ChEBI
;;  Note the use of the anonymous  blanknode, `_b1` to define boundaries
^{::clerk/viewer clerk/table}
(sq/query-pubchem
  `{:select-distinct [?name ?rolename]
    :where [
            ; :sio/CHEMINF_000477 = has PubChem normalized counterpar
            [:bind [:substance/SID46505803 ?sub]]
            [?sub :sio/CHEMINF_000477 ?comp]
            [?comp a ?chebi] 
            [?chebi :rdfs/subClassOf _b1]
            [_b1 a :owl/Restriction]
            [_b1 :owl/onProperty :obo/RO_0000087]
            [_b1 :owl/someValuesFrom ?role]

            ; make the names easier
            [?chebi :rdfs/label ?name]
            [?role :rdfs/label ?rolename]
            ]
    })


