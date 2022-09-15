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
(sq/query-metanetx 
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

(sq/query-metanetx
 `{:select [?metabolite ?xref]
   :where [[?metabolite :rdfs/comment "N-nitrosomethanamine"]
           [?metabolite :mnx/chemXref ?xref]]})


;; ## KEGG C01732
;;
;; For the KEGG compound C01732, retrieve the
;; MNXref identifier, name and reference

(sq/query-metanetx
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

(sq/query-metanetx
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

(sq/query-metanetx
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

(sq/query-metanetx
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
           [:filter (in ?side :mnx/left :mnx/right)]
           [:bind   [(if (= ?side :mnx/left) (- 0 ?c) ?c) ?coef]]]
   :limit 10})

;; ## Tartrate Succinate Antiporter
;; https://rdf.metanetx.org/
;; Show the reaction equation for the tartrate/succinate 
;;  antiporter (*rhea:34763*). NB: there are two generic 
;;  compartments here. 

(sq/query-metanetx
 `{:prefixes {:keggR "<https://identifiers.org/kegg.reaction:>"
              :rhea "<http://rdf.rhea-db.org/>"}
   :select [?chem ?chem_name ?comp ?comp_name ?coef]
   :where [[?reac :mnx/reacXref ~(keyword "rhea/34763")]
           [?reac ?side ?part]
           [?part :mnx/chem ?chem]
           [?part :mnx/comp ?comp]
           [?part :mnx/coef ?c]
           [?chem :rdfs/comment ?chem_name]
           [?comp :rdfs/comment ?comp_name]
           [:filter (in ?side :mnx/left :mnx/right)]
           [:bind   [(if (= ?side :mnx/left) (- 0 ?c) ?c) ?coef]]]})

;; ## ATP synthase & BiGG 
;;  Show the reaction equation for ATP synthase (reaction 
;;  *ATPS4m* from BiGG). NB: there are two types of protons 
;;  here, as MetaNetX distinguishes protons used for balancing 
;;  (MNXM1) from those that are translocated (MNXM01). 

(sq/query-metanetx
 `{:prefixes {:biggR "<https://identifiers.org/bigg.reaction:>"}
   :select [?chem ?chem_name ?comp ?comp_name ?coef]
   :where [[?reac :mnx/reacXref :biggR/ATPS4m]
           [?reac ?side ?part]
           [?part :mnx/chem ?chem]
           [?part :mnx/comp ?comp]
           [?part :mnx/coef ?c]
           [?chem :rdfs/comment ?chem_name]
           [?comp :rdfs/comment ?comp_name]
           [:filter (in ?side :mnx/left :mnx/right)]
           [:bind   [(if (= ?side :mnx/left) (- 0 ?c) ?c) ?coef]]]})



;; ## GEMs in MetaNetX
;;  List all GEMs currently in the MetaNetX repository, with 
;;  their numbers of reactions, chemical, compartments and 
;;  genes/proteins. 

(sq/query-metanetx
 `{:select [?mnet 
            ?taxon
            [(count ?reac :distinct? true)  ?count_reac]
            [(count ?chem :distinct? true)  ?count_chem]
            [(count ?comp :distinct? true)  ?count_comp]
            [(count ?pept :distinct? true)  ?count_pept]]
   :where [[?mnet a :mnx/MNET]
           [?mnet :mnx/gpr ?gpr]
           [?gpr  :mnx/reac ?reac]
           [?reac (alt  :mnx/left :mnx/right) ?part]
           [?part :mnx/chem ?chem]
           [?part :mnx/comp ?comp]
           [?gpr (alt :mnx/cata :mnx/pept) ?pept]
           [:optional [[?mnet :mnx/taxid ?taxon]]]]})


;; ## GEMs Reaction in E.Coli
;;  A GEM is primarily a set of reactions: here are all the 
;;  reaction equations occurring in *bigg_e_coli_core*. NB: here 
;;  the reac label is the one produced while compiling MetaNetX 

(sq/query-metanetx
 `{:select [?reac_label ?chem_name ?comp_name ?coef]
   :where [[?mnet :rdfs/label "bigg_e_coli_core"]
           [?mnet (cat :mnx/gpr :mnx/reac) ?reac]
           [?reac :rdfs/label ?reac_label]
           [?reac ?side ?part]
           [?part :mnx/chem ?chem]
           [?part :mnx/comp ?comp]
           [?part :mnx/coef ?c]
           [?chem :rdfs/comment ?chem_name]
           [?comp :rdfs/comment ?comp_name]
           [:filter (in ?side :mnx/left :mnx/right)]
           [:bind   [(if (= ?side :mnx/left) (- 0 ?c) ?c) ?coef]]]
   :order-by [?reac_label]})



;; ## Additional Reaction Properties
;;  ...in addition reactions are endowed with a direction, flux 
;;  bounds and possibly the description of the enzymes that 
;;  catalyze it. 
(sq/query-metanetx
 `{:select [?reac_orig_label ?reac_mnx_label  ?lb ?ub ?dir ?cata_orig 
            [(group-concat ?cplx_label :separator " OR ") ?cplx_info]
            ]
   :where [[?mnet :rdfs/label "bigg_e_coli_core"]
           [?mnet :mnx/gpr ?gpr]
           [?gpr   :rdfs/label ?reac_orig_label]
           [?gpr   :rdfs/comment ?cata_orig]
           [?gpr   :mnx/reac ?reac]
           [?gpr   :mnx/cata ?cata]
           [?reac :rdfs/label ?reac_mnx_label]
           [?cata :mnx/lb ?lb]
           [?cata :mnx/ub ?ub]
           [?cata :mnx/dir ?dir]
           [?cata :mnx/cplx ?cplx]
           [?cplx :rdfs/label ?cplx_label]] 
   :group-by [?reac_orig_label ?reac_mnx_label ?lb ?ub ?dir ?cata_orig]
   :order-by [?reac_orig_label]
   :limit 10})

