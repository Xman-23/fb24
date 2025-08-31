package com.example.demo.validation.string;

public class WordValidation {

	// 닉네임, 게시글, 댓글 비속어 
	public static final String[] BANNED_LIST = {
		    // 한글 비속어
		    "시발", "씨발", "씨팔", "십새", "개새", "개같", "븅신", "병신",
		    "미친", "미쳣", "엿같", "좆", "존나", "지랄", "자지", "보지",
		    "후장", "엉덩이", "창녀", "걸레", "떡치", "싸발", "애미", "애비",
		    "놈년", "놈새", "느금", "틀딱", "김치녀", "된장녀", "한남", "메갈",
		    "일베", "급식충", "관종", "노답", "폐급",

		    // 우회 표현
		    "ㅅㅂ", "ㅄ", "ㅁㅊ", "ㅁㅊ놈", "ㅈ같", "ㅊㄴ", "ㅂㅅ",
		    "븅", "븅ㅅ", "병1신", "시1발", "씨1발", "ㅈ1랄", "미1친",

		    // 영어 비속어
		    "fuck", "shit", "bitch", "bastard", "ass", "asshole", "dick",
		    "pussy", "slut", "jerk", "whore", "nigger", "faggot", "gay",
		    "retard", "crap", "hell", "damn"
	};

    // 비속어 유효성 검사
    public static boolean containsForbiddenWord(String string) {

    	if(string == null || string.isEmpty() || string.equals("")) {
    		return false;
    	}

    	String trimToLowerCaseNickname = string.toLowerCase().trim();

    	for(int i=0; i<BANNED_LIST.length; i++) {
    		String toLowerCaseBannedNickname = BANNED_LIST[i].toLowerCase();
    		// 'equals'가 아닌 'contains'를 사용하는 이유:
    		// 비속어 단어가 전체 문장에 '포함(contains)'되어 있기만 해도 부적절한 표현으로 간주해야 하기 때문.
    		// 예를 들어, '미친놈'은 '미친'이라는 금지어를 포함하고 있으므로 걸러야 하지만,
    		// 'equals'를 사용하면 '미친'과 정확히 일치하는 경우만 걸러지기 때문에 누락될 수 있다. 그러므로 'contains'를 사용해야한다. 
    		if(trimToLowerCaseNickname.contains(toLowerCaseBannedNickname)) {
    			return false; // 닉네임에 비속어가 있다면은 'false'
    		}
    	}
    	return true; // 닉네임에 비속어가 없다면은 'true'
    }

    // 자음, 모음 유효성 검사
    private static boolean isOnlyConsonantOrVowel(String string) {

    	if(string == null) {
    		return false;
    	}

    	String regex = "^[ㄱ-ㅎㅏ-ㅣ]+$";

    	return string.matches(regex);
    }

    // 닉네임 유효성 검사
    public static boolean isValidNickname(String string) {

    	if (string ==  null) {
    		return false;
    	}

    	// 완성형 한글, 영문, 숫자만 허용, 특수문자 불가
    	// 막는 것: 특수문자, 이모지, 띄어쓰기, 자음·모음 ‘외’의 이상한 문자들
    	String regex = "^[가-힣a-zA-Z0-9]{2,20}$";

    	String trimNickname = string.trim();

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
