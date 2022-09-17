(ns sparql-explore.query 
  (:require [com.yetanalytics.flint :as f]
            [mundaneum.query :as mq]
            [hato.client :as http]
            [clojure.data.json :as json]
            [tick.core :as tick]))


;; Prefix Values -------------

(def all-prefixes 
  {:CHEBI  "<http://purl.obolibrary.org/obo/CHEBI_>"
   :ECO    "<http://purl.obolibrary.org/obo/ECO_>"
   :GO     "<http://purl.obolibrary.org/obo/GO_>"
   :SLM    "<https://swisslipids.org/rdf/SLM_>"
   :allie  "<http://allie.dbcls.jp/>"
   :bao    "<http://www.bioassayontology.org/bao#>"
   :bibo   "<http://purl.org/ontology/bibo/>"
   :bp     "<http://www.biopax.org/release/biopax-level3.owl#>"
   :cco       "<http://rdf.ebi.ac.uk/terms/chembl#>"
   :chebi     "<https://idsm.elixir-czech.cz/sparql/endpoint/chebi>"
   :chebihash "<http://purl.obolibrary.org/obo/chebi#>"
   :chembl    "<http://rdf.ebi.ac.uk/terms/chembl#>"
   :chembl_mol "<http://rdf.ebi.ac.uk/resource/chembl/molecule/>"
   :cheminf   "<http://semanticscience.org/resource/>"
   :cheminfa "<http://semanticscience.org/resource/>"
   :cito     "<http://purl.org/spar/cito/>"
   :compound "<http://rdf.ncbi.nlm.nih.gov/pubchem/compound/>"
   :dc       "<http://purl.org/dc/terms/>"
   :dcterms  "<http://purl.org/dc/terms/>"
   :drugbank "<https://idsm.elixir-czech.cz/sparql/endpoint/drugbank>"
   :ec       "<http://purl.uniprot.org/enzyme/>"
   :ensembl  "<http://rdf.ebi.ac.uk/resource/ensembl/>"
   :ensemblexon       "<http://rdf.ebi.ac.uk/resource/ensembl.exon/>"
   :ensemblprotein    "<http://rdf.ebi.ac.uk/resource/ensembl.protein/>"
   :ensemblterms      "<http://rdf.ebi.ac.uk/terms/ensembl/>"
   :ensembltranscript "<http://rdf.ebi.ac.uk/resource/ensembl.transcript/>"
   :eunisSpecies      "<http://eunis.eea.europa.eu/rdf/species-schema.rdf#>"
   :fabio    "<http://purl.org/spar/fabio/>"
   :faldo    "<http://biohackathon.org/resource/faldo#>"
   :foaf     "<http://xmlns.com/foaf/0.1/>"
   :genex    "<http://purl.org/genex#>"
   :glycan   "<http://purl.jp/bio/12/glyco/glycan#>"
   :glyconnect "<https://purl.org/glyconnect/>"
   :identifiers "<http://identifiers.org/>"
   :insdc    "<http://identifiers.org/insdc/>"
   :insdcschema "<http://ddbj.nig.ac.jp/ontologies/nucleotide/>"
   :keywords "<http://purl.uniprot.org/keywords/>"
   :lipidmaps "<https://www.lipidmaps.org/rdf/>"
   :lscr     "<http://purl.org/lscr#>"
   :mesh     "<http://id.nlm.nih.gov/mesh/>"
   :mnet     "<https://rdf.metanetx.org/mnet/>"
   :mnx      "<https://rdf.metanetx.org/schema/>"
   :ncit     "<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>"
   :ndfrt    "<http://evs.nci.nih.gov/ftp1/NDF-RT/NDF-RT.owl#>"
   :nextprot "<http://nextprot.org/rdf/entry/>"
   :np       "<http://nextprot.org/rdf#>"
   :obo      "<http://purl.obolibrary.org/obo/>"
   :orth     "<http://purl.org/net/orth#>"
   :orthodb  "<http://purl.orthodb.org/>"
   :orthodbGroup "<http://purl.orthodb.org/odbgroup/>"
   :owl      "<http://www.w3.org/2002/07/owl#>"
   :p        "<http://www.wikidata.org/prop/>"
   :patent   "<http://data.epo.org/linked-data/def/patent/>"
   :pdbo     "<http://rdf.wwpdb.org/schema/pdbx-v40.owl#>"
   :pq       "<http://www.wikidata.org/prop/qualifier/>"
   :ps       "<http://www.wikidata.org/prop/statement/>"
   :pubchem  "<https://idsm.elixir-czech.cz/sparql/endpoint/pubchem>"
   :pubmed   "<http://rdf.ncbi.nlm.nih.gov/pubmed/>"
   :rdf      "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
   :rdfs     "<http://www.w3.org/2000/01/rdf-schema#>"
   :rh       "<http://rdf.rhea-db.org/>"
   :sachem   "<http://bioinfo.uochb.cas.cz/rdf/v1.0/sachem#>"
   :schema   "<https://schema.org/>"
   :sh       "<http://www.w3.org/ns/shacl#>"
   :sio      "<http://semanticscience.org/resource/>"
   :skos     "<http://www.w3.org/2004/02/skos/core#>"
   :sp       "<http://spinrdf.org/sp#>"
   :substance "<http://rdf.ncbi.nlm.nih.gov/pubchem/substance/>"
   :taxon    "<http://purl.uniprot.org/taxonomy/>"
   :uberon   "<http://purl.obolibrary.org/obo/uo#>"
   :uniprotkb "<http://purl.uniprot.org/uniprot/>"
   :up       "<http://purl.uniprot.org/core/>"
   :vg       "<http://biohackathon.org/resource/vg#>"
   :wd       "<http://www.wikidata.org/entity/>"
   :wdt      "<http://www.wikidata.org/prop/direct/>"
   :wikibase "<http://wikiba.se/ontology#>"
   :wikidata "<https://idsm.elixir-czech.cz/sparql/endpoint/wikidata>"
   :xsd      "<http://www.w3.org/2001/XMLSchema#>"})


