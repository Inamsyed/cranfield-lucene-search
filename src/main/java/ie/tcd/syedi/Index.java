package ie.tcd.syedi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Index {
    public static void main(String[] args) throws IOException {

        // Step 1: read cran.all.1400 into a list (block 2a)
        List<CranfieldDoc> docs = readDocuments(Common.DOCS_FILE);
        System.out.println("Documents read: " + docs.size());

        // Step 2: put every document into the Lucene index (block 2b)
        buildIndex(docs);

        // Step 3: check the index really holds them all
        checkIndex();
    }

    // Reads cran.all.1400 and splits it into a list of documents.
    static List<CranfieldDoc> readDocuments(String path) throws IOException {
        List<CranfieldDoc> docs = new ArrayList<>();

        CranfieldDoc current = null; // the document being built right now
        StringBuilder section = null; // the "pile" new lines are added to

        for (String line : Files.readAllLines(Paths.get(path))) {
            String marker = line.trim();

            if (marker.startsWith(".I ")) {
                // A new document starts: save the previous one first
                if (current != null) {
                    docs.add(current);
                }
                String id = marker.substring(3).trim();
                current = new CranfieldDoc(id);
                section = null;
            } else if (marker.equals(".T")) {
                section = current.title;
            } else if (marker.equals(".A")) {
                section = current.author;
            } else if (marker.equals(".B")) {
                section = current.source;
            } else if (marker.equals(".W")) {
                section = current.body;
            } else if (section != null) {
                // A normal text line: add it to the current pile
                section.append(line).append(' ');
            }
        }

        // The last document has no ".I" after it, so save it here
        if (current != null) {
            docs.add(current);
        }

        return docs;
    }

    // Creates a fresh index and adds every Cranfield document to it.
    static void buildIndex(List<CranfieldDoc> docs) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(Common.INDEX_DIR));
        Analyzer analyzer = Common.createAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        config.setSimilarity(Common.createSimilarity());

        IndexWriter writer = new IndexWriter(directory, config);

        for (CranfieldDoc cranDoc : docs) {
            writer.addDocument(toLuceneDocument(cranDoc));
        }

        writer.close();
        analyzer.close();
        directory.close();
    }

    // Copies one CranfieldDoc into a Lucene Document.
    static Document toLuceneDocument(CranfieldDoc cranDoc) {
        Document doc = new Document();
        doc.add(new StringField("id", cranDoc.id, Field.Store.YES));
        doc.add(new TextField("title", cranDoc.title.toString(), Field.Store.YES));
        doc.add(new TextField("author", cranDoc.author.toString(), Field.Store.YES));
        doc.add(new TextField("body", cranDoc.body.toString(), Field.Store.YES));
        return doc;
    }

    // Opens the finished index and prints how many documents it holds.
    static void checkIndex() throws IOException {
        Directory directory = FSDirectory.open(Paths.get(Common.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        System.out.println("Documents in index: " + reader.numDocs());
        reader.close();
        directory.close();
    }
}