package com.example.demo.dto.comment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.service.comment.CommentServiceImpl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: 댓글 응답 데이터 전송 객체
 *
 * 필드 설명:
 * commentId        - 댓글 ID
 * postId           - 댓글이 속한 게시글 ID
 * parentCommentId  - 대댓글인 경우 부모 댓글 ID (댓글이면 null)
 * authorId         - 작성자 ID
 * content          - 댓글 내용
 * createdAt        - 작성 시각
 * childComments    - 자식 댓글(대댓글) 리스트
 * likeCount        - 좋아요 수
 * dislikeCount		- 싫어요 수
 * isPinned         - 고정 여부
 * updatedAgo       - "몇 분 전 수정됨" 같은 상대 시간 표시
 */



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {

    private Long commentId;

    private Long postId;

    private Long parentCommentId; // null이면 부모댓글

    private Long authorId;

    private String authorNickname;

    private String content;

    private LocalDateTime createdAt;

    private List<CommentResponseDTO> childComments; // 대댓글 리스트

    private int likeCount;

    private int dislikeCount;

    private boolean isPinned;

    private String updatedAgo; // "몇 분 전 수정됨" 등

    private CommentStatus status;

    // "삭제된 댓글입니다", "신고 받은 댓글입니다" 등
    private String deletedMessage; 

    private static final Logger logger = LoggerFactory.getLogger(CommentResponseDTO.class);

    // Entity -> DTO 변환 메서드
    public static CommentResponseDTO fromEntity(Comment comment, int likeCount, int dislikeCount, boolean isPinned, String authorNickname) {
    	logger.info("CommentResponseDTO fromEntity() Start");
    	// 여기에 시간 포맷 추가
        CommentResponseDTO dto = CommentResponseDTO.builder()
                                                   .commentId(comment.getCommentId())
                                                   .postId(comment.getPost().getPostId())
                                                   .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                                                   .authorId(comment.getMember().getId())
                                                   .createdAt(comment.getCreatedAt())
                                                   .likeCount(likeCount)
                                                   .dislikeCount(dislikeCount)
                                                   .isPinned(isPinned)
                                                   .status(comment.getStatus())
                                                   .authorNickname(authorNickname)
                                                   .content(comment.getContent())
                                                   .build();

        // 수정된 시간과 생성 시간 비교해서 updatedAg
        if(CommentStatus.ACTIVE.equals(comment.getStatus())) {
            if (!comment.getUpdatedAt().equals(comment.getCreatedAt())) {
            	// Duration.between(A,B)는 -> A-B가 아닌, B-A로 계산이 된다
                Duration duration = Duration.between(comment.getCreatedAt(), comment.getUpdatedAt()).abs();
                long minutes = duration.toMinutes();
                if(minutes < 1){ 
                	dto.setUpdatedAgo("방금 전 수정됨");
            	}else if (minutes < 60) {
                    dto.setUpdatedAgo(minutes + "분 전 수정됨");
                } else if ( minutes < 1440){
                    long hours = duration.toHours();
                    dto.setUpdatedAgo(hours + "시간 전 수정됨");
                } else {
                	// '일' 기준
                	long days = duration.toDays();

                	// '10일' 까지만 '일'로 취급하여 변경
                	if(days < 10) {
                		dto.setUpdatedAgo(days+ "일 전 수정됨");
                	}else if(days < 365) {
                		// '10'일 이후부터는 '주'로 취급하여 변경
                		long weeks = days / 7; // '주' 계산
                		dto.setUpdatedAgo(weeks + "주 전 수정됨");
                	}else {
                		// '365일' 이후부터는 '년'으로 취급하여 변경
                		long years = days/365;
                		dto.setUpdatedAgo(years + "년 전 수정됨");
                	}
                }
            } else {
            	// 수정을 안 할 경우 생성시간으로 대체
                dto.setUpdatedAgo(null);
            }
        }

        if(comment.getStatus() == CommentStatus.DELETED) {
        	dto.setDeletedMessage("삭제된 댓글입니다.");
        	dto.setContent("");  
        }else if(comment.getStatus() == CommentStatus.HIDDEN) {
        	dto.setDeletedMessage("신고받은 댓글입니다.");
        	dto.setContent("");
        }

        logger.info("CommentResponseDTO fromEntity() End");
        return dto;
    }

}