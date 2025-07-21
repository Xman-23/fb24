package com.example.demo.validation.residentnumber;

public class ResidentNumberValidation {

	public static boolean isValidResidentNumberWithChecksum(String rrn) {
	    // null 또는 빈 문자열 검사
	    if (rrn == null || rrn.trim().isEmpty()) {
	        return false;
	    }

	    // 하이픈(-) 제거 및 앞뒤 공백 제거
	    rrn = rrn.replaceAll("-", "").trim();

	    // 숫자 13자리인지 확인 (형식 검사)
	    if (!rrn.matches("^\\d{13}$")) {
	        return false;
	    }

	    /*
	      입력: 990101-1234567 → 하이픈 제거: 9901011234567

		  1) 각 자리와 가중치 곱:
          9×2 + 9×3 + 0×4 + 1×5 + 0×6 + 1×7 + 1×8 + 2×9 + 3×2 + 4×3 + 5×4 + 6×5

		  2) 계산 결과 합: sum = 9×2 + 9×3 + 0 + 5 + 0 + 7 + 8 + 18 + 6 + 12 + 20 + 30 = 115

		  3) 검증값 계산: (11 - (115 % 11)) % 10
          → (11 - 5) % 10 = 6

		  4) 마지막 자리(13번째)와 비교
	    */
	    // 주민번호 앞 12자리에 곱할 가중치 배열
	    int[] weight = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};

	    int sum = 0;

	    // 앞의 12자리에 각각 가중치를 곱한 뒤 더함
	    for (int i = 0; i < 12; i++) {
	        int digit = rrn.charAt(i) - '0';  // 문자 -> 숫자 변환
	        sum += digit * weight[i];
	    }

	    // 검증 공식: (11 - (합계 % 11)) % 10
	    int check = (11 - (sum % 11)) % 10;

	    // 주민번호의 마지막 자리(13번째 숫자)와 계산 결과 비교
	    int lastDigit = rrn.charAt(12) - '0';

	    // 같으면 유효한 주민등록번호
	    return check == lastDigit;
	}

    /*
    // DB 암호화 주민번호 유효성 검사 (복호화 주민번호 걸러내기 위한 메서드)
    public static boolean isHexBinary(String s) {
        return s.matches("[0-9A-Fa-f]+");
    }
	*/
}