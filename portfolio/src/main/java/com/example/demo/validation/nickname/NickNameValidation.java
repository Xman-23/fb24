package com.example.demo.validation.nickname;

import com.example.demo.validation.bannedwords.BannedWords;

public class NickNameValidation {

	private static final String[] nicknameBannedList = BannedWords.BANNED_LIST;

    // 비속어 유효성 검사
    private static boolean containsForbiddenWord(String nickname) {

    	String trimToLowerCaseNickname = nickname.toLowerCase().trim();

    	for(int i=0; i<nicknameBannedList.length; i++) {
    		String toLowerCaseBannedNickname = nicknameBannedList[i].toLowerCase();
    		if(trimToLowerCaseNickname.contains(toLowerCaseBannedNickname)) {
    			return false; // 닉네임에 비속어가 있다면은 'false'
    		}
    	}
    	return true; // 닉네임에 비속어가 없다면은 'true'
    }

    // 자음, 모음 유효성 검사
    private static boolean isOnlyConsonantOrVowel(String nickname) {

    	String regex = "^[ㄱ-ㅎㅏ-ㅣ]+$";

    	return nickname.matches(regex);
    }
    // 닉네임 유효성 검사
    public static boolean isValidNickname(String nickname) {

    	if (nickname ==  null) {
    		return false;
    	}

    	// 완성형 한글, 영문, 숫자만 허용, 특수문자 불가
    	// 막는 것: 특수문자, 이모지, 띄어쓰기, 자음·모음 ‘외’의 이상한 문자들
    	String regex = "^[가-힣a-zA-Z0-9]{2,20}$";

    	String trimNickname = nickname.trim();

    	// 'true' : 유효하지 않은 닉네임, 'false' : 유효한 닉네임 
    	if(!trimNickname.matches(regex)) {
    		return false;
    	}

    	// '자음', '모음'만 있는 닉네임이라면은 'false';
    	if(isOnlyConsonantOrVowel(trimNickname)) {
    		return false;
    	}

    	// '비속어'가 있는 닉네임이라면은 'false';
    	if(!containsForbiddenWord(trimNickname)) {
    		return false;
    	}

    	// 정상적인 닉네임 이라면 'true'
    	return true;
    }

}
