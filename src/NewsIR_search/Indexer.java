/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NewsIR_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//import edu.stanford.nlp.process.DocumentPreprocessor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author dwaipayan
 */
public class Indexer extends AnalyzerClass {
//    Properties  prop;               // prop of the init.properties file

    String collPath;           // path of the collection
    String collSpecPath;       // path of the collection spec file
    File collDir;            // collection Directory
    File indexFile;          // place where the index will be stored
    IndexWriter indexWriter;
    boolean boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean boolIndexFromSpec;  // true; false if indexing from collPath
    int docIndexedCounter;  // document indexed counter
    boolean boolDumpIndex;      // true if want ot dump the entire collection
    String dumpPath;           // path of the file in which the dumping to be done

    int tweet_starts_from_date;
    int tweet_ends_from_date;

    String[] index_fields;
    String[] index_fields_with_analyze;

    public String[] INDEX_FIELDS;
    static public String FIELD_ID = "DOCNO";

    public Indexer() throws IOException, ClassCastException, ClassNotFoundException {

        GetProjetBaseDirAndSetProjectPropFile setPropFile = new GetProjetBaseDirAndSetProjectPropFile();
        prop = setPropFile.prop;
        tweet_starts_from_date = Integer.parseInt(prop.getProperty("tweet.starts.from.date", "20"));
        tweet_ends_from_date = Integer.parseInt(prop.getProperty("tweet.ends.from.date", "29"));

        String tmp = prop.getProperty("index_fields", "null");
        if (!tmp.contentEquals("null")) {
            index_fields = new String[tmp.split(",").length];
            for (int i = 0; i < tmp.split(",").length; i++) {
                index_fields[i] = tmp.split(",")[i];
            }
        }

        tmp = prop.getProperty("index_fields_with_analyze", "null");
        if (!tmp.contentEquals("null")) {
            index_fields_with_analyze = new String[tmp.split(",").length];
            for (int i = 0; i < tmp.split(",").length; i++) {
                index_fields_with_analyze[i] = tmp.split(",")[i];
            }
        }

        INDEX_FIELDS = new String[index_fields.length];

        setAnalyzer();

        /* property files are loaded */

        /* collection path setting */
        if (prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
        } else if (prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" + collDir.getAbsolutePath() + "' does not exist or is not readable");
                System.exit(1);
            }
        } else {
            System.err.println("Neither collPath not collSpec is present");
            System.exit(1);
        }
        /* collection path set */

        String indexBasePath = prop.getProperty("indexPath");
        System.err.println(indexBasePath);
        if (indexBasePath.endsWith("/")); else {
            indexBasePath = indexBasePath + "/";
        }
        System.err.println(indexBasePath);
        /* index path setting */
        indexFile = new File(indexBasePath);
        Directory indexDir = FSDirectory.open(indexFile);

