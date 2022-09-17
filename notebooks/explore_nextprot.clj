^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore-nextprot
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
  (clerk/show! "notebooks/explore_nextprot.clj")
;;   (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"
;;                                     "notebooks/explore_pubchem.clj"
;;                                     "notebooks/explore_chembl.clj"
;;                                     "notebooks/explore_metanetx.clj"]})
  )




;; # Introduction
;; 

;; NextProt
;; - [Sparql](https://api.nextprot.org/sparql)
;;
;; 

;; # NextProte Sparql Examples
;; 
;; ## Basic Information
;; Retrieve the MNXref metabolite with name 
;;  *N,N-dimethyl-beta-alanine*, together with molecular 
;;  information. 
(sq/query
 :nextprot
 `{:select-distinct [?entry] 
   :where [
           ;; Todo: this  values clause is causing issues
          ;;  [:values {?cytoloc 
          ;;            [:np-terminology/SL-0086 :np-terminlogy/GO_0005737]}] 
           [?entry :np/isoform ?iso]
           [?iso  (cat :np/keyword :np/term) :np-terminology/KW-0597] ;phosphorylated
           [?iso :np/cellularComponent ?loc]
           [?loc (cat :np/term :np/childOf)  ?cytoloc]
           ;No negative localization evidence
           [:filter (not (exists [[?loc :np/negativeEvidence ?negev]]))] ] 
   })
