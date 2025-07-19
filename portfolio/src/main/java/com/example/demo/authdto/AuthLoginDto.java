package com.example.demo.authdto;

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


@Data //setter,getter 자동생성
@NoArgsConstructor//기본생성자 생성
public class AuthLoginDto {
	
	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

}
