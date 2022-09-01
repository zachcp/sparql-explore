^{:nextjournal.clerk/visibility :hide-ns
  :nextjournal.clerk/toc true}
(ns sparql-explore.explore_uniprot
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
  (clerk/show! "notebooks/explore.clj")
  (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"]}) 
  )





;; # Introduction
;; 
;; This notebook is an exploration of the UNIPROT SPARQL interface by way
;; of copying/translating the [SPARQL examples](https://sparql.uniprot.org/.well-known/sparql-examples/)
;; into [flint](https://github.com/yetanalytics/flint),  really nice clojure SPARQL DSL. I 
;; also borrowed a number of functions from [Mundaneum](https://github.com/jackrusher/mundaneum), Jack Rusher's 
;; excellent SPARQL DSL for wikidata.
;; 
;; ## Brief Recap of Findings
;; 
;; ### Flint / SPARQL
;; 
;; Flint is a solid library and where I had trouble it was largely due to 
;; my lack of understanding of the SPARQL DSL. However, the [docs](https://cljdoc.org/d/com.yetanalytics/flint/0.2.0/doc/readme)
;; are really good and I was able to wrk around the issues I encountered which, 
;; roughly, were as follows.
;; 
;; 1. Lots of UNIPROT identifiers are  numbers or symbols and can cause issues
;; for Flint parsing. You can always drop down to full IRI specification, e.g. like [this](https://github.com/yetanalytics/flint/issues/29#issuecomment-1232096313), 
;; or you can use a function like `~(keywords "chebi/97065")` or the `full-tax-IRI`.
;; 
;; 2. The RDF object model feels slippery and its easy to get the node connections wrong. It
;; also took me awhile to find the [property path](https://cljdoc.org/d/com.yetanalytics/flint/0.2.0/doc/triples)
;; documents which explain how to construct complicated paths. (prperty paths are things like `alt`, `cat`, `*`, and `+`).

;; 3. Similar to property paths it took me a little while to grok some of the nested forms. Once again
;; the Flint docs and links to RDF were very helpful.

;; 4. I would benefit from a better investigation of the FALDO namespace; it is 
;; being adopted as the descriptive standard for all feature locations.

;;     - See [Variant: TYR->PHE](#Variant:%20TYR-%3EPHE) for FALDO describing the location of specifc mutations.
;;     - see [Transmembrane](#Transmembrane) for faldo properties in combination with `cat`
;;     - see [find phospho-Threonine](#find%20phospho-Threonine) this example uses faldo and is
;;         used to identify a specific sequence motif in which a phospho-threonine is found: one of my favorite examples
;; 



;; ### UNIPROT
;;
;; UNIPROT is amazing. No doubt about it. During this exercise I ended up exploring the
;; website deeper than I had in awhile and I get the feeling that the entire, awesome edifice
;; is build on linked data. The benefits show up in richness of the links on every 
;; page. I hadn't noticed the various panels on subcellular localization, nor the
;; slick ways to view feature/variant locations. These are really nice features and
;; you recreate the nuts and bolts of such viz when you go through some of the examples
;; below if you combined a front end on top of the SPARQL data you get back you can 
;; image creating the widgets without terrible difficulty (this is supposed to 
;; read as a compliment to their data architecture team)

;; Heres a couple of additional comments:

;; 1. Federated queries are slow. In general if a query had a call to 
;; `service` it was slow.  RheaDB seemed quite quick but many of the examples
;; were slow or didn't finish at all (note: structure early queries as limited subqueries).
;; One pleasant counterexample was the linkout to the [duck pics](#Ducks%20with%20Pictures) - 
;; they render nicely in the UNIPROT website.
;; 
;; 2. A bunch of the queries were broken or didn't work. As I was trying to both learn Flint 
;; and UNIPROT this tripped me up a bit but I began to always check in the browser to make 
;; sure a query actually could work as written.


;; ### SPARQL / FAIR Resource
;; 
;; I'm looking forward to trying more of the SPARQL scientific datasources out there. I think there
;; may be ways to make the querying more ergonomic/discvoerable when using the REPL approach.
;; Other Large Scientific SPARQL datasets.
;; - ChEMBL
;; - NEXTPROT 
;; - PUBCHEM/
;; - rheaDB
;; - wikidata



;; ## Helper Functions
;; 
;; I only ended up with one helper funciton but would consider a
;; few for commonly used item - e.g taxonomy, protein names.
;; note that this particular function only returns the [IRI literal](https://cljdoc.org/d/com.yetanalytics/flint/0.2.0/doc/rdf-terms)
;; for the taxon of interest.
;; 
(defn full-tax-IRI [taxid]
  (str  "<http://purl.uniprot.org/taxonomy/" taxid ">"))

;; ## Display
;; This page is rendered by [Clerk](https://github.com/nextjournal/clerk), an
;; `Local-First Notebook [Technology] for Clojure`. This is my first time giving
;; it a proper whirl and its great for many things although its a bit opposite
;; from Rmd/[quarto](https://quarto.org) where the doc is Markdown and code/functions
;; are fenced in blocks.  So, i don't have as good of a flow for working with it - but... 
;; man does is look slick!
;; 




;; # Examples from UNIPROT

;; I tried to implement each of the examples in the UNIPROT website. Not all worked;
;; some had issues; and some were slow. In each of those cases I ignored them and moved on.
;; In some cases I commented the SPARQL of Flint-equivalanet. FOr the others, you would need to
;; visit the UNIPROT website.
;;

;; ## Get Taxa
;; Pull down 10 taxa
(sq/query `{:select [?taxon]
            :where  [[?taxon :a :up/Taxon]]
            :limit 10})


;; ## Proteins from E.Coli
;; get bacterial taxa
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?taxon ?name]
   :where  [[?taxon :a :up/Taxon]
            [?taxon :up/scientificName ?name]
            [?taxon :rdfs/subClassOf  ~(full-tax-IRI 2)]]
   :limit 5})



;; ## Proteins from E.Coli
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?organism ?isoform ?aa_sequence]
   :where  [[?protein a :up/Protein]
            [?protein :up/organism ?organism]
            [?organism :rdfs/subClassOf ~(full-tax-IRI 83333)]
            [?protein :up/sequence ?isoform]
            [?isoform :rdf/value ?aa_sequence]]
   :limit 5})


;; ## Mnemonic
(sq/query
 `{:select [?protein ?name]
   :where  [[?protein a :up/Protein]
            [?protein :up/mnemonic "A4_HUMAN"]
            [?protein :rdfs/label ?name]]
   :limit 5})


;; ## PDB Linkout
;;
;; Note: the  `keywords` are UNIPROT keywords. In this case it 
;; is a keyword representing [Acetoin biosynthesis](https://www.uniprot.org/keywords/5)

^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?db]
   :where  [[?protein a :up/Protein]
            [?protein :up/classifiedWith ~(keyword "keywords/5")]
            [?protein :rdfs/seeAlso ?db]
            [?db :up/database "<http://purl.uniprot.org/database/PDB>"]]
   :limit 5})


;; ## PDB Linkout2
;;
;; Note: the  `keywords` are UNIPROT keywords. In this case it 
;; is a keyword representing [Acetoin biosynthesis](https://www.uniprot.org/keywords/5)

^{::clerk/viewer clerk/table}
(sq/query
 `{:select-distinct [?protein ?db ?link]
   :where  [[?protein a :up/Protein]
            [?protein :up/classifiedWith ~(keyword "keywords/5")]
            [?protein :rdfs/seeAlso ?link]
            [?link :up/database ?db]
            [?db :up/category "3D structure databases"]]
   :limit 5})


;; ## SWISSProt with 'DNA'
;;
;;  Bit of a convoluted query but helpful once in place.
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?name]
   :where  [[?protein a :up/Protein]
            [?protein   :up/reviewed true]
            [?protein   :up/recommendedName ?recommended]
            [?recommended :up/fullName ?name]
            [?protein :up/encodedBy ?gene]
            [?gene :skos/prefLabel ?text]
            [:filter  (contains ?text "DNA")]]

   :limit 5})


;; ## Rare Disease
;;
;;  Bit of a convoluted query but helpful once in place.
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?name ?text]
   :where  [[?protein a :up/Protein]
            [?protein :up/organism  ~(full-tax-IRI 9606)]
            [?protein :up/encodedBy ?gene]
            [?gene :skos/prefLabel ?name]
            [?protein :up/annotation ?annotation]
            [?annotation a :up/Disease_Annotation]
            [?annotation :rdfs/comment ?text]]
   :limit 5})


;; ## Gene Name Involved in Disease
;;
;;  Bit of a convoluted query but helpful once in place.
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?name ?text]
   :where  [[?protein a :up/Protein]
            [?protein :up/organism  ~(full-tax-IRI 9606)]
            [?protein :up/encodedBy ?gene]
            [?gene :skos/prefLabel ?name]
            [?protein :up/annotation ?annotation]
            [?annotation a :up/Disease_Annotation]
            [?annotation :rdfs/comment ?text]]
   :limit 5})


;; ## Loss of Function
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?text]
   :where  [[?protein a :up/Protein]
            [?protein :up/organism  ~(full-tax-IRI 9606)]
            [?protein :up/annotation ?annotation]
            [?annotation a :up/Natural_Variant_Annotation]
            [?annotation :rdfs/comment ?text]
            [:filter (contains ?text "loss of function")]]
   :limit 5})


;; ## Variant: TYR->PHE
;;
;; The faldo range stuff needed to go thourhg a blank node.
;; not really sure how to best use that class/ontology
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?annotation ?begin ?text ?original ?substitution]
   :where  [[?protein a :up/Protein]
            [?protein   :up/organism  ~(full-tax-IRI 9606)]
            [?protein   :up/annotation ?annotation]
            [?annotation a :up/Natural_Variant_Annotation]
            [?annotation   :rdfs/comment ?text]
            [?annotation   :up/substitution ?substitution]
            [?annotation (cat :up/range :faldo/begin) _b1]
            [_b1 :faldo/position ?begin]
            [_b1 :faldo/reference ?sequence]
            [?sequence :rdf/value ?value]
            [:bind [(substr ?value ?begin 1) ?original]]
            [:filter (and (= ?original "Y")  (= ?substitution "F"))]]
   :limit 5})


;; ## Transmembrane
;;
;; Note the annotation type is transmembrane 
;; 
;; and the faldo phrasing here makes more sense to me
;; note: running into some slowness on this query
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?begin ?end]
   :where  [[?protein a :up/Protein]
            [?protein   :up/annotation ?annotation]
            [?annotation a :up/Transmembrane_Annotation]
            [?annotation   :rdfs/comment ?text]
            [?annotation   :up/range ?range]
            [?range (cat :faldo/begin :faldo/position) ?begin]
            [?range (cat :faldo/end :faldo/position) ?end]]

   :limit 5})


;; ## Deposit Date
;;
;; Note the annotation type is transmembrane 
;; 
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?date]
   :where  [[?protein a :up/Protein]
            [?protein   :up/created  ?date]
            [:filter (= ?date ~(tick/date "2010-11-30"))]]

   :limit 5})

;; ## Check Integration Date
;;
;; use `ASK`. Need string literals
;; ToDO: Fix
^{::clerk/viewer clerk/table}
(sq/query
 `{:ask []
   :where  [[?protein a :up/Protein]
            [?protein   :up/created  ?date]
            [:filter (= ?date ~(tick/date "2013-01-09"))]]})



;; ## Construct Triples
;;
;; Todo: work with construct
;; 
;;```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:construct [?protein a :up/HumanProtein]
;;    :where  [[?protein a :up/Protein]
;;             [?protein   :up/organism  ~(full-tax-IRI 9606)]]
;;    :limit 5})
;; ```   


;; ## Describe Protein
;;
;; Todo: work with descrive
;;
;; ```clj
;;  ; DESCRIBE <http://purl.uniprot.org/embl-cds/AAO89367.1>
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:describe ["<http://purl.uniprot.org/embl-cds/AAO89367.1>"]})
;; ```


;; ## Describe Taxonmy
;;
;; Todo: work with describe
;;
;;  ; DESCRIBE <http://purl.uniprot.org/embl-cds/AAO89367.1>
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:describe [~(full-tax-IRI 9606)]
;;    :from  "<http://sparql.uniprot.org/taxonomy>"})



;; ## Avg X-ref to PDV
;;
;;  aggregate don't run by defualt
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?protein [(avg ?linksToPdbPerEntry) ?avgLinksToPdbPerEntry]]
;;    :where  {:select [?protein [(count ?db :distinct? true) ?linksToPdbPerEntry]]
;;             :where [[?protein a :up/Protein]
;;                     [?protein :rdfs/seeAlso ?db]
;;                     [?db :up/database "<http://purl.uniprot.org/database/PDB>"]
;;                     ]
;;             :group-by [?protein]
;;             :order-by [(desc ??linksToPdbPerEntry)]}
;; ```

;; ## Avg EC Class Count
;;
;;  Todo: 2 issues. 
;;  - one related to EC class names (use keyword trick)
;;  - values needs to be in teh where clause
;; 
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?enzyme [(count ?protein) ?size]]
;;    :where [[?protein :up/enzyme ?enzyme]
;;            [?enzyme rdfs:subClassOf ?ecClass]]
;;    :group-by [?enzyme]
;;    :order-by [?enzyme]
;;    :values [ ?ecClass [(ec:1.-.-.-) (ec:2.-.-.-) (ec:3.-.-.-) (ec:4.-.-.-) (ec:5.-.-.-) (ec:6.-.-.-) (ec:7.-.-.-)]]
;;    :limit 5})

;; (f/format-query
;;  `{:prefixes {:rdfs  "<http://www.w3.org/2000/01/rdf-schema#>"
;;               :taxon "<http://purl.uniprot.org/taxonomy/>"
;;               :up    "<http://purl.uniprot.org/core/>"
;;               :ec  "<http://purl.uniprot.org/enzyme/>"} 
;;    :select [?enzyme ?protein]
;;    :where [[?protein :up/enzyme ?enzyme]
;;            [?enzyme :rdfs/subClassOf ?ecClass]]
;;    :values {[?ecClass] [ [~(keyword "ec/1.-.-.-")] ]}
;;    :limit 5})

;; ```

;; ```sparql
;; SELECT ?enzyme (COUNT (?protein) as ?size)
;; WHERE
;; {VALUES (?ecClass) {(ec:1.-.-.-) (ec:2.-.-.-) (ec:3.-.-.-) (ec:4.-.-.-) (ec:5.-.-.-) (ec:6.-.-.-) (ec:7.-.-.-)}
;;  ?protein up:enzyme ?enzyme .
;;  ?enzyme rdfs:subClassOf ?ecClass .}
;; GROUP BY ?enzyme ORDER BY ?enzyme
;; ```


;; ## Natural Variants from PubMed Id
;;
;; Todo: fix / check binding clauses
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (f/format-query
;;  `{:select [?accession ?annotation_acc ?pubmed ?protein ?source]
;;    :where  [[?protein a :up/Protein]
;;             [?protein :up/annotation ?annotation]
;;             [?annotation a :up/Natural_Variant_Annotation]
;;             [?attribution :up/source ?source]
;;             [?linkToEvidence :rdf/object ?annotation]
;;             [?linkToEvidence :up/attribution ?attribution]
;;             [?source a :up/JournalCitation]
;;             [:bind [(substr (str ?protein) 33) ?accession]]
;;             [:bind [(if (contains (str ?annotation) "#SIP")
;;                       (substr (str ?annotation) 33)
;;                       (substr (str ?annotation) 36))
;;                     ?annotation_acc]]
;;             [:bind [(substr (str ?source) 35) ?pubmed]]]
;;    :limit 5}) 
;; ```


;; ## Attributions
;;
;; Todo: fix
;;
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?source [(count ?attribution) ?attributions]]
;;    :where  [ 
;;             [?protein a :up/Protein]
;;             [?protein :up/annotation ?annotation]
;;             [?linkToEvidence :rdf/object ?annotation]
;;             [?linkToEvidence :up/attribution ?attribution]
;;             [?attribution :up/source ?source]
;;             [?source a :up/JournalCitation]
;;             ]
;;    :group-by [?source]
;;    :order-by [(desc (count ?attribution))]})


;; ## Protein Location
;;
;; Note the annotation type is transmembrane 
;; 
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?disease ?location_inside_cell ?cellcmpt]
   :where  [[?protein :up/annotation ?diseaseAnnotation]
            [?protein :up/annotation ?subcellAnnotation]
            [?diseaseAnnotation (cat :up/disease :skos/prefLabel) ?disease]
            [?subcellAnnotation (cat :up/locatedIn :up/cellularComponent) ?cellcmpt]
            [?cellcmpt :skos/prefLabel ?location_inside_cell]]

   :limit 5})


;; ## Protein Location
;;
;; Note the annotation type is transmembrane 
;; 
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?disease ?location_inside_cell ?cellcmpt]
   :where  [[?protein :up/annotation ?diseaseAnnotation]
            [?protein :up/annotation ?subcellAnnotation]
            [?diseaseAnnotation (cat :up/disease :skos/prefLabel) ?disease]
            [?subcellAnnotation (cat :up/locatedIn :up/cellularComponent) ?cellcmpt]
            [?cellcmpt :skos/prefLabel ?location_inside_cell]]
   :limit 5})


;; ## Go Terminology Example
;;  Todo. Implement



;; ## Kinase Activity
;;
;; Count the kinases
;; 
(sq/query
 `{:select [[(count ?protein :distinct? true) ?pc]]
   :where  [[?protein a :up/Protein]
            [?protein   :up/reviewed true]
            [?protein   :up/organism ~(full-tax-IRI 9606)]
            [?protein
             (alt :up/classifiedWith (cat :up/classifiedWith :rdfs/subClassOf))
             ~(keyword "GO/0016301")]]})


;; ## Release Number
;;
;; Todo: Also  broken in the examples
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?version]
   :from "<http://sparql.uniprot.org/.well-known/void>"
   :where  [[_ "<http://purl.org/pav/2.0/version>" ?version]]})


;; ## Get HLA
;;  
;; Todo: also broken in the examples
;;
(sq/query
 `{:select [?protein ?anyKindOfName]
   :where  [[?protein a :up/Protein]
            [?protein  (alt :up/recommendedName :up/alternativeName) ?structuredName]
            [?structuredName ?anyKindOfName  "HLA class I histocompatibility antigen, B-73 alpha chain"]
            [?anyKindOfName :rdfs/subPropertyOf :up/structuredNameType]]})




;; ## Get HLA Part II
;;  
;; Todo: also broken in the examples
;;
(sq/query
 `{:select [?protein ?anyKindOfName]
   :where  [[?protein a :up/Protein]
            [?protein  (alt
                        (alt :up/recommendedName :up/alternativeName)
                        (cat (alt :up/domain :up/component)
                             (alt :up/recommendedName :up/alternativeName)))

             ?structuredName]
            [?structuredName ?anyKindOfName  "HLA class I histocompatibility antigen, B-73 alpha chain"]
            [?anyKindOfName :rdfs/subPropertyOf :up/structuredNameType]]})



;; ## Names of uniprot entry P05067
;;  
;; Todo: implement
;;


;; ## chromosome of proteome UP000000625
;; 
;; This is the [proteome of E.Coli](https://www.uniprot.org/proteomes/UP000000625)
;; 
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?proteome]
   :where  [[?protein a :up/Protein]
            [?protein :up/reviewed true]
            [?protein :up/proteome ?proteome]]
   :values {?proteome ["<http://purl.uniprot.org/proteomes/UP000000625#Chromosome>"]}
   :limit 5})

;; ## Japanese Translation
;; 
;; todo: Implement
;; 


;; ## Find Merged LOCI
;; 
;; Genome of [Bordetella avium](https://www.uniprot.org/taxonomy/360910)
;; Shows `group-concat` and `having`
;; 
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein [(group-concat ?locusName :separator ",") ?locusNames]]
   :where  [[?protein a :up/Protein]
            [?protein :up/organism ~(full-tax-IRI 360910)]
            [?protein :up/encodedBy ?gene]
            [?gene :up/locusName ?locusName]]

   :group-by [?protein]
   :having [(> (count ?locusName) 1)]})



