package com.example.demo.memberdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	회원정보 조회 DTO

	Return(응답) 목록
	사용자이름
	핸드폰번호
	주민번호
*/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoDto {

	private String username;

	private String email;

	private String phoneNumber;

	private String residentNumber;

	private String nickname;

	private String address;

}
