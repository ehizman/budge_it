package com.relab.budge_it;

import org.springframework.boot.SpringApplication;

public class TestBudgeItApplication {

	public static void main(String[] args) {
		SpringApplication.from(BudgeItApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
