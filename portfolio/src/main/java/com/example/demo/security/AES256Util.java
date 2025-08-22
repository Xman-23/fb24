package com.example.demo.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import jakarta.xml.bind.DatatypeConverter;

//AES(대칭키) 암호화 클래스
@Component
public class AES256Util {

	private static final String ALGORITHM = "AES";
	//16글자(128bit), 24글자(192bit) , 32글자 (256bit)
	private static final String SECRET_KEY = "aBcDeFgHiJkLmNoPqRsTuVwXyZ123456"; // 32글자 (256bit)

	//암호화에 사용할 키
	private static SecretKeySpec getSecretKey() {
								//32bit , AES
		return new SecretKeySpec(SECRET_KEY.getBytes() , ALGORITHM);
	}

	// 암호화
	public static String encrypt(String plainText) {
		try {
			//암호화 알고리즘(AES) 암호화(평문->암호문) 수행하는 'Cipher' 클래스
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			//암호화 모드
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			//1. 평문(plainText)를 바이트 배열로 변환 후,
			//2. cipher.doFinal() 메서드로 암호화 수행
			//3. 'byte 배열' 의 암호문 생성
			byte[] encrypted = cipher.doFinal(plainText.getBytes());
			//암호화된 바이트 배열을 16진수 문자열로 변환
			return DatatypeConverter.printHexBinary(encrypted);
		} catch (Exception e) {
			//'RuntimeException'운 Unchecked Exception(비체크 예외) 이므로, 
			//'RuntimeException'을 포함한 자식 'Exception'은  굳이 예외처리를 안해도 되므로,
			//throw 처리, 만약 Check Exception(체크 예외)일 경우 'throws' 키워드로 명시
			throw new RuntimeException("AES 암호화 실패", e);
		}
	}

	//복호화
	public static String decrypt(String encrypetedText) {
		try {
			//복호화(암호문->평문)을 수행하는 'Cipher' 클래스
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			//복호화 모드
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
			// '16진수' 문자열 -> 바이트 배열로 변환
			byte[] bytes = DatatypeConverter.parseHexBinary(encrypetedText);
			//복호화 수행
			byte[] decrypted = cipher.doFinal(bytes);
			//복호화된 바이트 배열을 String 클래스로 평문 문자열로 변환 후 Return
			return new String(decrypted);
		}catch (Exception e) {
			throw new RuntimeException("주민번호가 일치하지 않습니다.", e);
		}
	}

}
