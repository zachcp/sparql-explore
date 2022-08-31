(ns sparql-explore.query 
  (:require [com.yetanalytics.flint :as f]
            [mundaneum.query :as mq]
            [hato.client :as http]
            [clojure.data.json :as json]
            [tick.core :as tick])
  )



;; modify the outputs
;; (def ^:dynamic *endpoint* "https://query.wikidata.org/sparql")
;; (def ^:dynamic *endpoint* "https://idsm.elixir-czech.cz/sparql/endpoint/chebi")


(defn clojurize-values
  "Convert the values in `result` to Clojure types."
  [result]
  (into {} (map (fn [[k {:keys [type value datatype] :as v}]]
                  [k (condp = type
                       "uri" (or (mq/uri->keyword #"(.*#)(.*)$" value)
                                 (mq/uri->keyword #"(.*/)([^/]*)$" value)
                                 (:value v))
                       "literal" (condp = datatype
                                   "http://www.w3.org/2001/XMLSchema#decimal" (Float/parseFloat value)
                                   "http://www.w3.org/2001/XMLSchema#integer" (Integer/parseInt value)
                                   "http://www.w3.org/2001/XMLSchema#int" (Integer/parseInt value)
                                   "http://www.w3.org/2001/XMLSchema#dateTime" (tick/instant value)
                                   "http://www.w3.org/2001/XMLSchema#date" (tick/date value)
                                   nil value ; no datatype, return literal as is
                                   v) ; unknown datatype, return whole value map
                       v)]) ; unknown value type, return whole value map
                result)))

(defn do-query-uniprot
  "Query the WikiData endpoint with the SPARQL query in `sparql-text` and convert the return into Clojure data structures."
  [sparql-text]
   (mapv clojurize-values
         (-> (http/get "https://sparql.uniprot.org/sparql/"
                       {:query-params {:query sparql-text
                                       :format "json"}})
             :body
             (json/read-str :key-fn keyword)
             :results
             :bindings)))


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


;; TODO should label service be optional?
;; (defn query
;;   ([sparql-form]
;;    (query {} sparql-form))
;;   ([opts sparql-form]
;;    (-> sparql-form
;;        mq/clean-up-symbols-and-seqs
;;        (update :prefixes merge uniprot-prefixes)
;;        f/format-query
;;       do-query-uniprot)))

(defn query
  ([sparql-form]
   (query {} sparql-form))
  ([opts sparql-form]
    (let [sparql-query 
          (-> sparql-form 
              mq/clean-up-symbols-and-seqs 
              (update :prefixes merge uniprot-prefixes) 
              f/format-query)]
      (println (clojure.pprint/pprint sparql-query)) 
      (do-query-uniprot sparql-query))))