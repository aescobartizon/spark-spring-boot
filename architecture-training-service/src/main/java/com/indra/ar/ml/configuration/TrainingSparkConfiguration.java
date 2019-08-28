package com.indra.ar.ml.configuration;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TrainingSparkConfiguration {
	
	@Bean
	SparkSession getSparkSession() {
		return SparkSession.builder().appName("Spark Training Service").master("local").getOrCreate();
	}

}
