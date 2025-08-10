package com.example.demo.dto.member.memberauth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	리프레쉬토큰 요청 DTO

	Request
	refreshToken	(리프레쉬토큰)
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshTokenRequestDTO {

	@NotBlank
	private String refreshToken;

}
