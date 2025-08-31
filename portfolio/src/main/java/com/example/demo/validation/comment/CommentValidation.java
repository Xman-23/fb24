package com.example.demo.validation.comment;

public class CommentValidation {

	public static boolean isValidCommentId (Long commentId) {
		if(commentId == null || commentId <= 0) {
			return false;
		}

		return true;
	}

	public static boolean isValidSortBy (String sortBy) {
		if(sortBy == null) {
			return false;
		}

		return "recent".equalsIgnoreCase(sortBy) || "like".equalsIgnoreCase(sortBy) || "normal".equalsIgnoreCase(sortBy);
	}

}
