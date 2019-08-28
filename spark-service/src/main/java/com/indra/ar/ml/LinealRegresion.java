package com.indra.ar.ml;

import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LinealRegresion {
	
	@Autowired
	SparkSession spark;
	
	
	public void linealRegresion() {
	
	Dataset<Row> markVsSalesDf = spark.read()
			.option("header", "true")
			.option("inferSchema", "true")
			.format("csv")
			.load("src/main/resources/marketing-vs-sales.csv");
		markVsSalesDf.show();
		
		// go through the lecture first and then start un-commenting the code below

		Dataset<Row> mldf = markVsSalesDf.withColumnRenamed("sales", "label")
				.select("label", "marketing_spend");
		
		mldf.show();
		
//		mldf = mldf.withColumnRenamed("marketing_spend", "features").select("label","features");
//		mldf.show();
//		.select("label", "marketing_spend","bad_day");
//		
		//String[] featureColumns = {"marketing_spend", "bad_day"}	
		
		String[] featureColumns = {"marketing_spend"};	
		
		VectorAssembler assember = new VectorAssembler()
						.setInputCols(featureColumns)
						.setOutputCol("features");
//		
		Dataset<Row> lblFeaturesDf = assember.transform(mldf).select("label", "features");
		lblFeaturesDf = lblFeaturesDf.na().drop();
		lblFeaturesDf.show();
//		
//		// next we need to create a linear regression model object
		LinearRegression lr = new LinearRegression();
		LinearRegressionModel learningModel = lr.fit(lblFeaturesDf);
		
//		try {
//			learningModel.save("c:/linealregresion");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		//learningModel.summary().predictions().show();
	
		System.out.println("R Squared: "+ learningModel.summary().r2());
		
		
	}

}
