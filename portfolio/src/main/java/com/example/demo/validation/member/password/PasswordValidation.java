package com.example.demo.validation.member.password;

public class PasswordValidation {

    /* 비밀번호 유효성 검사 메소드 (최소 8자, 영문+숫자+특수문자)
	   /^                            // 문자열 시작
	   (?=.*[A-Za-z])                // 최소 한 개 이상의 영어 문자(A-Z 또는 a-z) 포함
	   (?=.*\d)                      // 최소 한 개 이상의 숫자(0-9) 포함
	   (?=.*[@$!%*#?&])              // 최소 한 개 이상의 특수문자(@$!%*#?&) 포함
	   [A-Za-z\d@$!%*#?&]{8,}       // 허용된 문자(A-Z, a-z, 0-9, @$!%*#?&)로 이루어진 최소 8자 이상
	   $/                             // 문자열 끝 (시작부터 끝까지 정규표현식 조건 검사)
	*/
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        // 비밀번호 유효성 검사 메소드 (최소 8자, 영문+숫자+특수문자)
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(regex);
    }

}
