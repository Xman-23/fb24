package com.example.demo.service.notification;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.dto.notification.NotificationPageResponseDTO;
import com.example.demo.dto.notification.NotificationResponseDTO;

 public interface NotificationService {

    // 알림 생성 - 게시글 좋아요
    void notifyPostLike(PostReaction postReaction);

    // 알림 생성 - 게시글 댓글 작성
    void notifyPostComment(Comment comment);

    // 알림 생성 - 댓글 좋아요
    void notifyCommentLike(CommentReaction commentReaction);

    void notifyPostWarned(Post post);

    // 알림 생성 - 대댓글
    void notifyChildComment(Comment childComment);

    // 알림 생성 - 댓글 경고 삭제
    void notifyCommentWarned(Comment comment);

    // 최신 4개 게시글 알림 조회
    List<NotificationResponseDTO> getRecentPostNotifications(Long receiverId);

    // 최신 4개 댓글 알림 조회
    List<NotificationResponseDTO> getRecentCommentNotifications(Long receiverId);

    // 전체 게시글 알림 페이지 조회 
    NotificationPageResponseDTO getPostNotifications(Long receiverId, Pageable pageable);

    // 전체 댓글 알림 페이지 조회
    NotificationPageResponseDTO getCommentNotifications(Long receiverId, Pageable pageable);

    // 총 알림 갯수
    long countAllNotifications(Long receiverId);

    // 게시글 알림 갯수
    long countPostNotifications(Long receiverId);

    // 댓글 알림 갯수
    long countCommentNotifications(Long receiverId);

    // 전체 알림 삭제
    void softDeleteAllNotifications(Long receiverId);

    // 단건 알림 삭제
    int softDeleteNotification(Long receiverId, Long notificationId);

    // 모두 읽음 처리
    void markAllAsRead(Long receiverId);

    // 단건 읽음 처리
    void markAsRead(Long notificationId, Long receiverId);

}
