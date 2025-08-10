package com.example.demo.service.comment.commentreaction;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.dto.comment.commentreaction.CommentReactionRequestDTO;
import com.example.demo.dto.comment.commentreaction.CommentReactionResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.comment.commentreaction.CommentReactionRepository;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
class CommentReactionServiceImpl implements CommentReactionService {

	// 레파지토리
	private final CommentRepository commentRepository;
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

		logger.info("PostReactionServiceImpl reactionToPost() Start");

		// 댓글 조회
		Comment comment = commentRepository.findById(commentId)
				                           .orElseThrow(() -> {
				                        	   logger.error("CommentReactionServiceImpl reactionToComment : 댓글이 존재하지 않습니다.");
				                        	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
				                           });

		// 기존 반응 조회 (최초 진입하거나, 반응이 없을 수 있으므로 'orElseThrow'로 예외 처리X
		Optional<CommentReaction> existingReactionOpt = commentReactionRepository.findByCommentAndUserId(comment,memberId);
		// 새로운 반응 DTO 에서 가져오기
		PostReactionType newReactionType= commentReactionRequestDTO.getCommentReactionType();

		// 기존 반응이 존재 한다면은 반응 수정 or 삭제
		if(existingReactionOpt.isPresent()) {
			CommentReaction existingCommentReaction= existingReactionOpt.get();
			// DB에서 기존의 리액션 가져오기
			PostReactionType existingCommentReactionType = existingCommentReaction.getReactionType();
			// 기존 반응과 요청 반응이 같다면은 기존 반응삭제
			if(existingCommentReactionType == newReactionType) {
				commentReactionRepository.delete(existingCommentReaction);
			}else {
				// 기존 반응과 요청반응이 다르다면 요청 반응 셋팅 JPA 영속성 상태라 'save()' 불필요
				existingCommentReaction.setReactionType(newReactionType);
				
			}
		}else {
			// 기존 반응이 존재하지 않는다면 반응 생성
			CommentReaction commentNewReaction = CommentReaction.builder()
					                                         .comment(comment)
					                                         .userId(memberId)
					                                         .reactionType(newReactionType)
					                                         .build();

			commentReactionRepository.save(commentNewReaction);

			// 좋아요일 경우 알림 발송
			if(newReactionType == COMMENT_LIKE) {
				notificationService.notifyCommentLike(commentNewReaction);
			}
		}

		// 최신 카운트 집계
		int likeCount = commentReactionRepository.countByCommentAndReactionType(comment, COMMENT_LIKE);
		int dislikeCount = commentReactionRepository.countByCommentAndReactionType(comment, COMMENT_DISLIKE);

	    // 반응 처리 후 변경된 DB 상태를 다시 조회하여 유저 반응 타입 결정
	    // - 삭제된 경우엔 조회 결과가 없으므로 'null'이 된다
	    // - 이렇게 함으로써 클라이언트와 DB 간 상태 불일치 방지
		PostReactionType userRecentReactionType = commentReactionRepository.findByCommentAndUserId(comment, memberId)
				                                                           .map(CommentReaction :: getReactionType)
				                                                           .orElse(null);
		logger.info("PostReactionServiceImpl reactionToPost() End");

		return CommentReactionResponseDTO.fromEntityToDto(commentId, likeCount, dislikeCount, userRecentReactionType);
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
