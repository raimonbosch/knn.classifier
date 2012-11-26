package org.github.indexer.twentynews;

import org.github.classifier.utils.ShellUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.github.classifier.knn.KnnClassifier;
import org.xml.sax.SAXException;

public class TwentyNewsSolrIndexer
{
  int count = 1;
  Random random = new Random(System.currentTimeMillis());
  EmbeddedSolrServer server = null;
  CoreContainer coreContainer = null;
  Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

  public SolrInputDocument createSolrDocument(String category,
    String content, int isCategorized, int count) throws SAXException, IOException
  {
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", count);
    doc.addField("nominals", category);
    doc.addField("numerics", content);
    doc.addField("is_categorized", isCategorized);
    //System.out.println( doc.toString() );
    return doc;
  }

  public void initSolrIndex(String solrConfPath, String solrOutput) throws Exception
  {
    System.out.println("solrConfPath: " + solrConfPath);
    ShellUtils.exec("rm -rf " + solrOutput);
    ShellUtils.exec("mkdir -p " + solrOutput);
    ShellUtils.exec("cp -r " + solrConfPath + " " + solrOutput);
    System.setProperty("solr.solr.home", solrOutput);
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();

    coreContainer  = initializer.initialize();
    server = new EmbeddedSolrServer(coreContainer, "knn");
  }

  public void index(String inputPath, String solrConfPath, String solrOutput, String sfolds)
  {
    try{

      initSolrIndex(solrConfPath, solrOutput);
      int folds = (sfolds == null) ? KnnClassifier.FOLDS : Integer.parseInt(sfolds);

      File[] files = new File(inputPath).listFiles();
      for(File categoryRoot : files){
        String category = categoryRoot.getName();
        if(category.equals(".DS_Store")){
          continue;
        }
        System.out.println("category: " + category);
        for(File f : categoryRoot.listFiles()){
          if(f.getName().equals(".DS_Store")){
            continue;
          }
          
          //Indexing document -->
          
          //We do not categorize documents inside the fold
          Integer isCategorized = (count % folds == 0) ? 0 : 1;
          String content = ShellUtils.readFile(f.getAbsolutePath());
          
          SolrInputDocument doc = createSolrDocument(category, content, isCategorized, count);
          docs.add( doc );

          if(docs.size() > 50){
            server.add( docs, commitWithinMs );
            docs.clear();
            System.out.println(count);
          }

          count++;
        }
      }

      System.out.println("Commiting...");
      server.add(docs);
      server.commit();


      System.out.println("Optimizing...");
      server.optimize();

      if (coreContainer != null) {
        coreContainer.shutdown();
      }

      System.out.println("Indexing completed. Added " + count + " documents at " + solrOutput);

    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public static String SPACE = " ";
  public static String SEPARATOR = "#";
  public static int commitWithinMs = 5*60*1000;
}