package com.example.demo.domain.comment.commentenums;

public enum CommentStatus {

	ACTIVE, // 정상 댓글
	DELETED, // 작성자 또는 관리자에 의해 삭제됨
	HIDDEN // 신고 누적으로 인해 자동 숨김 처리된 댓글

}
