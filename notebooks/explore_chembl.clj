^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-chembl
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
  (clerk/show! "notebooks/explore_chembl.clj"))
  ;(clerk/build-static-app! {:paths ["notebooks/explore.clj"]})




;; # Introduction
;; 

;; Chembl mirror hosted on Bigcat
;; https://chemblmirror.rdf.bigcat-bioinformatics.org/sparql

;; # Chembl Examples
;; 
;; ## Check
;; ```clj
;; (sq/query-chembl
;;  `{:select [[(count *) ?count]]
;;    :where [[?s ?p ?o]]})
;; ```


