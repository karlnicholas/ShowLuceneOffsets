import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

public class LuceneInvertedIndex {
    public static void main(String[] args) throws Exception {
        new LuceneInvertedIndex().run();
    }
    private void run() throws Exception {
        Directory directory = new RAMDirectory();
        // Builds an analyzer with the default stop words
        Analyzer analyzer = new StandardAnalyzer();

        // IndexWriter Configuration
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.CREATE);

        // IndexWriter writes new index files to the directory
        IndexWriter writer = new IndexWriter(directory, iwc);

        FieldType type = new FieldType();
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorOffsets(true);
        type.setIndexOptions(IndexOptions.DOCS);

        Field fieldStore = new Field("text", "The People acknowledge that they bear the burden.  The question here is what degree of proof is required.  We hold that proof beyond a reasonable doubt is required.", type);
        Document doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        fieldStore = new Field("text", "The defense cannot be faulted for failing.  Therefore, we must consider the question of prejudice.  We affirm the judgment of the Court of Appeal.", type);
        doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        fieldStore = new Field("text", "Contreras was sentenced to a term of 58 years to life.  We granted review to determine whether that violates the Eighth Amendment.  We hold that these sentences are unconstitutional.", type);
        doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        fieldStore = new Field("text", "It fails to establish either that defendant was convicted for theft.  The trial court properly denied defendant’s petition.  We modify the judgment of the Court of Appeal.", type);
        doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        fieldStore = new Field("text", "The question before us concerns defendants who were serving felony sentences.  Are such defendants entitled to automatic resentencing under Proposition 47?  We conclude that resentencing is available.", type);            
        doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        writer.close();
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        MatchAllDocsQuery query = new MatchAllDocsQuery();
        TopDocs hits = searcher.search(query, Integer.MAX_VALUE);

        Map<String, Set<String>> invertedIndex = new HashMap<>();

        for ( ScoreDoc scoreDoc: hits.scoreDocs ) {
            Fields termVs = reader.getTermVectors(scoreDoc.doc);
            Terms f = termVs.terms("text");
            TermsEnum te = f.iterator();
            PostingsEnum docsAndPosEnum = null;
            BytesRef bytesRef;
            while ( (bytesRef = te.next()) != null ) {
                docsAndPosEnum = te.postings(docsAndPosEnum, PostingsEnum.ALL);
                // for each term (iterator next) in this field (field)
                // iterate over the docs (should only be one)
                int nextDoc = docsAndPosEnum.nextDoc();
                assert nextDoc != DocIdSetIterator.NO_MORE_DOCS;
                final int p = docsAndPosEnum.nextPosition();
                String term = bytesRef.utf8ToString();
                if (invertedIndex.containsKey(term) ) {
                    Set<String> existingDocs = invertedIndex.get(term);
                    existingDocs.add((scoreDoc.doc+1)+":"+p);
                } else {
                    Set<String> docs = new TreeSet<>();
                    docs.add((scoreDoc.doc+1)+":"+p);
                    invertedIndex.put(term, docs);
                }
            }
        }
        invertedIndex.forEach((k,v)->{
            System.out.print(k+"\t[");
            v.forEach(va->System.out.print(va+", "));
            System.out.print("]");
            System.out.println();
        });
    }
}
