package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
	/*
	@Bean
	CommandLineRunner run(MemberRepository memberRepository) {
		return args -> {
			Member member = new Member();
			member.setUsername("testUser");
			member.setPassword("testPass");
			member.setEmail("test@example.com");

			memberRepository.save(member);
			System.out.println("Member saved : " + member.getUsername());
		};
	}
	*/
}