(def query-data 
  
   {;; UNIPROT Endpoint
    :uniprot {:sparql-url "https://sparql.uniprot.org/sparql/"
              :request-type :get
              :base-prefixes [:insdc :rh :lipidmaps :schema :chebihash :uberon
                              :ECO :sachem :orthodbGroup :vg :allie :dc
                              :mnet :bibo :sp :identifiers :GO :mnx :ensembl :foaf :xsd :ec
                              :uniprotkb :glycan :owl :rdfs :ensemblprotein :nextprot
                              :keywords :up :wikibase :genex :CHEBI :wd :sio :ensembltranscript
                              :taxon :pubmed :mesh :np :faldo :orthodb :eunisSpecies :glyconnect
                              :wdt :patent :SLM :pq :cco :lscr :sh :obo :orth :skos :p :insdcschema
                              :ensemblexon :ps :ensemblterms :rdf]}
    
    ;; query pubchem via IDSM pubchem server
    :pubchem {:sparql-url "https://idsm.elixir-czech.cz/sparql/endpoint/idsm"
              :request-type :post
              :base-prefixes [:sachem :compound :dcterms :owl :rdfs :bao
                              :cito :ncit :substance :chembl :sio :pubchem
                              :drugbank :ndfrt :bp :chebi :wikidata :fabio
                              :obo :cheminfa :rdf :pdbo]}
    
    ;; query chembl via BIGCAT server
    :chembl {:sparql-url "https://chemblmirror.rdf.bigcat-bioinformatics.org/sparql"
             :request-type :get
             :base-prefixes [:rdfs :chembl :cheminf :chembl_mol :skos :foaf :schema]}
    
    ;; metanet server
    :metanet {:sparql-url "https://rdf.metanetx.org/sparql?default-graph-uri=https://rdf.metanetx.org/"
              :request-type :get
              :base-prefixes [:mnx :owl :rdf :rdfs :chebi]}})


;; The similarity search procedure call is mapped to property sachem:similaritySearch. It accepts following arguments:

;; sachem:query specifies the chemical structure to be searched for. Supported query types include SMILES and MDL molecule file. This argument is mandatory.
;; sachem:cutoff specifies the cutoff for the similarity search (the default value is 0.8, values are in range of 0 to 1).
;; sachem:topn sets the upper limit on the count of returned results (default value is "unlimited" specified by -1).
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
                                         "http://www.w3.org/2001/XMLSchema#dateTime" (tick/instant value)
                                         "http://www.w3.org/2001/XMLSchema#date" (tick/date value)
                                         "http://www.w3.org/2001/XMLSchema#integer"  (Integer/parseInt value)
                                         "http://www.w3.org/2001/XMLSchema#int"      (Integer/parseInt value)
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



(defn prepare-query 
  [sparql-form prefix-map]
  (-> sparql-form
      mq/clean-up-symbols-and-seqs
      (update :prefixes merge prefix-map)
      f/format-query))



(defn query
  ;; default is to prepare and print not submit
  ;; will use all available RDF prefixes
  ([sparql-form]
   (prepare-query sparql-form all-prefixes))
  ([sparql-loc sparql-form]
   {:pre [(contains? query-data sparql-loc)]}
   (let [sparql-url      (get-in query-data [sparql-loc :sparql-url])
         sparql-prefixes (select-keys all-prefixes (get-in query-data [sparql-loc :base-prefixes]))
         request-type    (get-in query-data [sparql-loc :request-type])
         sparql-text     (prepare-query sparql-form sparql-prefixes)]
     (if (= request-type :get)
       (do-query      sparql-text sparql-url)
       (do-query-post sparql-text sparql-url)))))


 ;; Todo resolve numeric names that interfere with keywords - e.g. :taxon/9398
