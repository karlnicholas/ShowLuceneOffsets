import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

public class ShowLuceneInvertedIndex {
    public static void main(String[] args) throws Exception {
        new ShowLuceneInvertedIndex().run();
    }
    private void run() throws Exception {
        Directory directory = new RAMDirectory();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(directory, indexWriterConfig);

        FieldType type = new FieldType();
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorOffsets(true);
        type.setIndexOptions(IndexOptions.DOCS);

        Field fieldStore = new Field("text", "We hold that proof beyond a reasonable doubt is required.", type);
        Document doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        fieldStore = new Field("text", "We hold that proof requires reasoanble preponderance of the evidence.", type);
        doc = new Document();
        doc.add(fieldStore);
        writer.addDocument(doc);

        writer.close();
        
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        MatchAllDocsQuery query = new MatchAllDocsQuery();
        TopDocs hits = searcher.search(query, Integer.MAX_VALUE);

        Map<String, Set<String>> invertedIndex = new HashMap<>();
        BiFunction<Integer, Integer, Set<String>> mergeValue = 
            (docId, pos)-> {TreeSet<String> s = new TreeSet<>(); s.add((docId+1)+":"+pos); return s;};

        for ( ScoreDoc scoreDoc: hits.scoreDocs ) {
            Fields termVs = reader.getTermVectors(scoreDoc.doc);
            Terms terms = termVs.terms("text");
            TermsEnum termsIt = terms.iterator();
            PostingsEnum docsAndPosEnum = null;
            BytesRef bytesRef;
            while ( (bytesRef = termsIt.next()) != null ) {
                docsAndPosEnum = termsIt.postings(docsAndPosEnum, PostingsEnum.ALL);
                docsAndPosEnum.nextDoc();
                int pos = docsAndPosEnum.nextPosition();
                String term = bytesRef.utf8ToString();
                invertedIndex.merge(
                    term, 
                    mergeValue.apply(scoreDoc.doc, pos), 
                    (s1,s2)->{s1.addAll(s2); return s1;}
                );
            }
        }
        System.out.println( invertedIndex);
    }

}
