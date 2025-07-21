package com.example.demo.dto.member;

import com.example.demo.domain.member.Member;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 	이메일(ID) 찾기 요청 DTO

	Request(요청)
 	사용자 이름
 	주민번호
 
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberVerifyFindEmailRequestDTO {

	@NotBlank
	private String username;
	@NotBlank
	private String residentNumber;

    public MemberVerifyFindEmailRequestDTO toDto() {
    	//replaceAll : 정규식 전용 , replace : 문자열 전용
    	String cleanresidentNumber = residentNumber.replaceAll("-", "");

    	MemberVerifyFindEmailRequestDTO verifyFindEmailRequestDto = new MemberVerifyFindEmailRequestDTO(this.username
    																						, cleanresidentNumber);;


        return verifyFindEmailRequestDto;
    }

}
