package com.indra.ar.dataset;

import static org.apache.spark.sql.functions.concat;
import static org.apache.spark.sql.functions.lit;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.ReduceFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArrayToDataset {
	
	@Autowired
	private SparkSession spark;
	
	private List<String> data = Arrays.asList("Banna", "Car", "Glass", "Banana", "Computer", "Car");
	
	public void start() {

		Dataset<String> ds = spark.createDataset(data, Encoders.STRING());
		
		ds.show();
		ds.printSchema();

		ds = ds.map((MapFunction<String, String>) row -> "word: " + row, Encoders.STRING());
		ds.show(10);
		ds.printSchema();

		String stringValue = ds.reduce(new StringReducer());

		System.out.println(stringValue);

	}
	
	static class StringReducer implements ReduceFunction<String>, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String call(String v1, String v2) throws Exception {
			return v1 + v2;
		}
		
	}
	
	public void startHouse() {
		String filename = "src/main/resources/houses.csv";

		Dataset<Row> df = spark.read().format("csv").option("inferSchema", "true") // Make sure to use string version of															// true
				.option("header", true).option("sep", ";").load(filename);

		System.out.println("House ingested in a dataframe: ");
		df.show(5);
		df.printSchema();

		Dataset<House> houseDS = df.map(new HouseMapper(), Encoders.bean(House.class));

		System.out.println("*****House ingested in a dataset: *****");

		houseDS.show(5);
		houseDS.printSchema();

		Dataset<Row> df2 = houseDS.toDF();
		df2 = df2.withColumn("formatedDate", concat(df2.col("vacantBy.date"), lit("_"), df2.col("vacantBy.year")));
		df2.show(10);
	}
	
	public void startBoring() {
		String boringWords = " ('a', 'an', 'and', 'are', 'as', 'at', 'be', 'but', 'by',\r\n"
				+ "'for', 'if', 'in', 'into', 'is', 'it',\r\n" + "'no', 'not', 'of', 'on', 'or', 'such',\r\n"
				+ "'that', 'the', 'their', 'then', 'there', 'these',\r\n"
				+ "'they', 'this', 'to', 'was', 'will', 'with', 'he', 'she'," + "'your', 'you', 'I', "
				+ " 'i','[',']', '[]', 'his', 'him', 'our', 'we') ";

		String filename = "src/main/resources/shakespeare.txt";

		Dataset<Row> df = spark.read().format("text").load(filename);

		df.printSchema();
		df.show(10);

		Dataset<String> wordsDS = df.flatMap(new LineMapper(), Encoders.STRING());

		Dataset<Row> df2 = wordsDS.toDF();

		df2 = df2.groupBy("value").count();
		df2 = df2.orderBy(df2.col("count").desc());
		df2 = df2.filter("lower(value) NOT IN " + boringWords);

		df2.show(500);
	}

}