;; ## UNIParc Example
;; 
;; Todo: fix. 
;; Note: broken in examples 
;; 
;;
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?sequence ?entries]
;;    :where  {:select [?sequence [(count ?entry) ?entries]]
;;             :where [[:graph 
;;                      "<http://sparql.uniprot.org/uniparc>"
;;                      [[?sequence :up/sequenceFor ?entry]]]]
;;             :group-by [?sequence]
;;             :order-by [(desc ?entries)]
;;             }
;;    })
;; ```



;; ## UniProtKB entries with TDA
;; 
;;
;; Topological Domain Annotation [TDA]()https://www.uniprot.org/help/topo_dom)
;; 
;; Note: takes awhile. Off by default
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?protein [(group-concat ?comment :separator ",") ?comments]]
;;    :where  [[?protein a :up/Protein]
;;             [?protein :up/annotation ?annotation]
;;             [?annotation a :up/Topological_Domain_Annotation]
;;             [?annotation :rdfs/comment ?comment]]
;;    :group-by [?protein]
;;    :having [(> (count ?annotation) 1)]})
;; ```




;; ## Longest Variant Comment
;; 
;; This is the [proteome of E.Coli](https://www.uniprot.org/proteomes/UP000000625)
;; 
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?annotation ?comment]
   :where  [[?annotation a :up/Natural_Variant_Annotation]
            [?annotation :rdfs/comment ?comment]]
   :order-by [(desc (strlen ?comment))]})

