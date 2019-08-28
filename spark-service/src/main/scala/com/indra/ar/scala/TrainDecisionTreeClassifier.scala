package com.indra.ar.scala

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.functions._;
import org.apache.spark.sql.types._;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.SparkSession.Builder;

import scala.collection.JavaConversions._;

/**
 * Tweet Sentiment Decision Tree Classifier
 * Train a Decision Tree Classifier on a collection of pre-labelled tweets about airlines
 *
 * @author jillur.quddus
 * @version 0.0.1
 */

object TrainDecisionTreeClassifier {

  def main(args: Array[String]) = {

    /********************************************************************
     * SPARK CONTEXT
     ********************************************************************/

    // Create the Spark Context
    val conf = new SparkConf()
      .setAppName("Sentiment Models")
      .setMaster("spark://<Spark Master Hostname>:7077");

    val sc = new SparkContext(conf);
    val sparkSession = SparkSession.builder().getOrCreate();
    import sparkSession.implicits._;

    /********************************************************************
     * INGEST THE CORPUS
     ********************************************************************/

    // Define the CSV Dataset Schema
    val schema = new StructType(Array(
                    StructField("unit_id", LongType, true),
                    StructField("golden", BooleanType, true),
                    StructField("unit_state", StringType, true),
                    StructField("trusted_judgements", IntegerType, true),
                    StructField("last_judgement_at", StringType, true),
                    StructField("airline_sentiment", StringType, true),
                    StructField("airline_sentiment_confidence", DoubleType, true),
                    StructField("negative_reason", StringType, true),
                    StructField("negative_reason_confidence", DoubleType, true),
                    StructField("airline", StringType, true),
                    StructField("airline_sentiment_gold", StringType, true),
                    StructField("name", StringType, true),
                    StructField("negative_reason_gold", StringType, true),
                    StructField("retweet_count", IntegerType, true),
                    StructField("text", StringType, true),
                    StructField("tweet_coordinates", StringType, true),
                    StructField("tweet_created", StringType, true),
                    StructField("tweet_id", StringType, true),
                    StructField("tweet_location", StringType, true),
                    StructField("user_timezone", StringType, true)
                 ));

    // Read the CSV Dataset, keeping only those columns that we need to build our model
    var tweetsDF = SparkSession.builder().getOrCreate().read
                      .format("csv")
                      .option("header", true)
                      .option("delimiter", ",")
                      .option("mode", "DROPMALFORMED")
                      .schema(schema)
                      .load("hdfs://<Namenode Hostname>:9000/keisan/knowledgebase/spark/mllib/raw/Airline-Sentiment-2-w-AA.csv")
                      .select("airline_sentiment", "text");

    /********************************************************************
     * LABEL THE DATA
     ********************************************************************/

    // We are interested in detecting tweets with negative sentiment. Let us create a new column whereby
    // if the sentiment is negative, this new column is TRUE (Positive Outcome), and FALSE (Negative Outcome)
    // in all other cases

    tweetsDF = tweetsDF.withColumn("negative_sentiment_label", when(tweetsDF("airline_sentiment") === "negative", lit("true")).otherwise(lit("false")))
                .select("text", "negative_sentiment_label");

    /********************************************************************
     * APPLY THE PRE-PROCESSING PIPELINE
     ********************************************************************/

    // Let us now perform some simple pre-processing including converting the text column to lowercase
    // and removing all non-alphanumeric characters

    val lowercasedDF = PreProcessorUtils.lowercaseRemovePunctuation(tweetsDF, "text");

    // Lemmatize the text to generate a sequence of Lemmas using the Stanford NLP Library
    // By using mapPartitions, we create the Stanford NLP Pipeline once per partition rather than once per RDD entry

    val lemmatizedDF = lowercasedDF.select("text", "negative_sentiment_label").rdd.mapPartitions(p => {

      // Define the NLP Lemmatizer Pipeline once per partition
      val props = new Properties();
      props.put("annotators", "tokenize, ssplit, pos, lemma");
      val pipeline = new StanfordCoreNLP(props);

      // Lemmatize the text and preserve the Negative Sentiment Label
      p.map{
        case Row(text: String, negative_sentiment_label:String) => (PreProcessorUtils.lemmatizeText(text, pipeline), negative_sentiment_label);
      };

    }).toDF("lemmas", "negative_sentiment_label");

    // Remove Stop Words from the sequence of Lemmas
    val stopWordsRemovedDF = PreProcessorUtils.stopWordRemover(lemmatizedDF, "lemmas", "filtered_lemmas")
                                .where(size(col("filtered_lemmas")) > 1);

    /********************************************************************
     * SCALED FEATURE VECTOR
     ********************************************************************/

    // Generate the Scaled Feature Vectors
    val featurizedDF = ModelUtils.tfidf(stopWordsRemovedDF, "filtered_lemmas", "features");

    /********************************************************************
     * TRAIN AND EVALUATE A DECISION TREE CLASSIFIER
     ********************************************************************/

    // Split the data into Training and Test Datasets
    val Array(trainingDF, testDF) = featurizedDF.randomSplit(Array(0.7, 0.3))

    // Train a Decision Tree Model using the Training Dataset
    val decisionTreeModel = ModelUtils.trainDecisionTreeModel(featurizedDF, trainingDF, "negative_sentiment_label", "features");

    // Apply the Decision Tree Training Model to the Test Dataset
    val decisionTreePredictions = decisionTreeModel.transform(testDF);
    decisionTreePredictions.select("negative_sentiment_label", "predicted_label", "filtered_lemmas", "features").show(false);

    // Compute the accuracy of the Decision Tree Training Model on the Test Dataset
    val decisionTreeEvaluator = new MulticlassClassificationEvaluator()
                                  .setLabelCol("indexed_label")
                                  .setPredictionCol("prediction")
                                  .setMetricName("accuracy");
    val decisionTreeAccuracy = decisionTreeEvaluator.evaluate(decisionTreePredictions);
    println("Decision Tree Test Accuracy Rate = " + decisionTreeAccuracy);
    println("Decision Tree Test Error Rate = " + (1.0 - decisionTreeAccuracy));

    // Generate a Classification Matrix
    val metrics = ModelUtils.generateMulticlassMetrics(decisionTreePredictions, "prediction", "indexed_label");
    println(metrics.confusionMatrix);

    // Generate Label Accuracy Metrics
    val labelMetrics = metrics.labels;
    labelMetrics.foreach { l =>
      println(s"False Positive Rate ($l) = " + metrics.falsePositiveRate(l));
    }

    /********************************************************************
     * SAVE THE DECISION TREE CLASSIFIER FOR REAL-TIME STREAMING
     ********************************************************************/

    decisionTreeModel.save("hdfs://<Namenode Hostname>:9000/keisan/knowledgebase/spark/mllib/models/decisionTreeClassifier");

  }

}