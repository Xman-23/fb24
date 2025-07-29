package com.example.demo.service.file;

import org.springframework.web.multipart.MultipartFile;

import io.jsonwebtoken.io.IOException;

public interface FileService {

	// 파일 업로드
	String uploadToLocal(MultipartFile file) throws IOException;

}
