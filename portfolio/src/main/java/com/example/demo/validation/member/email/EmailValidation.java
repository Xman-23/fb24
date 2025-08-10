package com.example.demo.validation.member.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.controller.member.MemberController;

public class EmailValidation {

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(EmailValidation.class);

    // 이메일 유효성 검사 메소드
    public static boolean isValidEmail(String trimEmail) {

    	// ex)honggildong@example.com

    	if (trimEmail == null || trimEmail.trim().isEmpty()  ) {
    		logger.error("EmailValidation isValidEmail() '첫번째 IF문' trimEmail   :" + trimEmail);
            return false;
        }

        // 영문/숫자 포함, @ 포함, 도메인 형식
    	// ex) user@example.com, my.name+tag@gmail.co.kr, john-doe@sub.domain.net '가능'
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    	if(!trimEmail.matches(regex)) {
    		logger.error("EmailValidation isValidEmail() '두번째 IF문' trimEmail   :" + trimEmail);
    		return false;
    	}

    	// '@'기준으로 분리 후 도메인 길이 검사
    	String[] parts = trimEmail.split("@");
    	if(parts.length != 2) {
    		logger.error("EmailValidation isValidEmail() '세번째 IF문' trimEmail   :" + trimEmail);
    		// '@'기준으로 분리하면 무조건 parts 배열의 길이는 '2'이다.
    		return false;
    	}

    	int trimEmailLength = trimEmail.length();

    	if(trimEmailLength < 4 || trimEmailLength > 100) {
    		logger.error("EmailValidation isValidEmail() '네번쨰 IF문' trimEmail   :" + trimEmail);
    		return false;
    	}
    	
        String domain = parts[1];
        if (domain.length() > 25) {
        	logger.error("EmailValidation isValidEmail() '다섯번째 IF문' trimEmail   :" + trimEmail);
            return false;
        }

        logger.info("EmailValidation isValidEmail() PASS '" + trimEmail +"' 는 사용가능한 이메일입니다.");
        return true;
    }

}
