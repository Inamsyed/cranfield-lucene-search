# Cranfield Lucene Search

A search engine for the [Cranfield collection](http://ir.dcs.gla.ac.uk/resources/test_collections/cran/) built with Apache Lucene 10. It indexes 1,400 aeronautical abstracts, runs 225 test queries, and compares analyzers and scoring models using `trec_eval`.


## Results

Each run returns the top 50 documents for each of the 225 queries. Relevance grades of −1 ("no interest") are treated as non-relevant.

| Analyzer | Scoring | MAP | Recall@50 | P@5 | P@10 | nDCG |
| --- | --- | --- | --- | --- | --- | --- |
| Standard | BM25 | 0.254 | 0.590 | 0.294 | 0.216 | 0.412 |
| Standard | VSM | 0.252 | 0.581 | 0.289 | 0.208 | 0.407 |
| **English** | **BM25** | **0.281** | 0.628 | 0.311 | **0.228** | 0.440 |
| English | VSM | 0.280 | **0.646** | **0.314** | 0.225 | **0.444** |
| English | LM (Dirichlet) | 0.220 | 0.567 | 0.235 | 0.184 | 0.373 |

**Main findings**

- **The analyzer mattered most.** EnglishAnalyzer (stop-word removal and Porter stemming) raised MAP by 0.027 for both BM25 and VSM.
- **BM25 and VSM were practically tied** (MAP within 0.002). Cranfield's short, similar-length abstracts give BM25's term-frequency saturation and length normalisation little to work with.
- **The language model was weakest** with Lucene's default smoothing (μ = 2000), which is large compared with abstracts of fewer than 200 words.

Full `trec_eval` output for every run is in [`evaluation/`](evaluation/).

## How it works

1. **Parse:** `cran.all.1400` is read line by line and split into 1,400 documents using its `.I`, `.T`, `.A`, `.B` and `.W` markers.
2. **Index:** each document is stored with its id (exact, unanalysed), title, author and abstract.
3. **Search:** each query from `cran.qry` is numbered 1–225 by position, escaped, parsed and run against the index. The top 50 results are written in TREC format.
4. **Evaluate:** `trec_eval` compares the results with the relevance judgements.

The analyzer and scoring model are set in one place (`Common.java`), so indexing and searching always use the same configuration.

## Project structure

```
src/main/java/ie/tcd/syedi/
├── Common.java        Shared settings: file paths, analyzer, scoring model, run name
├── CranfieldDoc.java  One Cranfield document (id, title, author, source, body)
├── Index.java         Parses cran.all.1400 and builds the Lucene index
└── Search.java        Runs the 225 queries and writes a TREC results file
evaluation/            trec_eval output for each run
```

## Running it

**Requirements:** Java 21, Maven 3.9+, and `gcc` and `make` (to build `trec_eval`). Developed on Ubuntu 20.04.

**1. Download the Cranfield collection**

```bash
mkdir cranfield && cd cranfield
wget http://ir.dcs.gla.ac.uk/resources/test_collections/cran/cran.tar.gz
tar -xzf cran.tar.gz
cd ..
```

**2. Build the index and run the queries**

```bash
mvn compile exec:java -Dexec.mainClass=ie.tcd.syedi.Index
mvn compile exec:java -Dexec.mainClass=ie.tcd.syedi.Search
```

This writes `results/<run name>.txt`.

**3. Build trec_eval**

```bash
wget https://trec.nist.gov/trec_eval/trec_eval-9.0.7.tar.gz
tar -xzf trec_eval-9.0.7.tar.gz
cd trec_eval-9.0.7 && make && cd ..
```

**4. Convert the relevance judgements**

`cranqrel` grades relevance from 1 (most relevant) to 4, with −1 meaning no interest. `trec_eval` needs four columns and higher grades for more relevant documents:

```bash
awk '{ g = ($3 == -1) ? 5 : $3; print $1, 0, $2, 5 - g }' cranfield/cranqrel > cranfield/cranqrel.trec
```

**5. Evaluate**

```bash
./trec_eval-9.0.7/trec_eval -m all_trec cranfield/cranqrel.trec results/bm25_english.txt
```

## Trying other configurations

Edit `Common.java`:

- `createAnalyzer()`: `StandardAnalyzer` or `EnglishAnalyzer`
- `createSimilarity()`: `BM25Similarity`, `ClassicSimilarity` (VSM) or `LMDirichletSimilarity`
- `RUN_NAME`: change it for each run so earlier results aren't overwritten

Re-run `Index` after changing the analyzer, then `Search`.

## Built with

- [Apache Lucene 10.5.0](https://lucene.apache.org)
- [trec_eval 9.0.7](https://trec.nist.gov/trec_eval/) (NIST)
- [Cranfield collection](http://ir.dcs.gla.ac.uk/resources/test_collections/cran/) (University of Glasgow)