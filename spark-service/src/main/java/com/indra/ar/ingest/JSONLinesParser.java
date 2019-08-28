package com.indra.ar.ingest;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JSONLinesParser {
	
	@Autowired
	SparkSession spark;

	public void parseJsonLines() {
//		SparkSession spark = SparkSession.builder().appName("JSON Lines to Dataframe").master("local")
//				// .master("spark://localhost:7077")
//				.getOrCreate();

		// Dataset<Row> df = spark.read().format("json")
		// .load("src/main/resources/simple.json");

		Dataset<Row> df2 = spark.read().format("json").option("multiline", true)
				.load("src/main/resources/multiline.json");

		df2.show(5, 50);
		df2.printSchema();
	}
	
}



