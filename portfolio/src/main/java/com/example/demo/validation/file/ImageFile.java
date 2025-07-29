package com.example.demo.validation.file;

import org.springframework.web.multipart.MultipartFile;

public class ImageFile {

	public static boolean isValidFile(MultipartFile file) {

		if(file == null || file.isEmpty()) {
			return false;
		}

		return true;
	}

}
