package com.example.demo.dto.board;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/*
	게시판 응답 DTO

	Response	(응답)
	name		(게시판 제목)
	boardId		(이메일)
	description	(게시판 설명)
	isActive	(게시판 숨김기능)
	sortOrder 	(게시판 순서나열기능)
*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequestDTO {

	@Size(max = 100, message = "게시판 이름은 100자 이하로 입력해주세요.")
	private String name;

	@Size(max = 255, message = "게시판 설명은 255자 이하로 입력해주세요.")
	private String description;

	private boolean isActive;

	private Integer sortOrder;

}
