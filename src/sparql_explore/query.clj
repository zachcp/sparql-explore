(ns sparql-explore.query 
  (:require [com.yetanalytics.flint :as f]
            [mundaneum.query :as mq]
            [hato.client :as http]
            [clojure.data.json :as json]
            [tick.core :as tick]))


;; Static Prefix Values -------------

(def uniprot-prefixes
  "RDF prefixes automatically supported by the WikiData query service."
  {:xsd "<http://www.w3.org/2001/XMLSchema#>"
   :wikibase "<http://wikiba.se/ontology#>"
   :wdt "<http://www.wikidata.org/prop/direct/>"
   :wd "<http://www.wikidata.org/entity/>"
   :vg "<http://biohackathon.org/resource/vg#>"
   :uniprotkb "<http://purl.uniprot.org/uniprot/>"
   :uberon "<http://purl.obolibrary.org/obo/uo#>"
   :sp "<http://spinrdf.org/sp#>"
   :SLM "<https://swisslipids.org/rdf/SLM_>"
   :skos "<http://www.w3.org/2004/02/skos/core#>"
   :sio "<http://semanticscience.org/resource/>"
   :sh "<http://www.w3.org/ns/shacl#>"
   :schema "<http://schema.org/>"
   :sachem "<http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#>"
   :rh "<http://rdf.rhea-db.org/>"
   :rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
   :pubmed "<http://rdf.ncbi.nlm.nih.gov/pubmed/>"
   :ps "<http://www.wikidata.org/prop/statement/>"
   :pq "<http://www.wikidata.org/prop/qualifier/>"
   :patent "<http://data.epo.org/linked-data/def/patent/>"
   :p "<http://www.wikidata.org/prop/>"
   :owl "<http://www.w3.org/2002/07/owl#>"
   :orthodbGroup "<http://purl.orthodb.org/odbgroup/>"
   :orthodb "<http://purl.orthodb.org/>"
   :orth "<http://purl.org/net/orth#>"
   :obo "<http://purl.obolibrary.org/obo/>"
   :np "<http://nextprot.org/rdf#>"
   :nextprot "<http://nextprot.org/rdf/entry/>"
   :mnx "<https://rdf.metanetx.org/schema/>"
   :mnet "<https://rdf.metanetx.org/mnet/>"
   :mesh "<http://id.nlm.nih.gov/mesh/>"
   :lscr "<http://purl.org/lscr#>"
   :lipidmaps "<https://www.lipidmaps.org/rdf/>"
   :keywords "<http://purl.uniprot.org/keywords/>"
   :insdc "<http://identifiers.org/insdc/>"
   :identifiers "<http://identifiers.org/>"
   :glyconnect "<https://purl.org/glyconnect/>"
   :glycan "<http://purl.jp/bio/12/glyco/glycan#>"
   :genex "<http://purl.org/genex#>"
   :foaf "<http://xmlns.com/foaf/0.1/>"
   :faldo "<http://biohackathon.org/resource/faldo#>"
   :eunisSpecies "<http://eunis.eea.europa.eu/rdf/species-schema.rdf#>"
   :ensembltranscript "<http://rdf.ebi.ac.uk/resource/ensembl.transcript/>"
   :ensemblterms "<http://rdf.ebi.ac.uk/terms/ensembl/>"
   :ensemblprotein "<http://rdf.ebi.ac.uk/resource/ensembl.protein/>"
   :ensemblexon "<http://rdf.ebi.ac.uk/resource/ensembl.exon/>"
   :ensembl "<http://rdf.ebi.ac.uk/resource/ensembl/>"
   :ECO "<http://purl.obolibrary.org/obo/ECO_>"
   :ec "<http://purl.uniprot.org/enzyme/>"
   :dc "<http://purl.org/dc/terms/>"
   :cco "<http://rdf.ebi.ac.uk/terms/chembl#>"
   :chebihash "<http://purl.obolibrary.org/obo/chebi#>"
   :CHEBI "<http://purl.obolibrary.org/obo/CHEBI_>"
   :bibo "<http://purl.org/ontology/bibo/>"
   :allie "<http://allie.dbcls.jp/>"
   :GO "<http://purl.obolibrary.org/obo/GO_>"
   :insdcschema "<http://ddbj.nig.ac.jp/ontologies/nucleotide/>"
   :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
   :taxon "<http://purl.uniprot.org/taxonomy/>"
   :up "<http://purl.uniprot.org/core/>"})
   



