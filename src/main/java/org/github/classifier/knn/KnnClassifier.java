package org.github.classifier.knn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.search.SolrIndexSearcher;
import weka.core.Instances;

public class KnnClassifier
{
  int count = 0;
  int classified = 0;
  int totalNominals = 0;

  EmbeddedSolrServer server = null;
  CoreContainer coreContainer = null;
  Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
  
  Map<String, Float> nominalProbability = new TreeMap<String,Float>();
  //Map<String, Integer> nominalTf = new TreeMap<String,Integer>();

  public void readSolrIndex(String solrOutput) throws Exception
  {
    System.setProperty("solr.solr.home", solrOutput);
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();

    coreContainer  = initializer.initialize();
    server = new EmbeddedSolrServer(coreContainer, "knn");
  }

  public Instances readARFF(String inputPath) throws Exception
  {
    BufferedReader reader = new BufferedReader(new FileReader(inputPath));
    Instances data = new Instances(reader);
    reader.close();
    return data;
  }

  public List<String> argsMax(Map<String,Float> map)
  {
    float max = Float.MIN_VALUE;
    for(String key : map.keySet()){
      float value = map.get(key);
      if( value > max){
        max = value;
      }
    }

    List<String> argsMax = new ArrayList<String>();
    for(String key : map.keySet()){
      float value = map.get(key);
      if( value == max){ argsMax.add(key); }
    }

    return argsMax;
  }

  public void classify(String inputPath, String solrOutput, String sfolds, String stopTerms)
  {
    try{

      int totalMatches = 0;
      readSolrIndex(solrOutput);
      int folds = (sfolds == null) ? KnnClassifier.FOLDS : Integer.parseInt(sfolds);
      int topTerms = (stopTerms == null) ? KnnClassifier.TOP_TERMS : Integer.parseInt(stopTerms);
      SolrIndexSearcher searcher = coreContainer.getCore("knn").getSearcher().get();
      IndexReader reader = searcher.getIndexReader();

      System.out.println("folds: " + folds);
      System.out.println("topTerms: " + topTerms);

      for(int docID = 1; docID < reader.maxDoc(); docID++){

        Document doc = reader.document(docID);
        Integer id = doc.getField("id").numericValue().intValue();
        Integer isCategorized = doc.getField("is_categorized").numericValue().intValue();
        String sNominals = doc.getField("nominals").stringValue();
        String nominals[] = sNominals.split("#");
        
        //We do not classify documents outside the fold
        if(isCategorized == 1){
          count++;
          continue;
        }

        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/mlt");
        query.setQuery( "id:" + docID );
        query.setParam("fq", "is_categorized:1");
        query.setRows(topTerms);
        QueryResponse qr = server.query(query);
        
        Iterator<SolrDocument> results = qr.getResults().iterator();

        int i = 0;
        while(results.hasNext()){
          SolrDocument solrDoc = results.next();

          System.out.println("Found(" +
            solrDoc.getFieldValue("id") + ")=" +
            solrDoc.getFieldValue("nominals") + "; C=<" +
            solrDoc.getFieldValue("is_categorized") +  ">");
          
          for(String match : ((String)solrDoc.getFieldValue("nominals")).split("#") ){
            
            Float nP = nominalProbability.get((String)match);
            if(nP == null){ nP = 0.0f; }
            nP += 1.0f;

            nominalProbability.put((String)match, nP);
          }
          
          i++;
        }

        List<String> selectedCategories = argsMax(nominalProbability);
        boolean categoryMatches = false;
        for(String nominal : nominals){
          for(String selectedCategory : selectedCategories){
            if(nominal.equals(selectedCategory)){
              System.out.println(selectedCategory + " matches!");
              categoryMatches = true;
            }
          }
        }

        if(categoryMatches){
          totalMatches++;
        }
        
        System.out.println("Original(" + id + ")=" + sNominals);
        System.out.println(nominalProbability);
        System.out.println(argsMax(nominalProbability).toString());
        System.out.println(categoryMatches);
        
        nominalProbability.clear();
        count++;
        classified++;
        System.out.println(classified);
      }


      if (coreContainer != null) {
        coreContainer.shutdown();
      }

      System.out.println("Total matches " + totalMatches + " out of " + classified);
      Float accuracy = (float)totalMatches/(float)classified;
      System.out.println("Accuracy is " +  accuracy);

    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public static int FOLDS = 10;
  public static int TOP_TERMS = 4;
  public static String SPACE = " ";
  public static String SEPARATOR = "#";
  public static int commitWithinMs = 5*60*1000;
}