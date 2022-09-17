(ns sparql-explore.build_notebooks
  (:require  [nextjournal.clerk :as clerk]))

(comment

  (require '[babashka.fs :as fs])
  
  (map str (fs/glob "notebooks" "explore**{.clj,cljc}"))
  (clerk/build-static-app! {:paths ["notebooks/explore_uniprot.clj"
                                    "notebooks/explore_pubchem.clj"
                                    "notebooks/explore_chembl.clj"
                                    "notebooks/explore_metanetx.clj"]}))

