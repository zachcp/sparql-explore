^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-metanetx
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
  (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"
                                    "notebooks/explore_pubchem.clj"
                                    "notebooks/explore_chembl.clj"]}))




;; # Introduction
;; 

;; MetanetX
;; [metanet-x rdf](https://www.metanetx.org/cgi-bin/mnxget/mnxref/MetaNetX_RDF_schema.pdf)
;;

;; # Metanex Examples
;; 
;; ## Basic Information
;; Retrieve the MNXref metabolite with name 
;;  *N,N-dimethyl-beta-alanine*, together with molecular 
;;  information. 
(query-metanetx 
 `{:select [?metabolite ?label ?reference ?formula ?charge ?inchi ?inchikey ?smiles]
   :where [
           [?metabolite :a :mnx/CHEM]
           [?metabolite :rdfs/label ?label] 
           [?metabolite :rdfs/comment "N,N-dimethyl-beta-alanine"]
           [?metabolite :mnx/chemRefer ?reference]
           [:optional [[?metabolite :mnx/formula  ?formula ]]]
           [:optional [[?metabolite :mnx/charge   ?charge ]]]
           [:optional [[?metabolite :mnx/inchi    ?inchi]]]
           [:optional [[?metabolite :mnx/inchikey ?inchikey]]]
           [:optional [[?metabolite :mnx/smiles   ?smiles]]]
           ]
   :limit 10
   })

