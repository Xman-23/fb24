package com.example.demo.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	비밀번호 재설정 요청 DTO

	Request(요청)
	이메일
	사용자 이름
	주민번호

*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberVerifyResetPasswordDTO {
	
	@Email
	@NotBlank
    private String email;
	@NotBlank
    private String username;
	@NotBlank
    private String residentNumber;

    public MemberVerifyResetPasswordDTO toDto() {
    	//replaceAll : 정규식 전용 , replace : 문자열 전용
    	String cleanresidentNumber = residentNumber.replaceAll("-", "");

    	MemberVerifyResetPasswordDTO verifyResetPasswordDto = new MemberVerifyResetPasswordDTO(this.email
    																			   , username
    																			   , cleanresidentNumber);;


        return verifyResetPasswordDto;
    }
}