;; ## Co-occurence Count
;; Co-occurence count of Topological Domain comment text in UniProtKB entries
;; Todo: Implement


;; ## Similar Proteins
;; 
;; Find the similar proteins for UniProtKB entry P05067 sorted by UniRef cluser identity
;; 
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?similar ?identity]

   :where  [[:bind [:uniprotkb/P05607 ?protein]]
            [?cluster (cat :up/member :up/sequenceFor) ?protein]
            [?cluster :up/identity ?identity]
            [?cluster :up/member ?member]
            [?member  :up/sequenceFor ?similar]
            [:filter (not (sameterm ?similar ?protein))]]})


;; ## Use OrthoDB
;;
;;  Todo: fix
;;  Note: doesn't work on the server either



;; ## find phospho-Threonine
;; 
;; This works but it takes a bit long.
;;
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [?protein ?comment ?begin ?end ?sequence ?motif]
;;    :where  [[?protein a :up/Protein]
;;             [?protein :up/organism ~(full-tax-IRI 9606)]
;;             [?protein :up/sequence  ?sequence]
;;             [?protein :up/annotation ?annotation]
;;             [?annotation a :up/Modified_Residue_Annotation]
;;             [?annotation :rdfs/comment ?comment]
;;             [?annotation :up/range ?range]
;;             ; locations
;;             [?range :faldo/begin _b1]
;;               [_b1  :faldo/position ?begin]
;;               [_b1  :faldo/reference ?sequence]
;;             [?range :faldo/end _b2]
;;               [_b2  :faldo/position ?end]
;;               [_b2  :faldo/reference ?sequence]
;;             [?sequence :rdf/value ?aaSequence]
;;             ; filters
;;             [:bind [(substr ?aaSequence (- ?begin 2) 4) ?motif]]
;;             [:filter (= ?motif "VSTQ")]
;;             [:filter (contains ?comment "Phosphothreonine")]
;;             ]
;;    :limit 5}
;;  )
;; ```


;; ## Wikidata Service
;; 
;;
;; Execution 400 from wikidata.
;;
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select [	?proteinIRI ?protein ?begin ?end ?chromosome ?assembly]
;;    :where  [[:bind [:uniprotkb/P05067 ?proteinIRI]]
;;             [:bind ["P05067" ?protein]] 
;;             [:service 
;;              "<https://query.wikidata.org/sparql>" 
;;              [[?wp :wdt/P352 ?protein]]]]})
;; ```

