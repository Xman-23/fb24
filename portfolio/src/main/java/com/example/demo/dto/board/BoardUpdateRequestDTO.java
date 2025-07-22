package com.example.demo.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	게시판 수정 DTO

	Request(요청)
	name			(게시판 제목)
	boardId			(게시판 종류)
	description		(게시판 설명)
	isActive		(게시판 숨김기능)
	sortOrder 		(게시판 순서나열기능)
	parentBoardId	('Null'이면 부모게시판, '값'이 있으면 자식 게시판)
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequestDTO {

	@NotBlank
	@Size(max = 100, message = "게시판 이름은 100자 이하로 입력해주세요.")
	private String name;

	@Size(max = 255, message = "게시판 설명은 255자 이하로 입력해주세요.")
	private String description;

	private Long boardId;

	private boolean isActive;

	private Integer sortOrder;

	/* 
	  이 게시판이 속할 부모 게시판의 ID (boardId == parentBoardId )
	  - null이면 최상위 게시판으로 생성됨 (예: 자유게시판, 공지사항 등)
	  - 값이 있으면 해당 ID의 부모 게시판 아래 자식 게시판으로 생성됨
	   예: 자유게시판의 boardId가 1이라면 → 유머게시판 생성 시 parentBoardId = 1
	*/
	private Long parentBoardId;

}
