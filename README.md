solr.knn.classifier
===================

An example K-Nearest-Neighbors classifier built on top of Solr &amp; Lucene. Works with Weka ARFF formats.

You can try the program with this dataset:

     http://en.sourceforge.jp/projects/sfnet_meka/downloads/Datasets/IMDB-F.arff/


Clone the repo with github, and compile with maven by running:
 
    mvn clean install

(a target directory will be generated w/ a jar executable)

Create the index by executing:

     java -Xmx512m -Dfile.encoding=UTF-8 -jar target/knn-classifier-1.0-SNAPSHOT-jar-with-dependencies.jar \
     --action=index --input=/tmp/IMDB-F.arff --output=/tmp/solr --solrconf=./src/main/resources/solr/

Run your classification:

    java -Xmx512m -Dfile.encoding=UTF-8 -jar target/knn-classifier-1.0-SNAPSHOT-jar-with-dependencies.jar \
    --action=knn --input=/tmp/IMDB-F.arff --output=/tmp/solr --solrconf=./src/main/resources/solr/



Dependencies: maven2
TODO: Support for more input formats