(def pubchem-prefixes
  "RDF prefixes used by the IDSM webserver as described 
   here:https://idsm.elixir-czech.cz/sparql/doc/manual.html"
    ; add those use by pubchem but not in the uniprot list
    ; https://pubchemdocs.ncbi.nlm.nih.gov/rdf#table1 
    ; dcterms note we already have dc as alias
   {:sachem   "<http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#>"
    :pubchem  "<https://idsm.elixir-czech.cz/sparql/endpoint/pubchem>"
    :chembl   "<https://idsm.elixir-czech.cz/sparql/endpoint/chembl>"
    :chebi    "<https://idsm.elixir-czech.cz/sparql/endpoint/chebi>"
    :drugbank "<https://idsm.elixir-czech.cz/sparql/endpoint/drugbank>"
    :wikidata "<https://idsm.elixir-czech.cz/sparql/endpoint/wikidata>"
    :bao      "<http://www.bioassayontology.org/bao#>"
    :ndfrt    "<http://evs.nci.nih.gov/ftp1/NDF-RT/NDF-RT.owl#>"
    :ncit     "<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>"
    :cheminfa "<http://semanticscience.org/resource/>"
    :bp       "<http://www.biopax.org/release/biopax-level3.owl#>"
    :cito     "<http://purl.org/spar/cito/>"
    :fabio    "<http://purl.org/spar/fabio/>"
    :pdbo     "<http://rdf.wwpdb.org/schema/pdbx-v40.owl#>"

    ;; pubchem uses `dcterms` not `dc`
    :dcterms  "<http://purl.org/dc/terms/>"

    ;; duplicate from above
    :obo      "<http://purl.obolibrary.org/obo/>"
    :sio      "<http://semanticscience.org/resource/>"
    :rdf "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
    :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
    :owl "<http://www.w3.org/2002/07/owl#>"

    ;; pubchem terms
    :substance "<http://rdf.ncbi.nlm.nih.gov/pubchem/substance/>"
    :compound  "<http://rdf.ncbi.nlm.nih.gov/pubchem/compound/>"
    })


(def chembl-prefixes
  "RDF Prefixes for CHEMBL queries"
  {:rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
   :chembl "<http://rdf.ebi.ac.uk/terms/chembl#>"})

;; The similarity search procedure call is mapped to property sachem:similaritySearch. It accepts following arguments:

;; sachem:query specifies the chemical structure to be searched for. Supported query types include SMILES and MDL molecule file. This argument is mandatory.
;; sachem:cutoff specifies the cutoff for the similarity search (the default value is 0.8, values are in range of 0 to 1).
;; sachem:topn sets the upper limit on the count of returned results (default value is "unlimited", specified by -1).
;; Results of the procedure are compound values, that have following properties:

;; sachem:compound — compound URI
;; sachem:score — the similarity score of the compound
;; There is also a simplified variant of the similarity search procedure, mapped to property sachem:similarCompoundSearch. It uses the same arguments as sachem:similaritySearch, but returns the identified compounds directly as single-value non-structured results.

;; Substructure search
;; The substructure search procedure is mapped to property sachem:substructureSearch. It uses arguments sachem:query and sachem:topn with the same meaning as in the previous case, together with following extra arguments:

;; sachem:searchMode chooses between exact structure and substructure search, using parameter values sachem:exactSearch and sachem:substructureSearch, respectively.
;; sachem:tautomerMode chooses between various tautomer handling modes, value sachem:ignoreTautomers disables any tautomerism processing, sachem:inchiTautomers uses InChI-derived tautomer mathcing.
;; sachem:chargeMode chooses a coalescing mode of unspecified charge values in query, value sachem:defaultChargeAsAny assumes that unspecified charges are wildcard, sachem:defaultChargeAsZero assumes that unspecified charges must match zero, and sachem:ignoreCharges disables any charge matching.
;; sachem:isotopeMode chooses a coalescing mode of unspecified isotope values in query, value sachem:defaultIsotopeAsAny assumes that unspecified isotope values match any isotope, sachem:defaultIsotopeAsStandard assumes that unspecified isotopes must match the standard isotope of the element, and sachem:ignoreIsotopes disables any isotope matching.
;; sachem:stereoMode chooses stereochemistry handling using sachem:strictStereo or disables it completely using sachem:ignoreStereo
;; sachem:radicalMode chooses handling of free radicals, using either sachem:ignoreSpinMultiplicity that disables it completely, or sachem:defaultSpinMultiplicityAsZero and sachem:defaultSpinMultiplicityAsAny that behave just like the isotope and charge modes.



