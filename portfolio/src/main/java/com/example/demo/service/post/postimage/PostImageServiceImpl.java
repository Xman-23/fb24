package com.example.demo.service.post.postimage;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.example.demo.validation.postimage.PostImageValidation;
import com.example.demo.validation.string.WordValidation;

@Service
public class PostImageServiceImpl implements PostImageService {

	// 파일 확장자 검사
	private final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "pdf");
	// 파일 MIME 타입 검사
	private final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "application/pdf");
	// 파일 용량 
	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
	
	// 파일 업로드 경로 ('application.properties'에서 경로처리)
	@Value("${file.upload.base-path}")
	private String basePath;

	private static final Logger logger = LoggerFactory.getLogger(PostImageServiceImpl.class);

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
        if (!PostImageValidation.isValidFile(file)) {
        	return null;
        }

        String contentType = file.getContentType();
        logger.info("FileServiceImpl uploadToLocal() contentType: {}", contentType);

        // 이미지 파일이 있는경우 (MIME타입 검사)
        // "image/jpeg", "image/png", "application/pdf" 이외의 콘텐츠는 예외 발생
        if(!ALLOWED_CONTENT_TYPES.contains(contentType)) {
        	throw new IllegalArgumentException("허용되지 않은 파일형식입니다.");
        }

        // 파일의 용량이 5MB를 넘을경우 예외 발생
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과할 수 없습니다.");
        }

        String originalFilename =file.getOriginalFilename();
 
        if(originalFilename == null) {
        	// 이름이 비어있다면 예외 처리
        	throw new IllegalArgumentException("파일명이 비어있습니다.");
        }else {
        	originalFilename = UriUtils.decode(originalFilename, StandardCharsets.UTF_8);
        }

        // 파일명 비속어 검사
		if(!WordValidation.containsForbiddenWord(originalFilename)) {
			logger.error("FileServiceImpl uploadToLocal() IllegalArgumentException : 이미지 제목에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("이미지 제목에 비속어가 포함되어있습니다.");
		}

		// 파일 확장자 검사를 위한 변수
		// 파일 이름에서 마지막 "."을 기준으로 인덱스 +1하여 문자열 끝까지 substring
		// 즉, 확장자명 가져오기
		String ext = originalFilename.substring(originalFilename.lastIndexOf(".")+1).toLowerCase();

		//만약 "jpg", "jpeg", "png", "pdf" 확장자 이외의 확장자는 예외 발생
		if(!ALLOWED_EXTENSIONS.contains(ext)) {
			throw new IllegalArgumentException("허용되지 않은 확장자 입니다.");
		}

        // 파일명 공백, 한글, 특수문자 제거
        String cleanFileName = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        // 랜덤 파일명
        String fileName = UUID.randomUUID() + "_" + cleanFileName;
        // 업로드할 경로
        Path fullPath = Paths.get(basePath, fileName);

        try {
        	// 실제 파일 업로드
            // file.transferTo(fullPath.toFile());
        	// 파일이 이미지인지 체크
        	logger.info("FileServiceImpl uploadToLocal() 이미지 리사이즈 if 분기 Start");
        	if (contentType != null && contentType.toLowerCase().startsWith("image/")) {

                logger.info("FileServiceImpl uploadToLocal() 이미지 리사이즈 if 분기 In");

                BufferedImage originalImage = ImageIO.read(file.getInputStream());

                int targetWidth = 600;
                int targetHeight = 400;

                logger.info("원본 이미지 사이즈: {}x{}", originalImage.getWidth(), originalImage.getHeight());

                // 고정 크기 적용
                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
                g2d.dispose();

                logger.info("변환 후 이미지 사이즈: {}x{}", targetWidth, targetHeight);

                ImageIO.write(resizedImage, ext, fullPath.toFile());

        	} else {
        	    // 이미지가 아니면 (pdf 같은거) 그냥 저장
        		logger.info("FileServiceImpl uploadToLocal() 이미지 리사이즈 IF 분기 else Start");
        	    file.transferTo(fullPath.toFile());
        	    logger.info("FileServiceImpl uploadToLocal() 이미지 리사이즈 IF 분기 else End");
        	}
        } catch (IOException e) {
        	logger.error("FileServiceImpl uploadToLocal IOException : 파일 업로드 실패",e);
            throw new IllegalStateException("파일 업로드 실패", e);
        } catch (Exception e) {
        	logger.error("FileServiceImpl uploadToLocal Exception : 파일 업로드 실패",e);
        	throw new RuntimeException("파일 업로드 실패",e);
		}

        logger.info("FileServiceImpl uploadToLocal() Success End");
        return "/images/" + fileName; // DB에는 상대 경로 저장
	}

	//*************************************************** Service End ***************************************************//

}
