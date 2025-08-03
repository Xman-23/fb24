package com.example.demo.validation.file;

import org.springframework.web.multipart.MultipartFile;

public class ImageFile {

	/**
	  사용자가 실수로 빈 파일을 선택해서 업로드

	  네트워크 문제로 전송이 제대로 안 된 경우

	  클라이언트(브라우저, 앱 등)에서 빈 파일을 생성해 전송하는 경우
	*/
	public static boolean isValidFile(MultipartFile file) {

		if(file == null || file.isEmpty()) {
			return false;
		}

		return true;
	}

}
