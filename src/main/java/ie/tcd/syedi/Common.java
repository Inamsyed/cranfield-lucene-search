package ie.tcd.syedi;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

// Settings shared by the indexer and the searcher.
// Paths are relative to the project folder, so run programs from there.
public class Common {

    // The Cranfield files
    public static final String DOCS_FILE = "cranfield/cran.all.1400";
    public static final String QUERIES_FILE = "cranfield/cran.qry";
    public static final String QRELS_FILE = "cranfield/cranqrel";

    // Where the Lucene index will be saved
    public static final String INDEX_DIR = "index";
    // Which field the searcher looks in
    public static final String SEARCH_FIELD = "body";
    // How many results to return per query
    public static final int MAX_RESULTS = 50;
    // Where results files are saved
    public static final String RESULTS_DIR = "results";
    // Label for this experiment. Also used as the results file name.
    // CHANGE THIS whenever you change the analyzer or scoring, or the old file is
    // overwritten.
    public static final String RUN_NAME = "bm25_english";

    // The text cleaner. Indexer and searcher MUST use the same one.
    public static Analyzer createAnalyzer() {
        // return new StandardAnalyzer();
        return new EnglishAnalyzer();
    }

    // The scoring formula (BM25 for now; VSM later for comparison).
    public static Similarity createSimilarity() {
        return new BM25Similarity();
        // return new ClassicSimilarity();
        // return new LMDirichletSimilarity();
    }
}