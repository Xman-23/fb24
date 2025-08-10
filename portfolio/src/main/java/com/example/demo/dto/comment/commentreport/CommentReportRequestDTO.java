package com.example.demo.dto.comment.commentreport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportRequestDTO {

	@NotBlank(message = "신고 사유는 필수입니다.")
	@Size(min = 10, message = "최소 10글자 이상 신고 사유를 적어주세요.")
	private String reason;

}
