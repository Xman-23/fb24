package com.example.demo.memberdto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	회원정보변경 DTO

	Request(요청)
	닉네임
	핸드폰번호
	주소
*/

@Data
@NoArgsConstructor
public class MemberUpdateRequest {

	private String nickname;

	@Pattern(
			regexp = "^01[016789]-\\d{3,4}-\\d{4}$",
			message = "휴대폰 번호 형식이 올바르지 않습니다."
	)
	private String phoneNumber;

	private String address;


}
