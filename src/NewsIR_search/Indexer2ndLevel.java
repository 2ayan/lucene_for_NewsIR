/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NewsIR_search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author ayan
 */
public class Indexer2ndLevel extends AnalyzerClass {

    boolean boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean boolIndexFromSpec;  // true; false if indexing from collPath
    int docIndexedCounter;  // document indexed counter
    File indexFile4reader;          // place where the index will be stored
    Directory indexDir4reader;
    IndexReader indexReader;
    IndexWriter indexWriter;
    File indexFile4writer;          // place where the index will be stored

    boolean boolDumpIndex;      // true if want ot dump the entire collection
    String dumpPath;

    int bulkTagNo;

    NERtag nertag;
    POStag postag;

    Field fieldPOS, fieldNER;

    String[] index_fields;
    String[] index_fields_with_analyze;
    String[] INDEX_FIELDS;

    public Indexer2ndLevel() throws IOException, ClassCastException, ClassNotFoundException {

        GetProjetBaseDirAndSetProjectPropFile setPropFile = new GetProjetBaseDirAndSetProjectPropFile();
        prop = setPropFile.prop;

        // if(prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true"))
        postag = new POStag();
        //if(prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
        nertag = new NERtag();
        setAnalyzer();
        bulkTagNo = 200;

        String indexBasePath = prop.getProperty("indexPath");

        if (indexBasePath.endsWith("/")); else {
            indexBasePath = indexBasePath + "/";
        }
        System.err.println(indexBasePath);

        /* index path setting */
        indexFile4reader = new File(indexBasePath);
        indexDir4reader = FSDirectory.open(indexFile4reader);
        indexReader = DirectoryReader.open(indexDir4reader);

        String tmp = new File(indexBasePath).getParentFile().toPath().toString().concat("/2ndLevel/");
        System.err.println("tmp=" + tmp);
        indexFile4writer = new File(tmp);
        Directory indexDir4writer = FSDirectory.open(indexFile4writer);

        /* index path set */
        if (DirectoryReader.indexExists(indexDir4reader)) {
            System.err.println("Index exists in " + indexFile4reader.getAbsolutePath());

            System.out.println("Will create the index in: " + indexFile4writer.getName());
            //boolIndexExists = false;
            boolIndexExists = true;
            /* Create a new index in the directory, removing any previous indexed documents */
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            /*  */
            indexWriter = new IndexWriter(indexDir4writer, iwcfg);
            System.err.println("Index writer created");
        } else {
            System.err.println("Index Does not exists index in " + indexFile4reader.getAbsolutePath());
            boolIndexExists = false;
            return;
        }

        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex", "false"));
        if (boolIndexExists == true && boolDumpIndex == true) {
            dumpPath = prop.getProperty("dumpPath");
        }
    }

