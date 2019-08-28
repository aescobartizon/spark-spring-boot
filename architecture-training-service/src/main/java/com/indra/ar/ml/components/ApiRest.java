package com.indra.ar.ml.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.indra.ar.ml.models.airport.ExampleRandowForest;

@RestController
public class ApiRest {

	@Autowired
	private ExampleRandowForest exampleRandowForest;

	@GetMapping(path = "/examples/randowforest")
	public void getEventsSummary() {

		//exampleRandowForest.training();
		exampleRandowForest.predict();

	}

}
