(ns sparql-explore.build_notebooks
  (:require  [nextjournal.clerk :as clerk]))

(comment

  (require '[babashka.fs :as fs])
  
  (let [notebook-paths (map str (fs/glob "notebooks" "explore**{.clj,cljc}"))]
   (clerk/build-static-app! {:paths notebook-paths})) 
  )

