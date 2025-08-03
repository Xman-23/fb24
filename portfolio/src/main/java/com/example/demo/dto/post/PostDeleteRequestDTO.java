package com.example.demo.dto.post;

import lombok.Data;

@Data
public class PostDeleteRequestDTO {

	//이미지 삭제 여부
	private boolean deleteImages = false;

}
