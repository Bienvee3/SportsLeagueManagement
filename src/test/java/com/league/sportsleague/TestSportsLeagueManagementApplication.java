package com.league.sportsleague;

import org.springframework.boot.SpringApplication;

public class TestSportsLeagueManagementApplication {

	public static void main(String[] args) {
		SpringApplication.from(SportsLeagueManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
