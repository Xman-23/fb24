package com.example.demo.service.post.postreaction;

import java.util.List;


import java.util.NoSuchElementException;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.dto.post.postreaction.PostReactionRequestDTO;
import com.example.demo.dto.post.postreaction.PostReactionResponseDTO;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.repository.post.postreaction.PostReactionRepository;
import com.example.demo.repository.post.postreaction.PostReactionRepository.PostReactionCount;
import com.example.demo.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
class PostReactionServiceImpl implements PostReactionService {

	private final MemberRepository memberRepository;
	private final PostReactionRepository postReactionRepository;
	private final PostRepository postRepository;
	private final NotificationService notificationService;

	// 포스트 리액션
	private final PostReactionType POST_LIKE = PostReactionType.LIKE;
	private final PostReactionType POST_DISLIKE = PostReactionType.DISLIKE;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(PostReactionServiceImpl.class);	

	//*************************************************** Service START ***************************************************//

	// 게시글 리액션
	@Override
	public PostReactionResponseDTO reactionToPost(Long postId, Long memberId,
			                                      PostReactionRequestDTO postReactionRequestDTO) {

		logger.info("PostReactionServiceImpl reactionToPost() Start");

		// 게시글 조회
		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
				                	  logger.error("PostReactionServiceImpl reactionToPost() NoSuchElementException postId : {}", postId);
				                	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
				                  });

		// 멤버 조회
		Member member = memberRepository.findById(memberId)
				                        .orElseThrow(() -> {
				                        	logger.error("PostReactionServiceImpl reactionToPost() NoSuchElementException memberId : {}", memberId);
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		// 삭제된 댓글 반응 X
		if(post.getStatus().equals(PostStatus.DELETED)) {
			logger.error("PostReactionServiceImpl reactionToPost() IllegalStateException : 삭제된 게시글입니다.");
		    throw new IllegalStateException("삭제된 게시글입니다.");
		}

		// 신고된 댓글 반응 X
		if(post.getStatus().equals(PostStatus.BLOCKED)) {
			logger.error("PostReactionServiceImpl reactionToPost() IllegalStateException : 신고된 게시글입니다.");
		    throw new IllegalStateException("신고된 게시글입니다.");
		}

		// 게시글 기존 반응 조회 
		// 해당 게시글에 회원의 최초진입이라, 'reaction'이 없을 수 도 있어, 'orElseThrow()'로 예외 발생 X 
		Optional<PostReaction> existingPostReaction = postReactionRepository.findByPostAndUserId(post, memberId);
		// 클라이언트 요청 반응 가져오기
		PostReactionType dtoNewReactionType = postReactionRequestDTO.getReactionType();
		// 업데이트된 새로운 반응 조회없이 가져오기 위한 변수
		PostReactionType newReactionType = null;

		// 'Optional' 만약 해당 게시글에 회원의 '반응'이 존재한다면
		if(existingPostReaction.isPresent()) {
			// 'Optional'에서 'PostReaction' 가져오기
			PostReaction existingReaction = existingPostReaction.get();
			PostReactionType existionPostReactionType = existingReaction.getReactionType();

			// 만약 클라이언트의 요청 '반응'과 기존의 '반응'이 같다면은
			if(existionPostReactionType == dtoNewReactionType) {
				if(existionPostReactionType.equals(POST_LIKE)) {
					//그리고 그 반응이 '좋아요'라면은 '좋아요 점수(+5)' 원상복구(-5)
					member.cancelPostLikeScore();
				}else if(existionPostReactionType.equals(POST_DISLIKE)) {
					// 혹은 그 반응이 '싫어요'라면은 '싫어요 점수(-3)' 원상 복구(+3)
					member.cancelPostDislikeScore();
				}
				// 기존 반응 삭제
				postReactionRepository.delete(existingReaction);
				// 반응이 삭제되어 없으므로 'null'
				newReactionType = null;
			}else {
				// 만약 클라이언트의 요청 '반응'과 기존의 '반응'이 다르면은
				// 새로운 반응으로 업데이트 ('좋아요' -> '싫어요' 또는 '싫어요' -> '좋아요')
				existingReaction.setReactionType(dtoNewReactionType);

				// 새로운 반응으로 업데이트된 리액션 타입을 가져와 그 리액션이 좋아요라면은
				if(existingReaction.getReactionType().equals(POST_LIKE)) {
					// '싫어요'-> '좋아요'상태 즉, 싫어요 점수 압수(+3) 원상복구
					member.cancelPostDislikeScore();
					// 좋아요, 멤버등급 +5점
					member.addPostLikeScore();
					newReactionType = POST_LIKE;
				}else if(existingReaction.getReactionType().equals(POST_DISLIKE)) {
					// 싫어요라면은 '좋아요' -> '싫어요'상태 즉, 좋아요 점수 압수(-5) 원상 복구
					member.cancelPostLikeScore();
					// 싫어요, 멤버등급 -3점
					member.addPostDislikeScore();
					newReactionType = POST_DISLIKE;
				}
				// 만약 변경한 반응이 '좋아요'일 경우
				if(dtoNewReactionType == POST_LIKE) {
					notificationService.notifyPostLike(existingReaction);
				}
				postReactionRepository.save(existingReaction);
			}
		}else {
			// 회원이 게시글에 최초진입하여 반응버튼을 누를시
			// 새로운 반응 생성
			PostReaction newReaction =  PostReaction.builder()
					                                .post(post)
					                                .userId(memberId)
					                                .reactionType(dtoNewReactionType)
					                                .build();

			// 만약 처음 누른 리액션이 좋아요 라면은
			if(dtoNewReactionType.equals(POST_LIKE)) {
				// 좋아요, 멤버 점수 +5
				member.addPostLikeScore();
				newReactionType = POST_LIKE;
			}else if(dtoNewReactionType.equals(POST_DISLIKE)) {
				// 만약 처음 누른 리액션이 싫어요 라면은
				// 싫어요 , 멤버 점수 -3 (만약 등급 점수가 음수로 떨어질시 '0'으로 초기화)
				member.addPostDislikeScore();
				newReactionType = POST_DISLIKE;
			}

			postReactionRepository.save(newReaction);

			// 새로운 반응이 좋아요일 경우 알림바송
			if(dtoNewReactionType == POST_LIKE) {
				notificationService.notifyPostLike(newReaction);
			}
		}

		PostReactionCount postReactionCount = postReactionRepository.countReactionsByPostId(postId);
		int likeCount = postReactionCount.getLikeCount().intValue();
		int disLikeCount = postReactionCount.getDislikeCount().intValue();

		logger.info("PostReactionServiceImpl reactionToPost() End");
		return PostReactionResponseDTO.fromEntityToDto(postId, likeCount, disLikeCount, newReactionType);
	}

	// 배치 처리할 메서드
	// 배치로 휴먼 계정 반응 삭제 ( 0 0 3 * * ? => 매일 새벽 3시)
	@Override
	public void postRemoveReactionsByDeadtUsers(List<Long> userIds) {

		logger.info("PostReactionServiceImpl removeReactionsByDormantUsers() Start");

		// 휴먼 계정 '게시글 반응 삭제'
		postReactionRepository.deleteAllByUserIdIn(userIds);

		logger.info("PostReactionServiceImpl removeReactionsByDormantUsers() End");

	}

	//*************************************************** Service End ***************************************************//

}
