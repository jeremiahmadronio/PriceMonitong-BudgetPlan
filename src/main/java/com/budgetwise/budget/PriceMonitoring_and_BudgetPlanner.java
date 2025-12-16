package com.budgetwise.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PriceMonitoring_and_BudgetPlanner {

	public static void main(String[] args) {
		SpringApplication.run(PriceMonitoring_and_BudgetPlanner.class, args);
	}

}
