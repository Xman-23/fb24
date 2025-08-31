package com.example.demo.service.post.postreaction;

import java.util.List;



import java.util.NoSuchElementException;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	// 게시글 리액션 처리
	@Override
	public PostReactionResponseDTO reactionToPost(Long postId, Long memberId,
	                                              PostReactionRequestDTO postReactionRequestDTO) {

	    //  게시글 조회: 요청한 postId에 해당하는 게시글이 존재하는지 확인
	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

	    // 반응을 누른 회원 조회
	    Member reactingMember = memberRepository.findById(memberId)
	                                            .orElseThrow(() -> new NoSuchElementException("회원이 존재하지 않습니다."));

	    //  게시글 작성자: 점수 반영 대상
	    Member postAuthor = post.getAuthor();

	    // 게시글 상태 체크: 삭제되거나 신고된 게시글에는 반응 금지
	    if(post.getStatus().equals(PostStatus.DELETED)) {
	        throw new IllegalStateException("삭제된 게시글입니다.");
	    }
	    if(post.getStatus().equals(PostStatus.BLOCKED)) {
	        throw new IllegalStateException("신고된 게시글입니다.");
	    }

	    // 5️⃣ 기존 반응 조회: 해당 회원이 이 게시글에 이미 반응했는지 확인
	    Optional<PostReaction> existingPostReaction = postReactionRepository.findByPostAndUserId(post, memberId);
	    // 6️⃣ 클라이언트 요청 반응 타입 (좋아요/싫어요)
	    PostReactionType dtoNewReactionType = postReactionRequestDTO.getReactionType();
	    // 7️⃣ 업데이트 후 반영될 최종 반응 타입
	    PostReactionType newReactionType = null;

	    // 기존 반응이 존재할 경우 처리
	    if(existingPostReaction.isPresent()) {
	        PostReaction existingReaction = existingPostReaction.get();
	        PostReactionType existingType = existingReaction.getReactionType();

	        if(existingType == dtoNewReactionType) {
	            // 동일한 반응을 다시 누른 경우 -> 반응 취소
	            // 점수는 게시글 작성자 기준으로 복구
	            if(existingType.equals(POST_LIKE)) {
	            	postAuthor.cancelPostLikeScore();
	            }
	            else if(existingType.equals(POST_DISLIKE)) {
	            	postAuthor.cancelPostDislikeScore();
	            }

	            // DB에서 기존 반응 삭제
	            postReactionRepository.delete(existingReaction);
	            newReactionType = null;

	        } else {
	            //기존 반응과 다른 반응을 누른 경우 -> 반응 변경
	            existingReaction.setReactionType(dtoNewReactionType);

	            if(existingReaction.getReactionType().equals(POST_LIKE)) {
	                // 기존 싫어요 점수 복구 후, 좋아요 점수 추가 (작성자 기준)
	                postAuthor.cancelPostDislikeScore();
	                postAuthor.addPostLikeScore();
	                newReactionType = POST_LIKE;
	            } else if(existingReaction.getReactionType().equals(POST_DISLIKE)) {
	                // 기존 좋아요 점수 복구 후, 싫어요 점수 추가 (작성자 기준)
	                postAuthor.cancelPostLikeScore();
	                postAuthor.addPostDislikeScore();
	                newReactionType = POST_DISLIKE;
	            }
	            // 좋아요일 경우 알림 발송
	            if(dtoNewReactionType == POST_LIKE) {
	                notificationService.notifyPostLike(existingReaction);
	            }
	            // 변경된 반응 저장
	            postReactionRepository.save(existingReaction);
	        }

	    } else {
	        // 회원이 게시글에 최초 반응을 누른 경우
	        PostReaction newReaction = PostReaction.builder()
	                                               .post(post)
	                                               .userId(memberId)
	                                               .reactionType(dtoNewReactionType)
	                                               .build();

	        // 게시글 작성자 기준으로 점수 추가
	        if(dtoNewReactionType.equals(POST_LIKE)) {
	            postAuthor.addPostLikeScore();
	            newReactionType = POST_LIKE;
	        } else if(dtoNewReactionType.equals(POST_DISLIKE)) {
	            postAuthor.addPostDislikeScore();
	            newReactionType = POST_DISLIKE;
	        }

	        // DB 저장
	        postReactionRepository.save(newReaction);

	        // 좋아요일 경우 알림 발송
	        if(dtoNewReactionType == POST_LIKE) {
	            notificationService.notifyPostLike(newReaction);
	        }
	    }

	    // 최신 카운트 집계
	    PostReactionCount reactionCount = postReactionRepository.countReactionsByPostId(postId);
	    int likeCount = reactionCount.getLikeCount().intValue();
	    int dislikeCount = reactionCount.getDislikeCount().intValue();

	    logger.info("PostReactionServiceImpl reactionToPost() End");

	    return PostReactionResponseDTO.fromEntityToDto(postId, likeCount, dislikeCount, newReactionType);
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
