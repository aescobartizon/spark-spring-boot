package com.indra.ar.reddit;

import static org.apache.spark.sql.functions.desc;

import java.util.Arrays;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Reddit {
	
	@Autowired
	SparkSession spark;
	
	
	public void reddit() {
		 
		 //String redditFile = "D:/WS/spark-service/src/main/resources/Reddit_2007-small.json"; // <- change your file location
		 
		    Dataset<Row> redditDf = spark.read().format("json")
		        .option("inferSchema", "true") // Make sure to use string version of true
		        .option("header", true)
		        .load("src/main/resources/Reddit-2007-small.json");
		    
		    redditDf = redditDf.select("body");
		    Dataset<String> wordsDs = redditDf.flatMap((FlatMapFunction<Row, String>)
		    		r -> Arrays.asList(r.toString().replace("\n", "").replace("\r", "").trim().toLowerCase()
		    				.split(" ")).iterator(),
		    		Encoders.STRING());
		
		    Dataset<Row> wordsDf = wordsDs.toDF();
		    
		    Dataset<Row> boringWordsDf = spark.createDataset(Arrays.asList(WordUtils.stopWords), Encoders.STRING()).toDF();

//			wordsDf = wordsDf.except(boringWordsDf); // <-- This won't work because it removes duplicate words!!

		    wordsDf = wordsDf.join(boringWordsDf, wordsDf.col("value").equalTo(boringWordsDf.col("value")), "leftanti");
		   
		    wordsDf = wordsDf.groupBy("value").count();
		    wordsDf.orderBy(desc("count")).show();
	}

}