;; ```sparql
;; SELECT 
;; 	?protein 
;; 	?begin
;; 	?end
;; 	?chromosome
;; 	?assembly
;; WHERE {
;;     {
;;         BIND(uniprotkb:P05067 AS ?proteinIRI)
;;         BIND (SUBSTR(STR(?proteinIRI), STRLEN(STR(uniprotkb:))+1) AS ?protein)
;;     }
;;     SERVICE <https://query.wikidata.org/sparql> {
;;         ?wp wdt:P352 ?protein ;
;;             wdt:P702 ?wg . 
;;         ?wg p:P644   ?wgss .
;;         ?wgss ps:P644        ?begin ;
;;           pq:P1057/wdt:P1813 ?chromosome ;
;;           pq:P659/rdfs:label ?assembly .
;;         ?wg p:P645 ?wgse .
;;         ?wgse ps:P645        ?end ;
;;           pq:P1057/wdt:P1813 ?chromosome ;
;;           pq:P659/rdfs:label ?assembly .
;;       FILTER(lang(?assembly) = "en")
;;   } 
;; } 
;; ```



;; ## Catalytic Activities
;; 
;;   Taxon 9606 is is human
;;   ECO 269 is experimental evidence
;; 
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?protein ?rhea]
   :where  [[:bind ["<http://purl.obolibrary.org/obo/ECO_0000269>" ?evidence]]
            [?protein :up/organism ~(full-tax-IRI 9606)]
            [?protein :up/classifiedWith ~(keyword "keywords/1185")]
            [?protein :up/annotation ?a]
            ; catalytic activity
            [?a a     :up/Catalytic_Activity_Annotation]
            [?a       :up/catalyticActivity ?ca]
            [?ca      :up/catalyzedReaction ?rhea]
            ; link attributions and activity
            {_b1 {:rdf/subject    #{?a}
                  :rdf/predicate  #{:up/catalyticActivity}
                  :rdf/object     #{?ca}
                  :up/attribution #{?attribution}}}
            ; link attribution to evidence
            [?protein :up/attribution ?attribution]
            [?attribution :up/evidence ?evidence]]
   :limit 5})



