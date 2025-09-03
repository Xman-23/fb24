package com.example.demo.validation.post;

public class PostValidation {

	// 게시글 유효성 검사
	public static boolean isPostId(Long postId) {

		if(postId == null || postId <= 0 ) {
			return false;
		}

		return true;
	}

	// 닉네임, 검색어 유효성 검사
	public static boolean isValidString(String string) {

		if(string == null || string.trim().isEmpty()) {
			return false;
		}

		return true;
	}

	
	public static boolean isValidSortBy (String sortBy) {
		if(sortBy == null) {
			return false;
		}

		return "latest".equalsIgnoreCase(sortBy) || "popular".equalsIgnoreCase(sortBy);
	}
}
