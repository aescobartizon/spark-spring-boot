package com.indra.ar;

import org.apache.spark.sql.streaming.StreamingQueryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.indra.ar.dataset.ArrayToDataset;
import com.indra.ar.filters.Filters;
import com.indra.ar.ingest.DefineCSVSchema;
import com.indra.ar.ingest.InferCSVSchema;
import com.indra.ar.ingest.JSONLinesParser;
import com.indra.ar.kafka.KafkaTemplateTest;
import com.indra.ar.ml.LinealRegresion;
import com.indra.ar.reddit.Reddit;
import com.indra.ar.scala.GreetingInScala;
import com.indra.ar.streams.StreamsSpark;
import com.indra.ar.union.UnionExample;

@RestController
public class TestController {
	
	@Autowired
	GreetingInScala greeting;
	
	@Autowired
	FirstApplicationSpark firstApplicationSpark;
	
	@Autowired
	JSONLinesParser jSONLinesParser;
	
	
	@Autowired
	DefineCSVSchema defineCSVSchema;
	
	
	@Autowired
	InferCSVSchema inferCSVSchema;
	
	@Autowired
	UnionExample unionExample;
	
	@Autowired
	ArrayToDataset arrayToDataset;
	
	@Autowired 
	Filters filter;
	
	@Autowired
	StreamsSpark streams;
	
	@Autowired
	KafkaTemplateTest kafkaTemplateTest;
	
	@Autowired
	Reddit reddit;
	
	@Autowired
	LinealRegresion linealRegresion;
	
	
	@GetMapping(path = "/greet")
	public String getEventsSummary() {
		
		firstApplicationSpark.execute();
		return greeting.greet();
		
	}
	
	@GetMapping(path = "/jsonParser")
	public void jsonParser() {
		jSONLinesParser.parseJsonLines();
	}
	
	@GetMapping(path = "/cvsSchema")
	public void cvsSchema() {
		defineCSVSchema.printDefinedSchema();
	}
	

	@GetMapping(path = "/cvsInferSchema")
	public void cvsInferSchema() {
		inferCSVSchema.printSchema();
	}

	@GetMapping(path = "/unionDataFrame")
	public void unionDataFrame() {
		unionExample.combineData();
	}
	
	@GetMapping(path = "/arrayToDataset")
	public void arrayToDataset() {
		arrayToDataset.start();
		arrayToDataset.startHouse();
		arrayToDataset.startBoring();
	}
	
	@GetMapping(path = "/filters")
	public void filters() {
		filter.filter();
	}
	
	@GetMapping(path = "/socketStream")
	public void socketStream() throws StreamingQueryException, InterruptedException {
		streams.Kafka();
	}
	
	@GetMapping(path = "/socketStreamResults")
	public void socketStreamResults() throws StreamingQueryException, InterruptedException {
		streams.results();
	}
	
	@GetMapping(path = "/reddit")
	public void reddit() {
		reddit.reddit();
	}
	
	@GetMapping(path = "/sendStream")
	public void sendStream() throws StreamingQueryException, InterruptedException {

		for (int i = 0; i < 10; i++) {
			kafkaTemplateTest.sendMessage("Hola esto es un problema nuevo");
		}

	}
	
	
	@GetMapping(path = "/linealRegresion")
	public void linealRegresion() {
		linealRegresion.linealRegresion();
	}
}
