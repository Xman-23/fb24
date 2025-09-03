package com.example.demo.dto.member.memberauth;


import com.example.demo.domain.member.memberenums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
	리프레쉬토큰 요청 DTO

	Response(응답) 목록
	accessToken		(액세스토큰)
	refreshToken	(리프레쉬토큰)
	
*/

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponseDTO {

	private String accessToken;

	private String refreshToken ;

	private Long memberId;

	// enum Role 타입 필드 추가
	private Role role;

}
