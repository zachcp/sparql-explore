# sparql-explore/sparql-explore

Exploration of the UNIPROT SPARQL interface by way
of copying/translating the [SPARQL examples](https://sparql.uniprot.org/.well-known/sparql-examples/)
into [flint](https://github.com/yetanalytics/flint),  a really nice clojure SPARQL DSL. I 
also borrowed a number of functions from [Mundaneum](https://github.com/jackrusher/mundaneum), Jack Rusher's 
excellent SPARQL DSL for wikidata. 

Queries and data rendered [online](https://zachcp.github.io/sparql-explore/#/notebooks/explore.clj) using [Clerk](https://github.com/nextjournal/clerk). 
 

## The Setup

1. create new clj app with `neil`
2. modify `dpes.edn` to copy the Mundaneum repo.
3. open VSCode/calva and start repl.
4. start adding UNIPROT examples
5. git commit / push/ and serve.

```sh
lein new app sparql-explore
```

## Simple Deploy
```
# build in repl
cp public/build/index.html docs.index.ttml
# git commit / push
```