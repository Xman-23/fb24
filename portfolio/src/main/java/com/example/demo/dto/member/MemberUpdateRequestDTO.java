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

	@NotBlank(message = "닉네임은 필수 입니다.")
	private String nickname;

	@Pattern(
				regexp = "^01[016789]-\\d{3,4}-\\d{4}$",
				message = "휴대폰 번호 형식이 올바르지 않습니다."
			)

	@NotBlank(message = "핸드폰 번호는 필수 입니다")
	private String phoneNumber;

	@NotBlank(message = "주소는 필수 입니다.")
	private String address;

	// 클라이언트가 'null'을 보낼 수 있으므로 'Wrapper' 클래스인 'Boolean' 사용
	@NotNull (message = "게시글 알림여부는 필수 입니다.")
	private Boolean postNotificationEnabled;

	// 클라이언트가 'null'을 보낼 수 있으므로 'Wrapper' 클래스인 'Boolean' 사용
	@NotNull(message = "댓글 알림여부는 필수 입니다.")
	private Boolean commentNotificationEnabled;

}
