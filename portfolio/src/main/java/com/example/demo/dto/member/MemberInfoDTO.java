package com.example.demo.dto.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
	회원정보 조회 DTO

	Return(응답)
	사용자이름
	핸드폰번호
	주민번호
*/

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoDTO {

	private String username;

	private String email;

	private String phoneNumber;

	private String residentNumber;

	private String nickname;

	private String address;

}
