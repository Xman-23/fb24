package com.example.demo.memberdto;

import com.example.demo.domain.Member;

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
public class MemberVerifyFindEmailRequestDto {

	@NotBlank
	private String username;
	@NotBlank
	private String residentNumber;

    public MemberVerifyFindEmailRequestDto toDto() {
    	//replaceAll : 정규식 전용 , replace : 문자열 전용
    	String cleanresidentNumber = residentNumber.replaceAll("-", "");

    	MemberVerifyFindEmailRequestDto verifyFindEmailRequestDto = new MemberVerifyFindEmailRequestDto(this.username
    																						, cleanresidentNumber);;


        return verifyFindEmailRequestDto;
    }

}
