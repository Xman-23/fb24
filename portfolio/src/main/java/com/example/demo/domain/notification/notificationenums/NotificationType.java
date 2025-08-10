package com.example.demo.domain.notification.notificationenums;

public enum NotificationType {

    POST_LIKE,             // 게시글 좋아요
    POST_COMMENT,          // 게시글에 댓글 작성
    POST_WARNED_DELETED,
    COMMENT_LIKE,          // 댓글 좋아요
    CHILD_COMMENT,         // 댓글에 대댓글 작성
    COMMENT_WARNED_DELETED // 댓글 경고 누적으로 삭제

}
