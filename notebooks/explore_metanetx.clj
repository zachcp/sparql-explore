^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-metanetx
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
  (clerk/show! "notebooks/explore_chembl.clj")
  (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"
                                    "notebooks/explore_pubchem.clj"
                                    "notebooks/explore_chembl.clj"
                                    "notebooks/explore_metanetx.clj"]}))




;; # Introduction
;; 

;; MetanetX
;; [metanet-x rdf](https://www.metanetx.org/cgi-bin/mnxget/mnxref/MetaNetX_RDF_schema.pdf)
;;
;; 
;; [ From the [tutorial](https://www.metanetx.org/mnxdoc/short-tutorial.html)] MetaNetX.org treats each model (i.e., metabolic network or pathway) as 
;; an being constituted of different entities:
;; 
;;     - chemical compounds (chem)
;;     - subcellular compartments (comp)
;;     - species (spec): chemical compounds that are assigned to a subcellular compartment
;;     - metabolic reactions (reac): reactions that transform species into another
;;     - genes or peptides (pept): the two are currently not distinguished, which make more
;;     - "enzymes" (enzy): sets of peptides (or genes) linked to a reaction with information on bounds
;; (maximal and minimal bounds) defining the directionality such that 
;; the maximum flux can be carried by this reaction with these enzymes

;; Depending on the studied model (GEM or pathway), it may contain biomass production 
;; reaction(s) (identified by the identifier "BIOMASS" in the corresponding reaction equation),
;; or uptake or secretion reactions (external/boundary reactions; identified by chemical 
;; species associated with the artificial "BOUNDARY" compartment). 
                                                    

;; # Metanex Examples
;; 
;; ## Basic Information
;; Retrieve the MNXref metabolite with name 
;;  *N,N-dimethyl-beta-alanine*, together with molecular 
;;  information. 
(sq/query
 :metanet
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

(sq/query
 :metanet
 `{:select [?metabolite ?xref]
   :where [[?metabolite :rdfs/comment "N-nitrosomethanamine"]
           [?metabolite :mnx/chemXref ?xref]]})


;; ## KEGG C01732
;;
;; For the KEGG compound C01732, retrieve the
;; MNXref identifier, name and reference

(sq/query
 :metanet
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

(sq/query 
 :metanet
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

(sq/query
 :metanet
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

(sq/query
 :metanet
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

(sq/query
 :metanet
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

(sq/query
 :metanet
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

(sq/query
 :metanet
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
           [:optional [[?mnet :mnx/taxid ?taxon]]]]
   :group-by [?mnet ?taxon]})


;; ## GEMs Reaction in E.Coli
;;  A GEM is primarily a set of reactions: here are all the 
;;  reaction equations occurring in *bigg_e_coli_core*. NB: here 
;;  the reac label is the one produced while compiling MetaNetX 

(sq/query
 :metanet
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
(sq/query
 :metanet
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


;; ## Reactions involving PAT_ECOLI
;;  Given the protein with UniProt accession number *P42588* 
;;  (PAT_ECOLI, putrescine aminotransferase, EC 2.6.1.82) 
;;  retrieve all reactions and models in which this polypeptide 
;;  appears. 

(sq/query
 :metanet
 `{:prefixes {:up "<http://purl.uniprot.org/uniprot/>"}
   :select [?mnet_label ?reac_label ?reac_eq ?MNXR
            [(group-concat ?cata_label :separator ";") ?complex]]
   :where [[?pept :mnx/peptXref :up/P42588] 
           [?cata :mnx/pept ?pept]
           [?cata :rdfs/label ?cata_label]
           [?gpr  :mnx/cata ?cata]
           [?gpr  :mnx/reac ?reac]
           [?reac :rdfs/label ?reac_label]
           [?reac :rdfs/comment ?reac_eq]
           [?mnet :mnx/gpr ?gpr]
           [?mnet :rdfs/label ?mnet_label]
           [:optional [[?reac :mnx/mnxr ?MNXR]]]]
   :group-by [?mnet_label ?reac_label ?reac_eq ?MNXR]
   :order-by [?reac_label]
   :limit 10})



;; ## Reactions involving ExbB
;; 
;;  Same as previous but with *P0ABU7* as a query (Biopolymer
;; transport protein ExbB) .

(sq/query
 :metanet
 `{:prefixes {:up "<http://purl.uniprot.org/uniprot/>"}
   :select [?mnet_label ?reac_label ?reac_eq ?MNXR
            [(group-concat ?cata_label :separator ";") ?complex]]
   :where [[?pept :mnx/peptXref :up/P0ABU7]
           [?cata :mnx/pept ?pept]
           [?cata :rdfs/label ?cata_label]
           [?gpr  :mnx/cata ?cata]
           [?gpr  :mnx/reac ?reac]
           [?reac :rdfs/label ?reac_label]
           [?reac :rdfs/comment ?reac_eq]
           [?mnet :mnx/gpr ?gpr]
           [?mnet :rdfs/label ?mnet_label]
           [:optional [[?reac :mnx/mnxr ?MNXR]]]]
   :group-by [?mnet_label ?reac_label ?reac_eq ?MNXR]
   :order-by [?reac_label]
   :limit 10})
