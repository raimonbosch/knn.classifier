package org.github.classifier.knn;

import org.github.classifier.utils.CommandLineHelper;
import org.apache.commons.cli.ParseException;
import org.github.indexer.arff.ArffSolrIndexer;
import org.github.indexer.twentynews.TwentyNewsSolrIndexer;

public class EntryPoint
{
  public static void main(String[] args) throws ParseException
  {
    CommandLineHelper helper = new CommandLineHelper(args);

    helper.setRequiredOption("action", "action", true, "action");
    helper.setRequiredOption("input", "input", true, "input");
    helper.setRequiredOption("output", "output", true, "output");
    helper.setOption("solrconf", "solrconf", true, "solrconf");
    helper.setOption("folds", "folds", true, "folds");
    helper.setOption("topTerms", "topTerms", true, "topTerms");
    //helper.setOption("contributor", "contributor", true, "contributor");
    helper.parsePosix();

    String action = helper.getOptionValue("action");
    String inputPath = helper.getOptionValue("input");
    String solrConfPath = helper.getOptionValue("solrconf");
    String solrOutput = helper.getOptionValue("output");
    String folds = helper.getOptionValue("folds");
    String topTerms = helper.getOptionValue("topTerms");

    if(action.equals("index")){
      if(inputPath.contains(".arff")){
        ArffSolrIndexer indexer = new ArffSolrIndexer();
        indexer.index(inputPath, solrConfPath, solrOutput, folds);
      }
      else if(inputPath.contains("20news")){
        TwentyNewsSolrIndexer indexer = new TwentyNewsSolrIndexer();
        indexer.index(inputPath, solrConfPath, solrOutput, folds);
      }
      else{
        System.out.println("Cannot understand the format at '" + inputPath + "'");
      }
    }
    else if(action.equals("knn")){
      KnnClassifier knn = new KnnClassifier();
      knn.classify(inputPath, solrOutput, folds, topTerms);
    }
  }
}
