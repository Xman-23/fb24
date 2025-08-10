package com.example.demo.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/*
	비밀번호 변경 DTO

	Request(요청)
	newPassword			(새로운 비밀번호)
	confirmNewPassword	(비밀번호 확인)

*/

@Data
public class MemberResetPasswordDTO {

	@NotBlank(message = "새로운 비밀번호를 입력해주세요")
	private String newPassword;
	@NotBlank(message = "새로운 비밀번호 확인을 입력해주세요.")
	private String confirmNewPassword;

}