(defn clojurize-values
  "Convert the values in `result` to Clojure types."
  [result]
  (into {} (map (fn [[k {:keys [type value datatype] :as v}]]
                  [k (condp = type
                       "uri" (or (mq/uri->keyword #"(.*#)(.*)$" value)
                                 (mq/uri->keyword #"(.*/)([^/]*)$" value)
                                 (:value v))
                       
                       "typed-literal" (condp = datatype
                                         "http://www.w3.org/2001/XMLSchema#double"  (Float/parseFloat value)
                                          nil value ; no datatype, return literal as is
                                         v) ; unknown datatype, return whole value map)
                       
                       "literal"
                       (condp = datatype
                                   "http://www.w3.org/2001/XMLSchema#float"  (Float/parseFloat value)
                                   "http://www.w3.org/2001/XMLSchema#double"  (Float/parseFloat value)
                                   "http://www.w3.org/2001/XMLSchema#decimal" (Float/parseFloat value)
                                   "http://www.w3.org/2001/XMLSchema#integer" (Integer/parseInt value)
                                   "http://www.w3.org/2001/XMLSchema#int" (Integer/parseInt value)
                                   "http://www.w3.org/2001/XMLSchema#dateTime" (tick/instant value)
                                   "http://www.w3.org/2001/XMLSchema#date" (tick/date value)
                                   "http://www.w3.org/2001/XMLSchema#string" value

                                   nil value ; no datatype, return literal as is
                                   v) ; unknown datatype, return whole value map
                       v)]) ; unknown value type, return whole value map
                result)))



(defn do-query
  "Submit SPARQL data to SPARQL Endpoint"
  [sparql-text sparql-url]
  (mapv clojurize-values
        (-> (http/get sparql-url
                      {:query-params {:query sparql-text
                                      :format "json"}})
            :body
            (json/read-str :key-fn keyword)
            :results
            :bindings)))

(defn do-query-post
  "Submit SPARQL data to SPARQL Endpoint"
  [sparql-text sparql-url]
  (mapv clojurize-values
        (-> (http/post sparql-url 
                ; too me awhile to figure out the post params!
                ; cURL to the rescue then use clj
               {:accept "application/sparql-results+json"
                :form-params {:query sparql-text}})
            :body
            (json/read-str :key-fn keyword)
            :results
            :bindings)))



;; Query Helpers -----------------

(defn do-query-uniprot
  "Query uniprot"
  [sparql-text]
  (do-query sparql-text "https://sparql.uniprot.org/sparql/"))


(defn do-query-pubchem
  "Query pubchem via IDSM"
  [sparql-text]
  (do-query-post sparql-text "https://idsm.elixir-czech.cz/sparql/endpoint/idsm"))


(defn do-query-chembl
  "Query chembl via BIGCAT"
  [sparql-text]
  (do-query sparql-text "https://chemblmirror.rdf.bigcat-bioinformatics.org/sparql"))



(defn prepare-query 
  [sparql-form prefix-map]
  (-> sparql-form
      mq/clean-up-symbols-and-seqs
      (update :prefixes merge prefix-map)
      f/format-query))


;; Query Fns -----------------
;; Currently one per datasource.


(defn query
  ([sparql-form]
   (query {} sparql-form))
  ([opts sparql-form]
   (let [sparql-query (prepare-query sparql-form uniprot-prefixes)]
     (println (clojure.pprint/pprint sparql-query)) 
     (do-query-uniprot sparql-query))))


(defn query-pubchem
  ([sparql-form]
   (query-pubchem {} sparql-form))
  ([opts sparql-form]
   (let [sparql-query (prepare-query sparql-form pubchem-prefixes)]
     (println (clojure.pprint/pprint sparql-query))
     (do-query-pubchem sparql-query))))


(defn query-chembl
  ([sparql-form]
   (query-chembl {} sparql-form))
  ([opts sparql-form]
   (let [sparql-query (prepare-query sparql-form chembl-prefixes)]
     (println (clojure.pprint/pprint sparql-query))
     (do-query-chembl sparql-query))))