;; ## Sphingolipid Metabolism
;; 
;; via rheaDB. takes time so note sure if service is timing out
;; 
;; ```clj
;; (sq/query
;;  `{:select-distinct [?protein ?chemblEntry]
;;    :where  [[:service
;;              "<https://sparql.rhea-db.org/sparql>"
;;              [[?rhea :rdfs/subClassOf :rh/Reaction]
;;               [?rhea 
;;                (cat :rh/side :rh/contains :rh/compound :rh/chebi (+ :rdfs/subClassOf))
;;                ~(keyword "CHEBI/26739")]]] 
;;             [?ca :up/catalyzedReaction ?rhea]
;;             [?protein (cat :up/annotation :up/catalyticActivity) ?ca]
;;             [?protein :up/organism ~(full-tax-IRI 9606)]
;;             [?protein :rdfs/seeAlso ?chemblEntry]
;;             [?chemblEntry :up/database  "<http://purl.uniprot.org/database/ChEMBL>"]]})
;; ```



;; ## Sequence Fragments
;; 
;; Note use of `:minus`
;;
;; Todo: fix. Also broken on the site
;; ```clj
;; (sq/query
;;  `{:select-distinct [?protein]
;;    :where  [[?protein a :up/Protein]
;;             [?protein :up/sequence ?sequence] 
;;             [:minus [[?sequence :up/fragment _]]]]})
;; ```


;; ## Patent Connection
;;
;; Todo: fix. Also broken on the site
;; ```sparql
;; SELECT ?citation ?patent ?application ?applicationNo
;; WHERE
;; {
;;   ?citation a up:Patent_Citation ;
;;     skos:exactMatch ?patent .
;;   FILTER(CONTAINS(STR(?patent), 'EP'))
;;   BIND(SUBSTR(STR(?patent), 35) AS ?applicationNo)
;;   SERVICE<https://data.epo.org/linked-data/query>{
;;     ?application patent:publicationNumber ?applicationNo
;;   }
;; }
;; ```


;; ## Patents II
;; 
;; Connect patents cited in UniProtKB with those in the 
;; patent database at EPO via publication number, whose grant
;; date is more than twenty years in the past.
;;
;; Uses a service. Super slow to return. Turnin goff by defualt
;; 
;; 
;; ```clj
;; (sq/query
;;  `{:prefixes {:patent "<http://data.epo.org/linked-data/def/patent/>"}
;;    :select [?grantDate ?patent ?application ?applicationNo]
;;    :where  [[?citation a :up/Patent_Citation]
;;             [?citation :skos/exactMatch ?patent]
;;             [:bind [(substr (str ?patent), 35)     ?applicationNo]]
;;             [:bind [(substr (str ?patent), 33, 2)  ?countryCode]]
;;             [:service
;;              "<https://data.epo.org/linked-data/query>"
;;              [[?publication :patent/publicationNumber ?applicationNo]
;;               [?publication :patent/application ?application]
;;               [?application :patent/grantDate ?grantDate]]]

;;             [:bind [(- (year (now)) 20)  ?thisYearMinusTwenty]]
;;             [:bind [(year ?grantDate) ?grantYear]]
;;             [:filter (< ?grantYear ?thisYearMinusTwenty)]] 
;;    :limit 5})
;; ```



;; ## Interpro and RheaDB
;; 
;; Needs limit to work quickly.  Still slow. Adding to comments.
;;
;; ```clj
;; (sq/query
;;  `{:select [?interpro ?rhea]
;;    :from   ["<http://sparql.uniprot.org/uniprot>"]
;;    :where  [[?protein :up/reviewed true]
;;             [?protein :up/annotation ?annotation]
;;             [?protein :up/catalyticActivity ?rhea]
;;             [?protein :rdfs/seeAlso ?interpro]
;;             [?interpro :up/database "<http://purl.uniprot.org/database/InterPro>"]]
;;    :limit 5})
;; ```



;; ## Drugs from Wikibase
;; 
;; Todo: Implement Speed Issues/broken in examples
;;


;; ## Ducks with Pictures
;; 
;; Image Source: European Environmental Agency databases
;; Federated query to get images of ducks! Love it.
;;
^{::clerk/viewer clerk/table}
(sq/query
 `{:select [?taxon ?ncbiTaxid ?eunisTaxon ?eunisname ?image]
   :where  [[:graph
             "<http://sparql.uniprot.org/taxonomy>"
             [[?taxon a :up/Taxon]
              [?taxon :rdfs/subClassOf ~(full-tax-IRI 8835)]
              [:bind [(strafter (str ?taxon) "onomy/")  ?ncbiTaxid]]]]
            [:service
             "<https://semantic.eea.europa.eu/sparql>"
             [[?eunisTaxon a :eunisSpecies/SpeciesSynonym]
              [?eunisTaxon :eunisSpecies/binomialName ?eunisname]
              [?eunisTaxon :eunisSpecies/sameSpeciesNCBI ?ncbiTaxid]
              [?eunisTaxon "<http://xmlns.com/foaf/0.1/depiction>" ?image]]]]
   :limit 5})


;; ## Mega Query 
;; 
;; Retrieve the UniProt proteins, their catalyzed Rhea reactions, 
;; their encoding genes (Ensembl) and the anatomic entities where 
;; the genes are expressed (UBERON anatomic entites from Bgee
;; expression data resource) .
;;
;; worked decently until I added Bgee then hits 400 error
;; also takes a while on the public servers.
;; 
;; ```clj
;; (sq/query
;;  `{:prefixes {:genex "<http://purl.org/genex#>"
;;               :lscr   "<http://purl.org/lscr#>" }
;;    :select-distinct [?protein ?ensemblGene ?reaction ?anatomicEntityLabel ?anatomicEntity]
;;    :where  [
;;             ; subquery to rheadb
;;             [:where 
;;              {:select-distinct [?reaction]
;;               :where [[:service
;;                         "<https://sparql.rhea-db.org/sparql>"
;;                        [[?reaction     :rdfs/subClassOf :rh/Reaction]
;;                         [?reaction     :rh/equation    ?reactionEquation]
;;                         [?reaction     :rh/side        ?reactionSide]
;;                         [?reactionSide :rh/contains    ?participant]
;;                         [?participant  :rh/compound    ?compound]
;;                         ; filter on cholesterol (CHEBI:16113 == cholesterol)
;;                         [?compound     :rh/chebi      ~(keyword "CHEBI/16113")]]]]}]
;;             ; filter on human taxonomy
;;             [?protein :up/organism ~(full-tax-IRI 9606)]
;;             [?protein :up/annotation ?a]
;;             [?a  a :up/Catalytic_Activity_Annotation]
;;             [?a  :up/catalyticActivity ?ca]
;;             [?ca :up/catalyzedReaction ?reaction]
;;             [?protein (cat :rdfs/seeAlso :up/transcribedFrom) ?ensemblGene]
;; 
;;             ; federated query to Bgee (expression data)
;;             [:service 
;;              "<http://biosoda.expasy.org/rdf4j-server/repositories/bgeelight>" 
;;              [[?gene :genex/isExpressedIn ?anatomicEntity]
;;               [?gene :lscr/xrefEnsemblGene ?ensemblGene]
;;               [?anatomicEntity :rdfs/label ?anatomicEntityLabel]]]] 
;;    :limit 5 })
;; ```


;; ## Drugs for Sterol Metabolism
;; 
;; Todo: implement. Malformed query upstream
;;
;; ```sparql
;; SELECT
;;         DISTINCT  
;;             ?protein 
;;             ?proteinFullName 
;;             ?activityType 
;;             ?standardActivityValue 
;;             ?standardActivityUnit 
;;             ?chemblMolecule 
;;             ?chemlbMoleculePrefLabel
;; WHERE
;;   {
;;   # ChEBI: retrieve members of the ChEBI class ChEBI:15889 (sterol)
;;   # Rhea: retrieve the reactions involving these ChEBI as participants
;;   SERVICE <https://sparql.rhea-db.org/sparql> {
;;     ?reaction rdfs:subClassOf  rh:Reaction ;
;; 	      rh:status        rh:Approved ;
;; 	      rh:side          ?reactionSide .
;;     ?reactionSide
;; 	      rh:contains      ?participant .
;;     ?participant rh:compound  ?compound
;;     { 
;;       ?compound  rh:chebi  ?chebi .
;;       ?chebi (rdfs:subClassOf)+ CHEBI:15889
;;     } UNION {
;;       ?compound  rh:chebi           ?chebi .
;;       ?chebi2   rdfs:subClassOf     ?chebiRestriction .
;;       ?chebiRestriction
;; 		a           owl:Restriction ;
;; 		owl:onProperty      chebihash:has_major_microspecies_at_pH_7_3 ;
;; 		owl:someValuesFrom  ?chebi .
;;       ?chebi2 (rdfs:subClassOf)+ CHEBI:15889
;;     }
;;   }
;;   # UniProt: retrieve the human (taxid:9606) enzymes catalyzing these Rhea reactions 
;;   ?ca       up:catalyzedReaction  ?reaction .
;;   ?a        up:catalyticActivity  ?ca .
;;   ?protein  up:annotation         ?a ;
;; 	    up:organism           taxon:9606 ;
;; 	    up:recommendedName    ?proteinRecName .
;;   ?proteinRecName
;; 	    up:fullName           ?proteinFullName .
;;   # Find drugs in wikidata that interact with the UniProt Proteins
;;   # ChEMBL: retrieve the corresponding targets and with drugs in clinical phase 4
;;   # Via https://idsm.elixir-czech.cz/sparql/
;;   SERVICE <https://idsm.elixir-czech.cz/sparql/endpoint/idsm> { 
;;     ?activity a cco:Activity ;
;;       cco:hasMolecule ?chemblMolecule ;
;;       cco:hasAssay ?assay ;
;;       cco:standardType ?activityType ;
;;       cco:standardValue ?standardActivityValue ;
;;       cco:standardUnits ?standardActivityUnit .
;;     ?chemblMolecule cco:highestDevelopmentPhase ?highestDevelopmentPhase ;
;;       rdfs:label ?chemblMoleculeLabel ;
;;       skos:prefLabel ?chemlbMoleculePrefLabel .
;;     FILTER (?highestDevelopmentPhase > 3)
;;     ?assay cco:hasTarget ?target .
;;     ?target cco:hasTargetComponent/cco:targetCmptXref ?protein .
;;     ?protein a cco:UniprotRef .
;;   }
;; }
;; ```


;; ## Mouse Sterol Catalysis
;; 
;; Find mouse homologs in OMABrowser of human enzymes that catalyze 
;; reactions involving Sterols (CHEBI:15889) . Federating with Rhea-DB and OMABrowser.
;;
;; Note: takes a looooong time.
;; 
;; ```clj
;; ^{::clerk/viewer clerk/table}
;; (sq/query
;;  `{:select-distinct [?chebi ?reaction ?humanProtein ?mouseProtein ?cluster]
;;    :where  [
;;             ;; obtain rheaDB info
;;             [:service
;;              "<https://sparql.rhea-db.org/sparql>"
;;              [[?reaction :rdfs/subClassOf :rh/Reaction] 
;;               [?reaction (cat :rh/side :rh/contains :rh/compound) ?compound]
;;               [?compound :rh/chebi ?chebi] 
;;               [?chebi (* :rdfs/subClassOf) ~(keyword "CHEBI/15889")]]]

;;             ;; select/filter within uniprot/Sparql
;;             [?humanProtein :up/organism ~(full-tax-IRI 9606)]
;;             [?humanProtein :up/annotation ?a]
;;             [?a   a :up/Catalytic_Activity_Annotation]
;;             [?a   :up/catalyticActivity ?ca]
;;             [?ca  :up/catalyzedReaction ?reaction]

;;             ;; select/filter within uniprot/Sparql
;;             [:service
;;              "<https://sparql.omabrowser.org/sparql>"
;;              [[?cluster a :orth/ParalogsCluster]
;;               [?node1   :orth/hasHomologousMember* ?orthoProtein1]
;;               [?node2   :orth/hasHomologousMember* ?orthoProtein2]
;;               [?orthoProtein1 :lscr/xrefUniprot ?mouseProtein]
;;               [?orthoProtein2 :lscr/xrefUniprot ?humanProtein]
;;               ; homologs in mouse only
;;               [?orthoProtein1 
;;                (cat :orth/organism "<http://purl.obolibrary.org/obo/RO_0002162>")  
;;                ~(full-tax-IRI 10090)]]] 
;;             ]

;;    :limit 5})
;; ```