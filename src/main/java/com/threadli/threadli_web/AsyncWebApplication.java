package com.threadli.threadli_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AsyncWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsyncWebApplication.class, args);
	}

}
