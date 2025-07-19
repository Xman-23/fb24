package com.example.demo.memberdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/*
	비밀번호 변경 DTO

	Request(요청)
	newPassword			(새로운 비밀번호)
	confirmNewPassword	(비밀번호 확인)

*/

@Data
public class MemberResetPasswordDto {

	@NotBlank
	private String newPassword;
	@NotBlank
	private String confirmNewPassword;

}
