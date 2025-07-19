package com.example.demo.security;

//Main 메서드를 갖는 클래스명은 자바 파일명 과 같아야한다
//AESTest.Class(main 메서드 보유) == AESTest.java
public class AESTest {
	public static void main(String[] args) {
		String juminNumber = "900101-1234567";
		
		//암호화
		String encrypted = AES256Util.encrypt(juminNumber);
		//복호화
		String decrypted = AES256Util.decrypt(encrypted);
		//Test
        System.out.println("원문: " + juminNumber);
        System.out.println("암호화: " + encrypted);
        System.out.println("복호화: " + decrypted);

        //예외 발생 
        //Caused by: java.security.InvalidKeyException: Invalid AES key length:  27bytes
        //글자 수 맞춰 주기 (27 -> 32)
	}
}
