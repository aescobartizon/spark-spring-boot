package com.indra.ar.scala

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.functions._;
import org.apache.spark.sql.types._;
import org.apache.spark.sql.Row;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.classification.DecisionTreeClassifier;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;

import scala.collection.JavaConversions._;

/**
 * Predictive Model Utilities / Helper Functions
 * A collection of functions to build Predictive Models
 *
 * @author jillur.quddus
 * @version 0.0.1
 */

object ModelUtils {

  // Size of the fixed-length Feature Vectors
  val numFeatures = 4096;

  // Number of trees in our random forests
  val numTrees = 256;

  /**
   * Term Frequency-Inverse Document Frequency (TF-IDF)
   * Generate Term Frequency Feature Vectors by passing the sequence of lemmas to the HashingTF Transformer.
   * Fit the IDF Estimator to the Featurized Dataset to generate the IDFModel.
   * Pass the TF Feature Vectors to the IDFModel to scale based on frequency across the corpus
   *
   * @param corpus Dataset containing the sequence of lemmas
   * @param inputColumn The name of the column containing the sequence of (filtered) lemmas
   * @param outputColumn The name of the column to store the Scaled Feature Vectors
   * @return A DataFrame with the Scaled Feature Vectors
   *
   */

  def tfidf(corpus:Dataset[Row], inputColumn:String, outputColumn:String): Dataset[Row] = {

    // Convert the sequence of Lemmas into fixed-length feature vectors using the HashingTF Transformer
    val hashingTF = new HashingTF()
                      .setInputCol(inputColumn)
                      .setOutputCol("raw_features")
                      .setNumFeatures(numFeatures);
    val featurizedData = hashingTF.transform(corpus);

    // Takes the feature vectors and scale each column based on how frequently it appears in the corpus
    val idf = new IDF().setInputCol("raw_features").setOutputCol(outputColumn);
    val idfModel = idf.fit(featurizedData);
    return idfModel.transform(featurizedData);

  }

  /**
   * Build a Decision Tree Classifier
   * Train a Decision Tree Model by supplying the training dataset that includes the label and feature vector columns
   *
   * @param featuresDF The full DataFrame containing the labels and feature vectors
   * @param trainingDF The training split DataFrame to be used to train the Model
   * @param labelColumn The name of the column containing the labels
   * @param featuresColumn The name of the column containing the scaled feature vectors
   * @return The PipelineModel containing our trained decision tree model
   *
   */

  def trainDecisionTreeModel(featurizedDF:Dataset[Row], trainingDF:Dataset[Row], labelColumn:String,
      featuresColumn:String): PipelineModel = {

    // Index the Labels
    val labelIndexer = new StringIndexer()
                          .setInputCol(labelColumn)
                          .setOutputCol("indexed_label")
                          .fit(featurizedDF);

    // Define the Decision Tree Model
    val decisionTreeModel = new DecisionTreeClassifier()
                              .setLabelCol("indexed_label")
                              .setFeaturesCol(featuresColumn);

    // Convert the Indexed Labels back to the original Labels based on the trained predictions
    val labelConverter = new IndexToString()
                            .setInputCol("prediction")
                            .setOutputCol("predicted_label")
                            .setLabels(labelIndexer.labels);

    // Chain the Indexers and Decision Tree Model to form a Pipeline
    val pipeline = new Pipeline()
                    .setStages(Array(labelIndexer, decisionTreeModel, labelConverter));

    // Run the Indexers and Train the Model on the Training Data
    return pipeline.fit(trainingDF);

  }

  /**
   * Build a Random Forest Classifier
   * Train a Random Forest Model by supplying the training dataset that includes the label and feature vector columns
   *
   * @param featuresDF The full DataFrame containing the labels and feature vectors
   * @param trainingDF The training split DataFrame to be used to train the Model
   * @param labelColumn The name of the column containing the labels
   * @param featuresColumn The name of the column containing the scaled feature vectors
   * @return The PipelineModel containing our trained random forest model
   *
   */

  def trainRandomForestModel(featurizedDF:Dataset[Row], trainingDF:Dataset[Row], labelColumn:String,
      featuresColumn:String): PipelineModel = {

    // Index the Labels
    val labelIndexer = new StringIndexer()
                          .setInputCol(labelColumn)
                          .setOutputCol("indexed_label")
                          .fit(featurizedDF);

    // Define a Random Forest model
    val randomForestModel = new RandomForestClassifier()
                              .setLabelCol("indexed_label")
                              .setFeaturesCol(featuresColumn)
                              .setNumTrees(numTrees);

    // Convert the Indexed Labels back to the original Labels based on the trained predictions
    val labelConverter = new IndexToString()
                            .setInputCol("prediction")
                            .setOutputCol("predicted_label")
                            .setLabels(labelIndexer.labels);

    // Chain the Indexers and Random Forest Model to form a Pipeline
    val pipeline = new Pipeline()
                      .setStages(Array(labelIndexer, randomForestModel, labelConverter));

    // Run the Indexers and Train the Model on the Training Data
    return pipeline.fit(trainingDF);

  }

  /**
   * Generate Multi-class Metrics
   * Generate multi-class metrics given a predictions dataframe containing prediction and indexed label double columns.
   * Such metrics allow us to generate classification matrices, false and true positive rates etc.
   *
   * @param predictionsDF A DataFrame containing predictions and indexed labels
   * @param predictionColumn The name of the column containing the predictions [Double]
   * @param indexedLabelColumn The name of the column containing the indexed labels [Double]
   * @return A MulticlassMetrics object that can be used to output model metrics
   *
   */

  def generateMulticlassMetrics(predictionsDF:Dataset[Row], predictionColumn:String, indexedLabelColumn:String): MulticlassMetrics = {

    val predictionAndLabels = predictionsDF.select(predictionColumn, indexedLabelColumn).rdd.map{
      case Row(predictionColumn: Double, indexedLabelColumn:Double) => (predictionColumn, indexedLabelColumn);
    };
    return new MulticlassMetrics(predictionAndLabels);

  }

}