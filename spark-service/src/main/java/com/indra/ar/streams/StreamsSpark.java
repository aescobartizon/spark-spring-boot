package com.indra.ar.streams;

import java.util.Arrays;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StreamsSpark {

	@Autowired
	SparkSession spark;

	public void Kafka() throws StreamingQueryException, InterruptedException {

		Dataset<Row> lines = spark.readStream().format("socket").option("host", "127.0.0.1").option("port", 9999)
				.load();

		Dataset<String> words = lines.as(Encoders.STRING()).flatMap(
				(FlatMapFunction<String, String>) x -> Arrays.asList(x.split(" ")).iterator(), Encoders.STRING());

		Dataset<Row> wordCounts = words.groupBy("value").count();

		StreamingQuery query = wordCounts.writeStream().queryName("wordsCount").outputMode("complete").format("console")
				.start();

		
		query.awaitTermination();

	}
	
	public void results() {
		spark.sql("select * from wordsCount").count();
	}
}
