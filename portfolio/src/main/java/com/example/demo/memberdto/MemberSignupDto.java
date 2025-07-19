package com.example.demo.memberdto;

import com.example.demo.controller.MemberController;
import com.example.demo.domain.Member;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
	회원가입 DTO

	Request(요청)
	사용자이름
	이메일
	패스워드
	핸드폰번호
	주민번호
*/

//DTO = 경비원(데이터 유효성검사)
//클라이언트에게 공개해도 되는 정보만 선언(DTO)
@Data
@NoArgsConstructor
public class MemberSignupDto {
	private static final Logger logger = LoggerFactory.getLogger(MemberSignupDto.class);
	@NotBlank
	private String username;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String password;

	@NotBlank
	private String phoneNumber;

	@NotBlank
	@Pattern(regexp = "^\\d{6}-?\\d{7}$", message = "주민번호 형식이 올바르지 않습니다.")
	private String residentNumber; 

	@NotBlank
	@Size(min = 2, max = 20 , message = "닉네임은 2글자 이상 20자 이하로 입력해주세요.")
	private String nickname;

	private String address;

    public Member toEntity() {
    	//replaceAll : 정규식 전용 , replace : 문자열 전용
    	String cleanresidentNumber = residentNumber.replaceAll("-", "");
        Member member = new Member();
        member.setUsername(this.username);
        member.setEmail(this.email);
        member.setPassword(this.password);
        member.setPhoneNumber(this.phoneNumber);
        member.setResidentNumber(cleanresidentNumber);
        member.setNickname(this.nickname);
        member.setAddress(this.address);

        return member;
    }
}
