package com.example.demo.dto.auth;

import com.example.demo.domain.member.memberenums.Role;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	리프레쉬토큰 요청 DTO

	Response(응답) 목록
	accessToken		(액세스토큰)
	refreshToken	(리프레쉬토큰)
	
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponseDTO {

	private String accessToken;

	private String refreshToken;

	// enum Role 타입 필드 추가
	private Role role;

}
