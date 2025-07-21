package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
	로그인 DTO

	Request(요청)
	이메일
	패스워드
*/

import lombok.Data;
import lombok.NoArgsConstructor;

/*
	로그인 요청 DTO

	Request
	email		(이메일)
	password	(패스워드)
*/

@Data //setter,getter 자동생성
@NoArgsConstructor//기본생성자 생성
public class AuthLoginDTO {
	
	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

}
