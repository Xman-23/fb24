package com.example.demo.service.comment.commentreaction;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.dto.comment.commentreaction.CommentReactionRequestDTO;
import com.example.demo.dto.comment.commentreaction.CommentReactionResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository.CommentReactionCount;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
class CommentReactionServiceImpl implements CommentReactionService {

	// 레파지토리
	private final CommentRepository commentRepository;
	private final MemberRepository memberRepository;
	private final CommentReactionRepository commentReactionRepository;

	// 서비스
	private final NotificationService notificationService;

	// 댓글 리액션
	private final PostReactionType COMMENT_LIKE = PostReactionType.LIKE;
	private final PostReactionType COMMENT_DISLIKE = PostReactionType.DISLIKE;

	private static final Logger logger = LoggerFactory.getLogger(CommentReactionServiceImpl.class);

	@Override
	public CommentReactionResponseDTO reactionToComment(Long commentId, 
			                                            Long memberId,
			                                            CommentReactionRequestDTO commentReactionRequestDTO) {

	    logger.info("CommentReactionServiceImpl reactionToComment() Start");

	    // 댓글 조회
	    Comment comment = commentRepository.findById(commentId)
	                                       .orElseThrow(() -> new NoSuchElementException("댓글이 존재하지 않습니다."));

	    // 반응 누른 사용자
	    Member reactingMember = memberRepository.findById(memberId)
	                                           .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

	    // 댓글 작성자
	    Member commentAuthor = comment.getMember();

	    // 삭제/신고된 댓글은 반응 불가
	    if(comment.getStatus() == CommentStatus.DELETED) {
	        throw new IllegalStateException("삭제된 댓글입니다.");
	    }
	    if(comment.getStatus() == CommentStatus.HIDDEN) {
	        throw new IllegalStateException("신고된 댓글입니다.");
	    }

	    // 기존 반응 조회
	    Optional<CommentReaction> existingReactionOpt = commentReactionRepository.findByCommentAndUserId(comment, memberId);
	    PostReactionType dtoNewReactionType = commentReactionRequestDTO.getCommentReactionType();
	    PostReactionType newReactionType = null;

	    // 기존 반응 존재
	    if(existingReactionOpt.isPresent()) {
	        CommentReaction existingReaction = existingReactionOpt.get();
	        PostReactionType existingType = existingReaction.getReactionType();

	        if(existingType == dtoNewReactionType) {
	            // 같은 반응 클릭 → 삭제
	            if(!commentAuthor.getId().equals(memberId)) { // 자기 자신 제외
	                if(existingType == COMMENT_LIKE) commentAuthor.cancelCommentLikeScore();
	                if(existingType == COMMENT_DISLIKE) commentAuthor.cancelCommentDislikeScore();
	            }
	            commentReactionRepository.delete(existingReaction);
	            newReactionType = null;
	        } else {
	            // 다른 반응으로 변경
	            if(!commentAuthor.getId().equals(memberId)) {
	                if(existingType == COMMENT_LIKE) commentAuthor.cancelCommentLikeScore();
	                if(existingType == COMMENT_DISLIKE) commentAuthor.cancelCommentDislikeScore();

	                if(dtoNewReactionType == COMMENT_LIKE) commentAuthor.addCommentLikeScore();
	                if(dtoNewReactionType == COMMENT_DISLIKE) commentAuthor.addCommentDislikeScore();
	            }
	            existingReaction.setReactionType(dtoNewReactionType);
	            commentReactionRepository.save(existingReaction);
	            newReactionType = dtoNewReactionType;
	        }

	    } else {
	        // 새로운 반응 생성
	        CommentReaction newReaction = CommentReaction.builder()
	                                                     .comment(comment)
	                                                     .userId(memberId)
	                                                     .reactionType(dtoNewReactionType)
	                                                     .build();
	        commentReactionRepository.save(newReaction);

	        // 점수 추가 (자기 자신 제외)
	        if(!commentAuthor.getId().equals(memberId)) {
	            if(dtoNewReactionType == COMMENT_LIKE) commentAuthor.addCommentLikeScore();
	            if(dtoNewReactionType == COMMENT_DISLIKE) commentAuthor.addCommentDislikeScore();
	        }

	        // 알림 발송 (자기 자신 제외)
	        if(dtoNewReactionType == COMMENT_LIKE && !commentAuthor.getId().equals(memberId)) {
	            notificationService.notifyCommentLike(newReaction);
	        }

	        newReactionType = dtoNewReactionType;
	    }

	    // 최신 카운트 집계
	    CommentReactionCount count = commentReactionRepository.countReactionsByCommentId(commentId);
	    int likeCount = count.getLikeCount().intValue();
	    int dislikeCount = count.getDislikeCount().intValue();

	    logger.info("CommentReactionServiceImpl reactionToComment() End");

		return CommentReactionResponseDTO.fromEntityToDto(commentId, likeCount, dislikeCount, newReactionType);
	}

	// 배치 처리할 메서드
	// 배치로 휴먼 계정 반응 삭제 ( 0 0 3 * * ? => 매일 새벽 3시)
	@Override
	public void commentRemoveReactionsByDeadUsers(List<Long> userIds) {
		logger.info("CommentReactionServiceImpl commentRemoveReactionsByDeadtUsers() Start");

		// 휴먼 계정 '게시글 반응 삭제'
		commentReactionRepository.deleteAllByUserIdIn(userIds);

		logger.info("CommentReactionServiceImpl commentRemoveReactionsByDeadtUsers() End");
	}

}
