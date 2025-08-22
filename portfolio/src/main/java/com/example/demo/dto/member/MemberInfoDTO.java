package com.example.demo.dto.member;

import com.example.demo.domain.member.memberenums.MemberGradeLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberInfoDTO {

	private String username;

	private String phoneNumber;

	private String nickname;

	private String address;

	private MemberGradeLevel memberGradeLevel;
	
    // 새로 추가
    private boolean postNotificationEnabled;
    private boolean commentNotificationEnabled;


}
