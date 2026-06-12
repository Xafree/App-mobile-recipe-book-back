package fr.gymgod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication()
@EnableScheduling
public class GymgodApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymgodApplication.class, args);
	}

}
