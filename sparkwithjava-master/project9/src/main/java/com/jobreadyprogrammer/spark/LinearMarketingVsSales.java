package com.jobreadyprogrammer.spark;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class LinearMarketingVsSales {

	public static void main(String[] args) {
		
		Logger.getLogger("org").setLevel(Level.ERROR);
		Logger.getLogger("akka").setLevel(Level.ERROR);
		
		SparkSession spark = new SparkSession.Builder()
				.appName("LinearRegressionExample")
				.master("local")
				.getOrCreate();
		
		Dataset<Row> markVsSalesDf = spark.read()
			.option("header", "true")
			.option("inferSchema", "true")
			.format("csv")
			.load("src/main/resources/marketing-vs-sales2.csv");
		markVsSalesDf.show();
		
		// go through the lecture first and then start un-commenting the code below

		Dataset<Row> mldf = markVsSalesDf.withColumnRenamed("sales", "label")
		.select("label", "bad_day","marketing_spend");
		
		String[] featureColumns = {"bad_day","marketing_spend"};
		
		VectorAssembler assember = new VectorAssembler()
						.setInputCols(featureColumns)
						.setOutputCol("features");
		
		Dataset<Row> lblFeaturesDf = assember.transform(mldf).select("label", "features");
		
		lblFeaturesDf = lblFeaturesDf.na().drop();
		lblFeaturesDf.show();
		
		// next we need to create a linear regression model object
		LinearRegression lr = new LinearRegression();
		LinearRegressionModel learningModel = lr.fit(lblFeaturesDf);
		
		learningModel.summary().predictions().show();
		learningModel.coefficients().toString();
		

		double[] features = new double[1];
		features[0] = Double.parseDouble("1000000");
		//double carPricePrediction = learningModel.predict(Vectors.dense(features));
		double carPricePrediction = learningModel.predict(Vectors.dense(1.0,100000));
		
		System.out.println("Prevision 1000000 : "+ carPricePrediction);
		
		System.out.println("R Squared: "+ learningModel.summary().r2());
	}
}
