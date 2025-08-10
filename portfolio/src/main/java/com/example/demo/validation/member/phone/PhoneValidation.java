package com.example.demo.validation.member.phone;

public class PhoneValidation {

    // 휴대폰 번호 유효성 검사 메소드
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 숫자만 남기기 (하이픈, 공백 제거)
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // 정규표현식: 010으로 시작 + 10자리 또는 11자리
        String regex = "^01[016789]\\d{7,8}$";

        return cleaned.matches(regex);
    }
}
