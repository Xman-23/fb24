package com.example.demo.dto.member.memberauth;

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
	@NotBlank(message = "이메일은 필수 입니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수 입니다.")
	private String password;

}
