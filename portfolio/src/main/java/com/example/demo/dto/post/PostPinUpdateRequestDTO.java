package com.example.demo.dto.post;

import lombok.Data;

/*
	게시글 수정 DTO

	Request(요청)
	pinned (핀 설정)
*/

@Data
public class PostPinUpdateRequestDTO {

	private boolean pinned;

}
