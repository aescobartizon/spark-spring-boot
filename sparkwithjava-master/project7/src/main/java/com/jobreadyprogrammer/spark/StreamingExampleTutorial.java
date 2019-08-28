package com.jobreadyprogrammer.spark;

import java.util.Arrays;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;

public class StreamingExampleTutorial {
	
	public static void main(String[] args) throws StreamingQueryException {

		SparkSession spark = SparkSession.builder().appName("StreamingFileDirectoryWordCount").master("local")
				.getOrCreate();
		// Create DataFrame representing the stream of input lines from connection to
		// localhost:9999
		Dataset<Row> lines = spark.readStream().format("socket").option("host", "127.0.0.1").option("port", 9999)
				.load();

		// Split the lines into words
		Dataset<String> words = lines.as(Encoders.STRING()).flatMap(
				(FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());

		// Generate running word count
		Dataset<Row> wordCounts = words.groupBy("value").count();
		
		StreamingQuery query = wordCounts.writeStream().outputMode("update").format("console").start();

		query.awaitTermination();

	}

}