    public void createIndex2ndlevel() throws Exception {

        if (indexWriter == null) {
            System.err.println("Index already exists at " + indexFile4writer.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");

        IndexIndex();

        indexWriter.close();

        System.out.println("Indexing ends");
        System.out.println(docIndexedCounter + " files indexed");
    }

    public void IndexIndex() throws IOException {
        Document d1;
        int docnoinIndex = (int) indexReader.numDocs();
        System.err.println(docnoinIndex);

        String tmp = prop.getProperty("index_fields", "null");
        INDEX_FIELDS = new String[tmp.split(",").length + 2];

        String[] toTag = new String[bulkTagNo];
        String[] nerTag = new String[bulkTagNo];
        String[] posTag = new String[bulkTagNo];
        int count = 0;

        for (int c = 0; c < docnoinIndex; c++) {
            if ((c % bulkTagNo == 0 || (c + 1) == docnoinIndex) && c > 0) {

                System.out.println("Starting to NER Tag..");
                nerTag = nertag.NERtagStringArray(toTag);
                System.out.println("NER Tag ended.");
                System.out.println("Starting to POS Tag..");
                posTag = postag.POStagStringArray(toTag);
                System.out.println("POS Tag ended.");

                int tostart = 0;
                if ((c + 1) < docnoinIndex) {
                    tostart = c - bulkTagNo;
                } else {
                    tostart = docnoinIndex % bulkTagNo;
                    tostart = docnoinIndex - tostart;
                    c++;
                }
                for (int dc = tostart; dc < c; dc++) {
                    Document d = indexReader.document(dc);

                    int i = 0;
                    if (!tmp.contentEquals("null")) {
                        index_fields = new String[tmp.split(",").length + 2];
                        for (i = 0; i < tmp.split(",").length; i++) {
                            index_fields[i] = tmp.split(",")[i];
                            INDEX_FIELDS[i] = d.get(index_fields[i]);
                        }
                    }

                    //System.out.println(toTag[dc-tostart]+"\nc===============\n"+nerTag[dc-tostart]);
                    index_fields[i] = "POStag";
                    INDEX_FIELDS[i++] = nerTag[dc - tostart];
                    index_fields[i] = "NERtag";
                    INDEX_FIELDS[i++] = posTag[dc - tostart];

                    d1 = this.constructDoc();
                   //System.err.println(dc+"\n"+nerTag[dc - tostart]+"\n"+posTag[dc - tostart]);

                    indexWriter.addDocument(d1);
                    //System.err.println(d1.get("DOCNO"));
                }
                System.out.println(c + " documents indexed in ");
                for (int i1 = 0; i1 < bulkTagNo; i1++) {
                    toTag[i1] = "";
                }
                count = 0;
                if ((c + 1) < docnoinIndex) {
                    toTag[count++] = indexReader.document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", "");
                }

            } else {
                toTag[count++] = indexReader.document(c).get("rawtext").replaceAll("\n", "").replaceAll("\r", "");
                // System.err.println(toTag[count-1]);

            }
        }
    }

    public Document constructDoc() throws IOException {

        Document doc = new Document();

        byte b[] = INDEX_FIELDS[INDEX_FIELDS.length - 2].getBytes("UTF-8");
        if (b.length >= 32766) // lucene can hold max of length 32766 in a string. 
        {
            INDEX_FIELDS[INDEX_FIELDS.length - 2] = INDEX_FIELDS[INDEX_FIELDS.length - 2].substring(0, 16000);
            INDEX_FIELDS[INDEX_FIELDS.length - 2] = INDEX_FIELDS[INDEX_FIELDS.length - 2].substring(0,INDEX_FIELDS[INDEX_FIELDS.length - 2].lastIndexOf(" "));
        }
        byte b1[]=INDEX_FIELDS[INDEX_FIELDS.length - 1].getBytes("UTF-8");
        if (b1.length >= 32766) // lucene can hold max of length 32766 in a string. 
        {
            INDEX_FIELDS[INDEX_FIELDS.length - 1] = INDEX_FIELDS[INDEX_FIELDS.length - 1].substring(0, 16000);
            INDEX_FIELDS[INDEX_FIELDS.length - 1] = INDEX_FIELDS[INDEX_FIELDS.length - 1].substring(0,INDEX_FIELDS[INDEX_FIELDS.length - 1].lastIndexOf(" "));
        }

        for (int i = 0; i < INDEX_FIELDS.length; i++) {
            doc.add(new Field(index_fields[i], INDEX_FIELDS[i],
                    Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        }

        return doc;
    }

    public static void main(String[] args) throws IOException, ClassCastException, ClassNotFoundException, Exception {
        CalculateTotalRunTime crt = new CalculateTotalRunTime();
        Indexer2ndLevel i2l = new Indexer2ndLevel();

        if (i2l.boolIndexExists == true) {
            i2l.createIndex2ndlevel();
            i2l.boolIndexExists = false;
        }
        crt.PrintRunTime();
    }

}
