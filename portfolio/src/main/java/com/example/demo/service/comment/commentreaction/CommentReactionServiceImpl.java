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
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentReactionServiceImpl reactionToComment() NoSuchElementException commentId : {} ", commentId);
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		Member member = memberRepository.findById(comment.getMember().getId())
				                        .orElseThrow(() -> {
				                        	logger.error("CommentReactionServiceImpl reactionToComment() NoSuchElementException memberId : {} ", memberId);
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		// 삭제된 댓글 반응 X
		if(comment.getStatus().equals(CommentStatus.DELETED)) {
			logger.error("CommentReactionServiceImpl reactionToComment() IllegalStateException : 삭제된 댓글입니다.");
		    throw new IllegalStateException("삭제된 댓글입니다.");
		}

		// 신고된 댓글 반응 X
		if(comment.getStatus().equals(CommentStatus.HIDDEN)) {
			logger.error("CommentReactionServiceImpl reactionToComment() IllegalStateException : 신고된 댓글입니다.");
		    throw new IllegalStateException("신고된 댓글입니다.");
		}

		// 기존 반응 조회 (최초 진입하거나, 반응이 없을 수 있으므로 'orElseThrow'로 예외 처리X
		Optional<CommentReaction> existingReactionOpt = commentReactionRepository.findByCommentAndUserId(comment,memberId);
		// 새로운 반응 DTO 에서 가져오기
		PostReactionType dtoNewReactionType= commentReactionRequestDTO.getCommentReactionType();
		// 업데이트된 새로운 반응 조회없이 가져오기 위한 변수
		PostReactionType newReactionType = null;

		// 기존 반응이 존재 한다면은 반응 수정 or 삭제
		if(existingReactionOpt.isPresent()) {
			CommentReaction existingCommentReaction= existingReactionOpt.get();
			// DB에서 기존의 리액션 가져오기
			PostReactionType existingCommentReactionType = existingCommentReaction.getReactionType();
			// 기존 반응과 요청 반응이 같다면은 기존 반응삭제
			if(existingCommentReactionType == dtoNewReactionType) {
				// 만약 기존 반응이 '좋아요(+5)'라면은 댓글 삭제시 원상복귀 
				if(existingCommentReactionType.equals(COMMENT_LIKE)) {
					member.cancelCommentLikeScore();
				}else if(existingCommentReactionType.equals(COMMENT_DISLIKE)) {
					member.cancelCommentDislikeScore();
				}
				commentReactionRepository.delete(existingCommentReaction);
				// 반응 삭제시 'null'로 반응 업데이트
				newReactionType = null;
			}else {
				// 기존 반응과 요청반응이 다르다면 요청 반응 셋팅 JPA 영속성 상태라 'save()' 불필요
				existingCommentReaction.setReactionType(dtoNewReactionType);
				// 기존 반응이 '싫어요' -> '좋아요'로 변경시
				if(existingCommentReaction.getReactionType().equals(COMMENT_LIKE)) {
					// 싫어요 점수 복구
					member.cancelCommentDislikeScore();
					// 좋아요 점수 추가
					member.addCommentLikeScore();
					// 새로운 반응으로 업데이트
					newReactionType = COMMENT_LIKE;
				}else if(existingCommentReaction.getReactionType().equals(COMMENT_DISLIKE)) {
					// 기존 반응이 '좋아요' -> '싫어요'로 변경시
					// 좋아요 점수 복구
					member.cancelCommentLikeScore();
					// 싫어요 점수 추가
					member.addCommentDislikeScore();
					// 새로운 반응으로 업데이트
					newReactionType = COMMENT_DISLIKE;
				}
				commentReactionRepository.save(existingCommentReaction);
			}
		}else {
			// 기존 반응이 존재하지 않는다면 반응 생성
			CommentReaction commentNewReaction = CommentReaction.builder()
					                                         .comment(comment)
					                                         .userId(memberId)
					                                         .reactionType(dtoNewReactionType)
					                                         .build();

			commentReactionRepository.save(commentNewReaction);

			// 만약 새로운 반응이 '좋아요'일 경우 
			if(commentNewReaction.getReactionType().equals(COMMENT_LIKE)) {
				// 좋아요 점수 추가
				member.addCommentLikeScore();
				// 새로운 반응으로 업데이트
				newReactionType = COMMENT_LIKE;
			}else if(commentNewReaction.getReactionType().equals(COMMENT_DISLIKE)) {
				// 싫어요 점수 추가
				member.addCommentDislikeScore();
				// 새로운 반응으로 업데이트
				newReactionType = COMMENT_DISLIKE;
			}

			// 좋아요일 경우 알림 발송
			if(dtoNewReactionType == COMMENT_LIKE) {
				notificationService.notifyCommentLike(commentNewReaction);
			}
		}

		// 최신 카운트 집계
		CommentReactionCount commentReactionCount = commentReactionRepository.countReactionsByCommentId(commentId);
		int likeCount = commentReactionCount.getLikeCount().intValue();
		int dislikeCount = commentReactionCount.getDislikeCount().intValue();

		logger.info("CommentReactionServiceImpl reactionToComment()");

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
