package com.example.demo.validation.string;

public class SafeTrim {

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    public static String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

}
