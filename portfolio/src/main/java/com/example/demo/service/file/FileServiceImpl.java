package com.example.demo.service.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.example.demo.service.post.PostServiceImpl;
import com.example.demo.validation.file.ImageFile;

@Service
public class FileServiceImpl implements FileService {

	// 파일 업로드 경로 ('application.properties'에서 경로처리)
	@Value("${file.upload.base-path}")
	private String basePath;

	private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

	//*************************************************** Service START ***************************************************//

	@Override
	public String uploadToLocal(MultipartFile file)  {

		logger.info("FileServiceImpl uploadToLocal() Start");

		//String uploadDir = "C:/upload/image";

		// 디렉토리 생성
		File dir = new File(basePath);

		if(!dir.exists()) {
			//상위 디렉토리까지 생성 (upload 생성 후 -> image 생성) 
			dir.mkdirs();
		}

		// 이미지를 없로드 안 할경우 'null' 처리
        if (!ImageFile.isValidFile(file)) {
        	return null;
        }

        String originalFilename =file.getOriginalFilename();
 
        if(originalFilename == null) {
        	originalFilename = "unknown";
        }else {
        	originalFilename = UriUtils.decode(originalFilename, StandardCharsets.UTF_8);
        }

        // 파일명 공백, 한글, 특수문자 제거
        String cleanFileName = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        // 랜덤 파일명
        String fileName = UUID.randomUUID() + "_" + cleanFileName;
        // 업로드할 경로
        String fullPath = basePath + "/" + fileName;

        try {
        	// 실제 파일 업로드
            file.transferTo(new File(fullPath));
        } catch (IOException e) {
        	logger.error("FileServiceImpl uploadToLocal IOException Error");
            throw new RuntimeException("파일 업로드 실패", e);
        }

        logger.info("FileServiceImpl uploadToLocal() Success End");
        return "/images/" + fileName; // DB에는 상대 경로 저장
	}

	//*************************************************** Service End ***************************************************//

}
