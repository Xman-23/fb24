package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//내가 만든 스케쥴러를 활성화하기 위한 어노테이션 보통 'Main'에 명시
@EnableScheduling
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