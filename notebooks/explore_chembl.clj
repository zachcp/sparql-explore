^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-chembl
  (:require [sparql-explore.query :as sq]
            [nextjournal.clerk :as clerk]
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
(sq/query
 :chembl
 `{:select-distinct [?identifier ?targetLabel ?molLabel ?assayLabel ?type ?value ?molecule]
   :where [
           [?assay    :chembl/hasTarget     ?target]
           [?activity :chembl/hasAssay      ?assay]


           [?activity :chembl/hasMolecule   ?molecule]
           [?activity :chembl/type          ?type] 
           [?activity :chembl/standardValue ?value]
           ; get labels
           [?molecule :chembl/chemblId ?identifier]
           [?target   :rdfs/label ?targetLabel]
           [?molecule :rdfs/label ?molLabel]
           [?assay    :rdfs/label ?assayLabel]
           [?molecule :rdfs/label ?molLabel]] 
   
   :limit 10})
   

;; ## SMILES
;;
^{::clerk/viewer clerk/table}
(sq/query
 :chembl
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
(sq/query
 :chembl
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
(sq/query
 :chembl
 `{:select [[(count *) ?count]]
   :where [[?s ?p ?o]]
   :limit 10})


;; ## Molecules
(sq/query
 :chembl
 `{:select [?molecule ?type ?moleculelabel]
   :where [[?molecule a ?type]
           [?type (* :rdfs/subClassOf) :chembl/Substance] 
           [?moleculelabel :rdfs/label ?molLabel]
           ]
   :limit 10})


(sq/query 
 :chembl
 `{:select [?molecule ?type ?moleculelabel]
   :where [[?molecule a ?type]
           [?type (* :rdfs/subClassOf) :chembl/SmallMolecule]
           [?moleculelabel :rdfs/label ?molLabel]]
   :limit 10})

;; ## Target of Gleevec 
;; Get ChEMBL activities, assays and targets for the drug Gleevec (CHEMBL941)
^{::clerk/viewer clerk/table}
(sq/query
 :chembl
 `{:select [?activity ?assay ?target ?targetcmpt ?uniprot]
   :where [
           [?activity a :chembl/Activity ]
           [?activity   :chembl/hasMolecule :chembl_mol/CHEMBL941]
           [?activity   :chembl/hasAssay ?assay]
           [?assay      :chembl/hasTarget ?target]
           [?target     :chembl/hasTargetComponent ?targetcmpt]
           [?targetcmpt :chembl/targetCmptXref ?uniprot]
           [?uniprot a :chembl/UniprotRef]
           ]
   })



^{::clerk/viewer clerk/table}
(sq/query
 :chembl
 `{:select-distinct [?molecule ?p]
   :where [[:bind [:chembl_mol/CHEMBL941 ?molecule]]
           [?molecule ?p ?o]
           ]})

; paclitaxel
^{::clerk/viewer clerk/table}
(sq/query
 :chembl
 `{:select-distinct [?molecule ?p]
   :where [[:bind [:chembl_mol/CHEMBL428647 ?molecule]]
           [?molecule ?p ?o]]})

^{::clerk/viewer clerk/table}
(sq/query
 :chembl
 `{:select-distinct [?molecule ?p]
   :where [[:bind [:chembl_mol/CHEMBL428647 ?molecule]]
           [?s ?p ?molecule]]})

;; Todo: Fix
(sq/query
 :chembl
 `{:ask []
   :where [[:chembl_mol/CHEMBL428647 a :chembl/Substance]]})

(sq/query 
 :chembl
 `{:select-distinct [?p]
   :where [[:chembl_mol/CHEMBL428647 ?p ?o]]})

;; link to chebi
(sq/query
 :chembl
 `{:select-distinct [?o]
   :where [[:chembl_mol/CHEMBL428647 :skos/exactMatch ?o]]})

(sq/query
 :chembl
 `{:select-distinct [?chebi ?p ?o]
   :where [[:chembl_mol/CHEMBL428647 :skos/exactMatch ?chebi]
           [?chebi ?p ?o]
           ]})


;; cross refs
^{::clerk/viewer clerk/table}
(sq/query
 :chembl
 `{:select-distinct [?o]
   :where [[:chembl_mol/CHEMBL428647 :chembl/moleculeXref ?o]]})

(sq/query
 :chembl
 `{:select [?chemblid]
   :where [[:chembl_mol/CHEMBL428647 :chembl/chemblId ?chemblid]]})

(sq/query
 :chembl
 `{:select [?chemblid]
   :where [[:chembl_mol/CHEMBL428647 ?p ?o]]})

(sq/query
 :chembl
 `{:select-distinct [?s ?p]
   :where [
           [?s a :chembl/Activity]
           [?s ?p :chembl_mol/CHEMBL428647]
           ]})