package org.github.indexer.arff;

import org.github.classifier.utils.ShellUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Random;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.github.classifier.knn.KnnClassifier;
import org.xml.sax.SAXException;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class ArffSolrIndexer
{
  int count = 1;
  int indexed = 0;
  Random random = new Random(System.currentTimeMillis());
  EmbeddedSolrServer server = null;
  CoreContainer coreContainer = null;
  Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
  StringBuilder nominals = new StringBuilder();
  StringBuilder numerics = new StringBuilder();

  public SolrInputDocument createSolrDocument(StringBuilder nominals,
    StringBuilder numerics, int isCategorized, int count) throws SAXException, IOException
  {
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField("id", count);
    
    for(String nominal : nominals.toString().split(SEPARATOR)){
      doc.addField("nominal", nominal);
    }

    doc.addField("nominals", nominals.subSequence(0, nominals.length()-1).toString());
    doc.addField("numerics", numerics.toString());
    doc.addField("is_categorized", isCategorized);

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

  public Instances readARFF(String inputPath) throws Exception
  {
    BufferedReader reader = new BufferedReader(new FileReader(inputPath));
    Instances data = new Instances(reader);
    reader.close();
    return data;
  }

  public void index(String inputPath, String solrConfPath, String solrOutput, String sfolds)
  {
    try{

      initSolrIndex(solrConfPath, solrOutput);
      Instances data = readARFF(inputPath);
      int folds = (sfolds == null) ? KnnClassifier.FOLDS : Integer.parseInt(sfolds);
      
      Enumeration<Instance> instances = data.enumerateInstances();
      while(instances.hasMoreElements()){

        Instance instance = (Instance)instances.nextElement();

        //We do not categorize documents inside the fold
        Integer isCategorized = (count % folds == 0) ? 0 : 1;
        
        //For each attribute we fill nominals and numerics
        Enumeration attributes = data.enumerateAttributes();
        while(attributes.hasMoreElements()){
          Attribute attribute = (Attribute)attributes.nextElement();
          if(instance.toString(attribute).equals("1")){
            if(attribute.isNumeric()){
              numerics.append(attribute.name());
              numerics.append(SPACE);
            }
            else if(attribute.isNominal()){
              nominals.append(attribute.name());
              nominals.append(SEPARATOR);
            }
          }
        }
       
        SolrInputDocument doc = createSolrDocument(nominals, numerics, isCategorized, count);
        docs.add( doc );
        System.out.println(indexed++);
        
        //server.add( createSolrDocument(nominals, numerics), commitWithinMs);
        //System.out.println(count++);
        nominals.delete(0, nominals.length());
        numerics.delete(0, numerics.length());

        if(docs.size() > 50){
          server.add( docs, commitWithinMs );
          docs.clear();
        }

        count++;
      }
      
      System.out.println("Commiting...");
      server.add(docs);
      server.commit();
      

      System.out.println("Optimizing...");
      server.optimize();

      if (coreContainer != null) {
        coreContainer.shutdown();
      }

      System.out.println("Indexing completed. Added " + indexed + " documents at " + solrOutput);

    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public static String SPACE = " ";
  public static String SEPARATOR = "#";
  public static int commitWithinMs = 5*60*1000;
}
