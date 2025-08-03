package com.example.demo.dto.post;

import java.util.List;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
	게시글 생성 DTO

	Request(요청)
	boardId		(게시글이 속한 게시판ID(boardId)
	title		(게시글 게목)
	content		(게시글 내용)
	isNotice 	(공지 여부(공지글 인지, 일반글 인지))
	images		(이미지'들')
*/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequestDTO {


	@NotNull(message = "게시판 ID는 필수입니다.")
	private Long boardId;

	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
	private String title;

	@NotBlank(message = "본문 내용은 필수입니다.")
	private String content;

	private boolean notice = false;

	/* 
	 JSON 형태의 데이터와 이미지 파일을 동시에 받을려면, 
	 클라이언트가 multipart/form-data 형식으로 데이터를 보내고
	 컨트롤러 에서 '@ModelAttribute' 사용
	 */
	private List<MultipartFile> images;

}
