package com.example.demo.service.post.postimage;

import org.springframework.web.multipart.MultipartFile;

import io.jsonwebtoken.io.IOException;

public interface PostImageService {

	// 파일 업로드
	String uploadToLocal(MultipartFile file) throws IOException;

}
