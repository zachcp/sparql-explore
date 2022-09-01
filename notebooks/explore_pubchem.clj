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

;; ## E1: 
;; What protein targets does donepezil (CHEBI_53289) 
;; inhibit with an IC50 less than 10 µM?

(sq/query-pubchem
   `{:select-distinct [?protein]
     :where [; :obo/RO_0000056 = mg
           ; :obo/RO_0000057 = ?
           ; :obo/CHEBI_53289 = donepezil
           ; :obo/CHEBI_53289 = donepezil
             [?sub :a             :obo/CHEBI_53289]
             [?sub :obo/RO_0000056 ?mg]
             [?mg  :obo/RO_0000057 ?protein]
             [?mg  :obo/OBI_0000299 ?ep]
             [?protein :a :bp/Protein]
             [?ep :a               :bao/BAO_0000190]
             [?ep :obo/IAO_0000136 ?sub]
             [?ep :sio/has-value   ?value]
             [:filter (< ?value 10)]]})

