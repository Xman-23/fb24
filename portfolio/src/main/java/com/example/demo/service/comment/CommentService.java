package com.example.demo.service.comment;

import com.example.demo.dto.comment.CommentPageResponseDTO;

import com.example.demo.dto.comment.CommentRequestDTO;
import com.example.demo.dto.comment.CommentResponseDTO;

import org.springframework.data.domain.Pageable;

public interface CommentService {

    // 댓글 등록 (댓글 또는 대댓글)
    CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO, Long authorId);

    // 댓글 수정
    CommentResponseDTO updateComment(Long commentId, String content, Long authorId);

    // 댓글 삭제
    CommentResponseDTO deleteComment(Long commentId, Long authorId);


    // 댓글 신고
    String reportComment(Long commentId, Long reporterId, String reason);

    // 트리구조 조회 
    CommentPageResponseDTO getCommentsTreeByPost(Long postId, String sortBy, Pageable pageable);

}
