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
   :where [[?metabolite :a :mnx/CHEM]
           [?metabolite :rdfs/label ?label] 
           [?metabolite :rdfs/comment "N,N-dimethyl-beta-alanine"]
           [?metabolite :mnx/chemRefer ?reference]
           [:optional [[?metabolite :mnx/formula  ?formula ]]]
           [:optional [[?metabolite :mnx/charge   ?charge ]]]
           [:optional [[?metabolite :mnx/inchi    ?inchi]]]
           [:optional [[?metabolite :mnx/inchikey ?inchikey]]]
           [:optional [[?metabolite :mnx/smiles   ?smiles]]]]})

;; ## External Identifiers
;;
;; Retrieve the identifiers for *N,N-dimethyl-beta-alanine* in
;; external databases. This crosslinking of external
;; identifiers is the core of MNXref.

(query-metanetx
 `{:select [?metabolite ?xref]
   :where [[?metabolite :rdfs/comment "N-nitrosomethanamine"]
           [?metabolite :mnx/chemXref ?xref]]})


;; ## KEGG C01732
;;
;; For the KEGG compound C01732, retrieve the
;; MNXref identifier, name and reference

(query-metanetx
 `{:prefixes {:keggC "<https://identifiers.org/kegg.compound:>"}
   :select [?metabolite ?reference ?name]
   :where [[?metabolite :a :mnx/CHEM]
           [?metabolite :mnx/chemRefer ?reference]
           [?metabolite :rdfs/comment ?name]
           [?metabolite :mnx/chemXref :keggC/C01732]]})


;; ##  Reaction Identifier
;;
;; Retrieve the MNXref reaction identifier, 
;; that corresponds to the KEGG reaction R00703 
;; (lactate dehydrogenase)

(query-metanetx
 `{:prefixes {:keggR "<https://identifiers.org/kegg.reaction:>"}
   :select [?reaction ?reference]
   :where [[?reaction a :mnx/REAC]
           [?reaction :mnx/reacXref :keggR/R00703]
           [?reaction :mnx/reacRefer ?reference]]})


;; ##  External Reaction Identifier
;;
;; List the external identifiers that correspond 
;; to the KEGG reaction R00703 (lactate dehydrogenase).
;; This crosslinking of external identifiers is the core 
;; of MNXref.

(query-metanetx
 `{:prefixes {:keggR "<https://identifiers.org/kegg.reaction:>"}
   :select [?xref]
   :where [[?reaction a :mnx/REAC]
           [?reaction :mnx/reacXref :keggR/R00703]
           [?reaction :mnx/reacXref ?xref]]})



;; ##  Stoicheoetry
;;
;; Show the reaction equation catalyzed by lactate 
;; dehydrogenase (KEGG reaction R00703). NB: 
;; Stoichiometric coefficients for substrates are 
;; given a negative value

(query-metanetx
 `{:prefixes {:keggR "<https://identifiers.org/kegg.reaction:>"
              :rhea "<http://rdf.rhea-db.org/>"}
   :select [?chem ?chem_name ?comp ?comp_name ?coef]
   :where [[?reac :mnx/reacXref :keggR/R00703]
           [?reac ?side ?part]
           [?part :mnx/chem ?chem]
           [?part :mnx/comp ?comp]
           [?part :mnx/coef ?c]
           [?chem :rdfs/comment ?chem_name]
           [?comp :rdfs/comment ?comp_name]
           [?reaction :mnx/reacXref ?xref]
           [:filter (in ?side :mnx/left :mnx/right)]
           [:bind   [(if (= ?side :mnx/left) (- 0 ?c) ?c) ?coef]]
           ]
   :limit 10
   })





