package com.example.demo.dto.comment;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.dto.post.PostListResponseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentListResponseDTO {

    private Long commentId;

    private Long postId;

    private Long boardId;

    private Long parentCommentId; // null이면 부모댓글

    private Long authorId;

    private String authorNickname;

    private String content;

    private String boardName;

    private String createdAt;

    private List<CommentResponseDTO> childComments; // 대댓글 리스트

    private int likeCount;

    private int dislikeCount;

    private boolean isPinned;

    private CommentStatus status;

    private long no;

    // 시간 변경
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Entity -> DTO 변환 메서드
    public static CommentListResponseDTO fromEntity(Comment comment, int likeCount) {

    	// 여기에 시간 포맷 추가
    	CommentListResponseDTO dto = CommentListResponseDTO.builder()
                                                           .commentId(comment.getCommentId())
                                                           .postId(comment.getPost().getPostId())
                                                           .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                                                           .authorId(comment.getMember().getId())
                                                           .createdAt(comment.getCreatedAt().format(formatter))
                                                           .status(comment.getStatus())
                                                           .content(comment.getContent())
                                                           .boardId(comment.getPost().getBoard().getBoardId())
                                                           .boardName(comment.getPost().getBoard().getName())
                                                           .likeCount(likeCount)
                                                           .build();
        return dto;
    }
}
