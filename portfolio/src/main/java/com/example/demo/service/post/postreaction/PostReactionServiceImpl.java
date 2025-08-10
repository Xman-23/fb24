package com.example.demo.service.post.postreaction;

import java.util.List;


import java.util.NoSuchElementException;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.dto.post.postreaction.PostReactionRequestDTO;
import com.example.demo.dto.post.postreaction.PostReactionResponseDTO;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.repository.post.postreaction.PostReactionRepository;
import com.example.demo.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
class PostReactionServiceImpl implements PostReactionService {

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
				                	  logger.error("PostReactionServiceImpl reactionToPost() NoSuchElementException : 게시글이 존재하지 않습니다.");
				                	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
				                  });

		// 게시글 기존 반응 조회 
		// 해당 게시글에 회원의 최초진입이라, 'reaction'이 없을 수 도 있어, 'orElseThrow()'로 예외 발생 X 
		Optional<PostReaction> existingPostReaction = postReactionRepository.findByPostAndUserId(post, memberId);
		// 클라이언트 요청 반응 가져오기
		PostReactionType newReactionType = postReactionRequestDTO.getReactionType();

		// 만약 해당 게시글에 회원의 '반응'이 존재한다면
		if(existingPostReaction.isPresent()) {
			// 'Optional'에서 'PostReaction' 가져오기
			PostReaction existingReaction = existingPostReaction.get();
			PostReactionType existionPostReactionType = existingReaction.getReactionType();

			// 만약 클라이언트의 요청 '반응'과 기존의 '반응'이 같다면은
			if(existionPostReactionType == newReactionType) {
				// 기존 반응 삭제
				postReactionRepository.delete(existingReaction);
			}else {
				// 만약 클라이언트의 요청 '반응'과 기존의 '반응'이 다르면은
				// 새로운 반응으로 업데이트
				existingReaction.setReactionType(newReactionType);

				// 만약 변경한 반응이 '좋아요'일 경우
				if(newReactionType == POST_LIKE) {
					notificationService.notifyPostLike(existingReaction);
				}
			}
		}else {
			// 회원이 게시글에 최초진입하여 반응버튼을 누를시
			// 새로운 반응 생성
			PostReaction newReaction =  PostReaction.builder()
					                                .post(post)
					                                .userId(memberId)
					                                .reactionType(newReactionType)
					                                .build();
			postReactionRepository.save(newReaction);

			// 새로운 반응이 좋아요일 경우 알림바송
			if(newReactionType == POST_LIKE) {
				notificationService.notifyPostLike(newReaction);
			}
		}

		// 3. 변경된 후의 최신 좋아요/싫어요 개수 조회
		int likeCount= postReactionRepository.countByPostPostIdAndReactionType(postId, POST_LIKE);
		int disLikeCount= postReactionRepository.countByPostPostIdAndReactionType(postId, POST_DISLIKE);

	    // 반응 처리 후 변경된 DB 상태를 다시 조회하여 유저 반응 타입 결정
	    // - 삭제된 경우엔 조회 결과가 없으므로 'null'이 된다
	    // - 이렇게 함으로써 클라이언트와 DB 간 상태 불일치 방지
		PostReactionType memberRecentReactionType = postReactionRepository.findByPostAndUserId(post, memberId)
				                                                          .map(PostReaction :: getReactionType)
				                                                          // 반응 삭제시 최신 DB상태를 가져오기때문에 데이터가 없으므로, 'null'반환
				                                                          .orElse(null); 

		logger.info("PostReactionServiceImpl reactionToPost() End");
		return PostReactionResponseDTO.fromEntityToDto(postId, likeCount, disLikeCount, memberRecentReactionType);
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
