package com.api.agenda_odontologica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.api.agenda_odontologica.api.entity")
@EnableJpaRepositories("com.api.agenda_odontologica.api.repository")
public class AgendaOdontologicaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgendaOdontologicaApplication.class, args);
	}

}
