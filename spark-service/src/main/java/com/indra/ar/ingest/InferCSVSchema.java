package com.indra.ar.ingest;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class InferCSVSchema {
	
	
	@Autowired
	SparkSession spark;
	
	public void printSchema() {
//		SparkSession spark = SparkSession.builder()
//		        .appName("Complex CSV to Dataframe")
//		        .master("local")
//		        .getOrCreate();
		 
		    Dataset<Row> df = spark.read().format("csv") //
		        .option("header", "true") //
		        .option("multiline", true) //
		        .option("sep", ";") //
		        .option("quote", "^") //
		        .option("dateFormat", "M/d/y") //
		        .option("inferSchema", true) //
		        .load("src/main/resources/amazonProducts.txt");
		 
		    System.out.println("Excerpt of the dataframe content:");
//		    df.show(7);
		    df.show(7, 90); // truncate after 90 chars
		    System.out.println("Dataframe's schema:");
		    df.printSchema();
	}
	
}