        /* index path set */
        if (DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index exists in " + indexFile.getAbsolutePath());
            boolIndexExists = true;
        } else {
            System.out.println("Will create the index in: " + indexFile.getName());
            boolIndexExists = false;
            /* Create a new index in the directory, removing any previous indexed documents */
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            /*  */
            indexWriter = new IndexWriter(indexDir, iwcfg);

        }

        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex", "false"));
        if (boolIndexExists == true && boolDumpIndex == true) {
            dumpPath = prop.getProperty("dumpPath");
        }
    }

    private void indexDirectory(File collDir) throws Exception {
        File[] files = collDir.listFiles();
        //System.out.println("Indexing directory: "+files.length);
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory: " + f.getName());
                indexDirectory(f);  // recurse
            } else {
                System.out.println((docIndexedCounter + 1) + ": Indexing file: " + f.getName());
                indexFile(f);
                docIndexedCounter++;
                --docIndexedCounter;
            }
        }
    }

    public Document constructDoc(String rawtext) throws IOException {
        /*
         id: Unique document identifier
         content: Total content of the document
         */

        Document doc = new Document();
        String tmp;

        tmp = prop.getProperty("index_fields", "null");
        if (!tmp.contentEquals("null")) {
            for (int i = 0; i < tmp.split(",").length; i++) {
                int flag = 0;
                for (int j = 0; j < index_fields_with_analyze.length; j++) {
                    if (index_fields[i].contentEquals(index_fields_with_analyze[j])) {
                        flag = 1;
                        break;
                    }
                }

                if (flag == 1) {
                    doc.add(new Field(index_fields[i], INDEX_FIELDS[i], Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
                } else {
                    doc.add(new Field(index_fields[i], INDEX_FIELDS[i], Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
                }
            }
        }

        // this is a extra field
        doc.add(new Field("rawtext", rawtext, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
//        
        return doc;
    }

    void indexFile(File collFile) throws Exception {

        Document doc;

        String docType = prop.getProperty("docType");

        if (docType.equalsIgnoreCase("trec")) {
            try {
                TrecDocIterator docElts = new TrecDocIterator(collFile);

                Document docElt;

                while (docElts.hasNext()) {
                    docElt = docElts.next();

                    if (docElt == null) {
                        System.out.println("docElt null");
                        break;
                    }

                    String tmp;
                    int i = 0;
                    tmp = prop.getProperty("index_fields", "null");
                    if (!tmp.contentEquals("null")) {
                        for (i = 0; i < tmp.split(",").length; i++) {
                           // System.out.println(index_fields[i]+" ---> "+docElt.getField(index_fields[i]).stringValue());
                            INDEX_FIELDS[i] = docElt.getField(index_fields[i]).stringValue();
                        }
                    }

                    String DOCNOElt = docElt.getField("DOCNO").stringValue();
                    FIELD_ID = DOCNOElt;
                    String TEXTElt = docElt.getField("TEXT").stringValue();
                  
                    doc = constructDoc(TEXTElt);

                    indexWriter.addDocument(doc);
                    System.out.println(DOCNOElt);
                    docIndexedCounter++;

            }
        }
catch (FileNotFoundException ex) {
            System.err.println("Error: '"+collFile.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        }catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+collFile.getAbsolutePath()+"'");
            ex.printStackTrace();
        }
        }
    }
    /**
     *
     * @throws Exception sds fdf
     */
    public void createIndex() throws Exception{
         for(int i=tweet_starts_from_date;i<=tweet_ends_from_date;i++)
                {
                int j=i-tweet_starts_from_date;
                if (indexWriter == null ) {
                    System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
                    return;
                    }
                }
        System.out.println("Indexing started");
        //System.out.println(prop.containsKey("collSpec"));

        if (boolIndexFromSpec) {
            /* if collectiomSpec is present, then index from the spec file*/
            String specPath = prop.getProperty("collSpec");
            System.out.println("Reading from spec file at: "+specPath);
            try (BufferedReader br = new BufferedReader(new FileReader(specPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                   indexFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                indexDirectory(collDir);
            else
                indexFile(collDir);
        }
        for(int i=tweet_starts_from_date;i<=tweet_ends_from_date;i++)
                {
                    int j=i-tweet_starts_from_date;
                    indexWriter.close();
                }
        System.out.println("Indexing ends");
        System.out.println(docIndexedCounter + " files indexed");
    }

//    public void dumpIndex() {
//        System.out.println("Dumping the index in: "+ dumpPath);
//        File f = new File(dumpPath);
//        if (f.exists()) {
//            System.out.println("Dump existed.");
//            System.out.println("Last modified: "+f.lastModified());
//            System.out.println("Overwrite(Y/N)?");
//            Scanner reader = new Scanner(System.in);
//            char c = reader.next().charAt(0);
//            if(c == 'N' || c == 'n')
//                return;
//            else
//                System.out.println("Dumping...");
//        }
//        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
//            FileWriter dumpFW = new FileWriter(dumpPath);
//            int maxDoc = reader.maxDoc();
//            for (int i = 0; i < maxDoc; i++) {
//                Document d = reader.document(i);
//                //System.out.print(d.get(FIELD_BOW) + " ");
//                dumpFW.write(d.get(FIELD_TEXT) + " ");
//            }
//            System.out.println("Index dumped in: " + dumpPath);
//            dumpFW.close();
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//        }
//    }


    public static void main(String args[]) throws IOException, Exception {

        CalculateTotalRunTime crt=new CalculateTotalRunTime();
        Indexer indexer = new Indexer();

        if(indexer.boolIndexExists==false) {
            indexer.createIndex();
            indexer.boolIndexExists = true;
        }
        
//        Indexer2ndLevel i2l;
//        if(indexer.prop.getProperty("pos.tag","false").toLowerCase().contentEquals("true") ||
//          indexer.prop.getProperty("ner.classify","false").toLowerCase().contentEquals("true"))
//        {
//            i2l = new Indexer2ndLevel();
//            i2l.createIndex2ndlevel();
//        }
//        
//        if(indexer.boolIndexExists == true && indexer.boolDumpIndex == true) {
//            indexer.dumpIndex();
//        }
        crt.PrintRunTime();
    }

}
