package com.example.demo.validation.password;

public class PasswordValidation {

    // 비밀번호 유효성 검사 메소드 (최소 8자, 영문+숫자+특수문자)
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(regex);
    }

}
