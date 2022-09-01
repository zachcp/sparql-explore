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
  (clerk/show! "notebooks/explore_chembl.clj")
  (clerk/build-static-app! {:paths [
                                    "notebooks/explore.clj"
                                    "notebooks/explore_pubchem.clj"
                                    "notebooks/explore_chembl.clj"
                                    ]})
  )




;; # Introduction
;; 

;; Chembl mirror hosted on Bigcat
;; https://chemblmirror.rdf.bigcat-bioinformatics.org/sparql
;;
;; Initial impressions are very clean syntax. 
;; I encountered only one issue: the use of the `void` namespace - will 
;; need to look that up.


;; # Chembl Examples
;; 
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
   

;; ## SMILES
;;
^{::clerk/viewer clerk/table}
(sq/query-chembl
 `{:select [?identifier ?smiles ?image ]
   :where [; SIO_000008     has-attribute
           ; CHEMINF_000018 SMILES descriptor
           ; SIO_000300     has value    
           [?s a :chembl/SmallMolecule]
           [?s :chembl/chemblId ?identifier]
           [?s :foaf/depiction  ?image]
           [?s :cheminf/SIO_000008 _b1]
             [_b1 a :cheminf/CHEMINF_000018]
             [_b1 :cheminf/SIO_000300 ?smiles]
           ]
   :limit 10})


;; ## Metadata: Terms
;; 
;; Todo: handle VOID. Not sure what that is.
^{::clerk/viewer clerk/table}
(sq/query-chembl
 `{:prefixes {:pav     "<http://purl.org/pav/>"
              :dcterms "<http://purl.org/dc/terms/>"}
   :select-distinct [ [(str ?titleLit) ?title] ?date ?license ?dataset]
   :where [
           ;[?dataset a void:Dataset") ]
           [?dataset :dcterms/title   ?titleLit]
           [?dataset :dcterms/license ?license]
           [?dataset :pav/createdOn   ?date]]
   :limit 5})


;; ## Triples Count
(sq/query-chembl 
 `{:select [[(count *) ?count]]
   :where [[?s ?p ?o]]
   :limit 10})