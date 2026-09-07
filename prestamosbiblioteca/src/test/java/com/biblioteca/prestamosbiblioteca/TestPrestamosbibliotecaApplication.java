package com.biblioteca.prestamosbiblioteca;

import org.springframework.boot.SpringApplication;

public class TestPrestamosbibliotecaApplication {

	public static void main(String[] args) {
		SpringApplication.from(PrestamosbibliotecaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
