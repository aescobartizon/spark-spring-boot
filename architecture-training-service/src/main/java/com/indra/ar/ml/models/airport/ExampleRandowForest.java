package com.indra.ar.ml.models.airport;

import java.io.IOException;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorIndexer;
import org.apache.spark.ml.feature.VectorIndexerModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.indra.ar.ml.interfaces.ModelsMl;

@Component
public class ExampleRandowForest implements ModelsMl {
	
	@Autowired
	private SparkSession spark;

	@Override
	public void training() {
		// Load and parse the data file, converting it to a DataFrame.
		Dataset<Row> data = spark.read().format("libsvm").load("src/main/resources/examples/sample_libsvm_data.txt");

		// Index labels, adding metadata to the label column.
		// Fit on whole dataset to include all labels in index.
		StringIndexerModel labelIndexer = new StringIndexer()
		  .setInputCol("label")
		  .setOutputCol("indexedLabel")
		  .fit(data);
		// Automatically identify categorical features, and index them.
		// Set maxCategories so features with > 4 distinct values are treated as continuous.
		VectorIndexerModel featureIndexer = new VectorIndexer()
		  .setInputCol("features")
		  .setOutputCol("indexedFeatures")
		  .setMaxCategories(4)
		  .fit(data);

		// Split the data into training and test sets (30% held out for testing)
		Dataset<Row>[] splits = data.randomSplit(new double[] {0.7, 0.3});
		Dataset<Row> trainingData = splits[0];
		Dataset<Row> testData = splits[1];

		// Train a RandomForest model.
		RandomForestClassifier rf = new RandomForestClassifier()
		  .setNumTrees(10)
		  .setLabelCol("indexedLabel")
		  .setFeaturesCol("indexedFeatures");
		
//		try {
//			rf.save("D://models/examplerandowforest/algoritmo");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// Convert indexed labels back to original labels.
		IndexToString labelConverter = new IndexToString()
		  .setInputCol("prediction")
		  .setOutputCol("predictedLabel")
		  .setLabels(labelIndexer.labels());

		// Chain indexers and forest in a Pipeline
		Pipeline pipeline = new Pipeline()
		  .setStages(new PipelineStage[] {labelIndexer, featureIndexer, rf, labelConverter});

		// Train model. This also runs the indexers.
		PipelineModel model = pipeline.fit(trainingData);
		
		try {
			model.write().overwrite().save("D://models/examplerandowforest/modelo");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		model =  PipelineModel.read().load("D://models/examplerandowforest/modelo");
		// Make predictions.
		Dataset<Row> predictions = model.transform(testData);
		
		predictions.show();

		// Select example rows to display.
		predictions.select("predictedLabel", "label", "features").show(5);

		// Select (prediction, true label) and compute test error
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
		  .setLabelCol("indexedLabel")
		  .setPredictionCol("prediction")
		  .setMetricName("accuracy");
		
		double accuracy = evaluator.evaluate(predictions);
		System.out.println("Test Error = " + (1.0 - accuracy));

		RandomForestClassificationModel rfModel = (RandomForestClassificationModel)(model.stages()[2]);
		System.out.println("Learned classification forest model:\n" + rfModel.toDebugString());
		
	}
	
	
	public void predict() {
		
		Dataset<Row> data = spark.read().format("libsvm").load("src/main/resources/examples/sample_libsvm_data.txt");
		PipelineModel model = PipelineModel.read().load("D://models/examplerandowforest/modelo");
		Dataset<Row> predictions = model.transform(data);
		predictions.show();
		predictions.select("predictedLabel", "label", "features").show(5);
	}
	
	

}
