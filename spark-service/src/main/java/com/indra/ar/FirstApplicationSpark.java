package com.indra.ar;

import static org.apache.spark.sql.functions.concat;
import static org.apache.spark.sql.functions.lit;

import java.util.Properties;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FirstApplicationSpark {
	
	@Autowired
	SparkSession spark;

	public void execute() {

//		// Create a session
//		SparkSession spark = new SparkSession.Builder().appName("CSV to DB").master("local").getOrCreate();

		// get data
		// En Produccion los datos se cogerán de hadoop o S3 Amazon
		Dataset<Row> df = spark.read().format("csv").option("header", true)
				.load("src/main/resources/name_and_comments.txt");

		 df.show();

		// Transformation
		df = df.withColumn("full_name", concat(df.col("last_name"), lit(", "), df.col("first_name")))
				.filter(df.col("comment").rlike("\\d+")).orderBy(df.col("last_name").asc());
		
		df.show();

		// Write to destination
		String dbConnectionUrl = "jdbc:postgresql://localhost:5432/postgres"; // <<- You need to create this database
		Properties prop = new Properties();
		prop.setProperty("driver", "org.postgresql.Driver");
		prop.setProperty("user", "admin");
		prop.setProperty("password", "admin"); // <- The password you used while installing Postgres

		df.write().mode(SaveMode.Overwrite).jdbc(dbConnectionUrl, "project1", prop);
	}

}
