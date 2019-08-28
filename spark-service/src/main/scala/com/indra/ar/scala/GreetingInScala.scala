package com.indra.ar.scala

import org.springframework.stereotype.Component;

@Component
class GreetingInScala {
   def greet(): String = {
    val greeting = "Hello from SCALA!!!!"
    greeting
  }
}