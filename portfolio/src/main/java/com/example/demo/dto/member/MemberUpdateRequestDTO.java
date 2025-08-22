package com.example.demo.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MemberUpdateRequestDTO {

    private String nickname;
    private String phoneNumber;
    private String address;
    private Boolean postNotificationEnabled;
    private Boolean commentNotificationEnabled;

}
