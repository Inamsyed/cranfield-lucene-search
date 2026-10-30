package ie.tcd.syedi;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Locale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Search {
    public static void main(String[] args) throws IOException {
        // Step 1: read cran.qry into a list of query texts
        List<String> queries = readQueries(Common.QUERIES_FILE);
        System.out.println("Queries read: " + queries.size());
        System.out.println();

        // Step 2: open the index ONCE, before the loop
        Directory directory = FSDirectory.open(Paths.get(Common.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(Common.createSimilarity());

        // The parser turns query text into a Lucene query, using the SAME analyzer as
        // the indexer
        Analyzer analyzer = Common.createAnalyzer();
        QueryParser parser = new QueryParser(Common.SEARCH_FIELD, analyzer);

        // Used to look up each hit's stored fields (its Cranfield ID and title)
        StoredFields storedFields = searcher.storedFields();

        // Create the results folder and open the results file for writing
        Files.createDirectories(Paths.get(Common.RESULTS_DIR));
        Path resultsFile = Paths.get(Common.RESULTS_DIR, Common.RUN_NAME + ".txt");
        PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultsFile));

        // Step 3: search every query
        int totalHits = 0;
        for (int i = 0; i < queries.size(); i++) {
            int queryNumber = i + 1;
            String queryText = queries.get(i);

            // Escape special characters, then parse
            Query query;
            try {
                query = parser.parse(QueryParser.escape(queryText));
            } catch (ParseException e) {
                System.out.println("Could not parse query " + queryNumber + ": " + e.getMessage());
                continue;
            }

            // Get the top results for this query
            ScoreDoc[] hits = searcher.search(query, Common.MAX_RESULTS).scoreDocs;
            totalHits += hits.length;

            // Write this query's hits to the results file
            writeResults(out, queryNumber, hits, storedFields);

            // Print the top 10 for query 1 only, to check by eye
            if (queryNumber == 1) {
                printTopHits(queryNumber, queryText, query, hits, storedFields, 10);
            }
        }

        // Close the results file (this is what makes sure everything is saved)
        out.close();

        System.out.println("Queries searched: " + queries.size());
        System.out.println("Total hits returned: " + totalHits);

        // Step 4: close everything
        analyzer.close();
        reader.close();
        directory.close();
    }

    // Reads cran.qry and returns the query texts, in file order.
    // A query's number is its position in this list + 1.
    static List<String> readQueries(String path) throws IOException {
        List<String> queries = new ArrayList<>();
        StringBuilder current = null; // the query being built right now

        for (String line : Files.readAllLines(Paths.get(path))) {
            String marker = line.trim();

            if (marker.startsWith(".I")) {
                // A new query starts: save the previous one first
                if (current != null) {
                    queries.add(current.toString().trim());
                }
                current = new StringBuilder();
            } else if (marker.equals(".W")) {
                // The query text starts on the next line: nothing to do here
            } else if (current != null) {
                // A normal text line: add it to the current query
                current.append(line).append(' ');
            }
        }

        // The last query has no ".I" after it, so save it here
        if (current != null) {
            queries.add(current.toString().trim());
        }

        return queries;
    }

    // Writes one query's hits to the results file in TREC format:
    // query_number Q0 doc_id rank score run_name
    static void writeResults(PrintWriter out, int queryNumber, ScoreDoc[] hits,
            StoredFields storedFields) throws IOException {
        for (int rank = 1; rank <= hits.length; rank++) {
            ScoreDoc hit = hits[rank - 1];
            String id = storedFields.document(hit.doc).get("id");
            out.printf(Locale.ROOT, "%d Q0 %s %d %.4f %s%n",
                    queryNumber, id, rank, hit.score, Common.RUN_NAME);
        }
    }

    // Prints the top few hits for one query: rank, Cranfield ID, score and title.
    static void printTopHits(int queryNumber, String queryText, Query query,
            ScoreDoc[] hits, StoredFields storedFields, int howMany) throws IOException {
        System.out.println("Query " + queryNumber + ": " + queryText);
        System.out.println("Parsed as: " + query);
        System.out.println();

        for (int rank = 1; rank <= Math.min(howMany, hits.length); rank++) {
            ScoreDoc hit = hits[rank - 1];
            Document doc = storedFields.document(hit.doc);
            String id = doc.get("id");
            String title = doc.get("title").trim();
            System.out.printf("%2d) id=%-5s score=%.3f  %s%n", rank, id, hit.score, title);
        }
        System.out.println();
    }
}
