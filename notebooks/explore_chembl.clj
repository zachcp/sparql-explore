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

;; ## Binding Affinity
^{::clerk/viewer clerk/table}
(sq/query-chembl 
 `{:select-distinct [?targetLabel ?molLabel ?assayLabel ?type ?value]
   :where [
           [?assay    :chembl/hasTarget     ?target]
           [?activity :chembl/hasAssay      ?assay]
           [?activity :chembl/hasMolecule   ?molecule]
           [?activity :chembl/type          ?type] 
           [?activity :chembl/standardValue ?value]
           ; get labels
           [?target   :rdfs/label ?targetLabel]
           [?molecule :rdfs/label ?molLabel]
           [?assay    :rdfs/label ?assayLabel]
           [?molecule :rdfs/label ?molLabel]] 
   
   :limit 10})
   

