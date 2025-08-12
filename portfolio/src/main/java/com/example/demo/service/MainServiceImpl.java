package com.example.demo.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.dto.MainPopularPostDTO;
import com.example.demo.dto.MainPopularPostPageResponseDTO;
import com.example.demo.repository.MainPostRepository;
import com.example.demo.repository.board.BoardRepository;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.post.postimage.PostImageRepository;
import com.example.demo.repository.post.postreaction.PostReactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class MainServiceImpl implements MainService {

	private final MainPostRepository mainPostRepository;
	private final BoardRepository boardRepository;
	private final PostReactionRepository postReactionRepository;
	private final CommentRepository commentRepository;
	private final PostImageRepository postImageRepository;
	private final MemberRepository memberRepository;

	private final String DEFAULT_THUMBNAIL_URL = "/images/default-thumbnail.png";

	// 메인 인기글 좋아요 기준
    @Value("${main.popular.likeThreshold}")
    private int likeThreshold;

    // 메인 좋아요, 싫어요 차이 기준
    @Value("${main.popular.postNetLikeThreshold}")
    private int netLikeThreshold;

    // 메인 인기글 날짜 기준
    @Value("${main.popular.mainDayLimit}")
    private int mainDayLimit;

	//로그
	private static final Logger logger = LoggerFactory.getLogger(MainServiceImpl.class);

	// 모든 자식게시판 인기글 조회 
	@Override
	public MainPopularPostPageResponseDTO getMainPopularPosts(Pageable pageable) {

		logger.info("MainServiceImpl getMainPopularPosts() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		// 게시판이 존재 하지 않을경우
		if(allChildBoardIds.isEmpty()) {
			// 빈 페이지 반환
			return MainPopularPostPageResponseDTO.builder()
					                             .posts(Collections.emptyList())
					                             .pageNumber(pageable.getPageNumber())
					                             .pageSize(pageable.getPageSize())
					                             .totalElements(0)
					                             .totalPages(0)
					                             .hasPrevious(false)
					                             .hasNext(false)
					                             .hasFirst(false)
					                             .hasLast(false)
					                             .jumpBackwardPage(0)
					                             .jumpForwardPage(0)
					                             .build();
		}

		// 2. 인기글 조회(페이징 포함)
		Page<Post> mainPopularPost = mainPostRepository.findMainPopularPosts(allChildBoardIds, 
				                                                             likeThreshold, 
				                                                             netLikeThreshold, 
				                                                             mainDayLimit, 
				                                                             pageable);

		// 모든 인기글ID 구하기
		List<Long> postIds = mainPopularPost.stream()
				                            .map(post -> post.getPostId())
				                            .collect(Collectors.toList());

		// 인기글 ID가 없다면 빈페이지 반환
		if (postIds.isEmpty()) {
			return MainPopularPostPageResponseDTO.builder()
                                                 .posts(Collections.emptyList())
                                                 .pageNumber(pageable.getPageNumber())
                                                 .pageSize(pageable.getPageSize())
                                                 .totalElements(0)
                                                 .totalPages(0)
                                                 .hasPrevious(false)
                                                 .hasNext(false)
                                                 .hasFirst(false)
                                                 .hasLast(false)
                                                 .jumpBackwardPage(0)
                                                 .jumpForwardPage(0)
                                                 .build();
		}

		/**
		 * HashMap 초기 크기 설정
		 * HashMap은 부하율이라는게 존재, HashMap 데이터가 75프로 차지하게 된다면,
		 * HashMap은 자동으로 배열 크기를 늘리고, 데이터를 재배치하는 '리사이즈'를 수행
		 * 그러므로 '리사이즈'를 방지하기 위해서 HashMap의 초기 크기 (list.size/0.75f)+1를 설정
		 */
		// 3. 좋아요 일괄 조회
		List<PostReactionRepository.PostReactionCount> reactionCounts = postReactionRepository.countReactionsByPostIds(postIds);
		int likeInitialCapacity = (int) (reactionCounts.size()/0.75f)+1;
		Map<Long, Long> likeCountMap = new HashMap<>(likeInitialCapacity);
		reactionCounts.forEach(rc-> likeCountMap.put(rc.getPostId(), rc.getLikeCount()));

	    // 4. 댓글 수 일괄 조회 + 초기 용량 설정
	    List<CommentRepository.PostCommentCount> commentCounts = commentRepository.countCommentsByPostIds(postIds);
	    int commentInitialCapacity = (int) (commentCounts.size() / 0.75f) + 1;
	    Map<Long, Long> commentCountMap = new HashMap<>(commentInitialCapacity);
	    commentCounts.forEach(cc -> commentCountMap.put(cc.getPostId(), cc.getCommentCount()));

	    // 5. 썸네일 이미지 URL 일괄 조회 + 초기 용량 설정
	    List<PostImageRepository.PostThumbnail> thumbnails = postImageRepository.findThumbnailsByPostIds(postIds);
	    int thumbnailInitialCapacity = (int) (thumbnails.size() / 0.75f) + 1;
	    Map<Long, String> thumbnailMap = new HashMap<>(thumbnailInitialCapacity);
	    thumbnails.forEach(th -> thumbnailMap.put(th.getPostId(), th.getImageUrl()));

	    // 6. 작성자 닉네임 일괄 조회 + 초기 용량 설정
	    Set<Long> authorIds = mainPopularPost.stream()
	                                         .map(post -> post.getAuthor().getId())
	                                         .collect(Collectors.toSet());

	    List<Member> members = memberRepository.findAllById(authorIds);
	    int nicknameInitialCapacity = (int) (members.size() / 0.75f) + 1;
	    Map<Long, String> nicknameMap = new HashMap<>(nicknameInitialCapacity);
	    members.forEach(m -> nicknameMap.put(m.getId(), m.getNickname()));

		// 7. DTO변환
		List<MainPopularPostDTO> dtoList = mainPopularPost.stream()
				                                          .map(post -> MainPopularPostDTO.builder()
				                                                    		             .postId(post.getPostId())
				                                                    		             .title(post.getTitle())
				                                                    		             .authorNickname(nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"))
				                                                    		             .likeCount(likeCountMap.getOrDefault(post.getPostId(), 0L).intValue())
				                                                    		             .commentCount(commentCountMap.getOrDefault(post.getPostId(), 0L).intValue())
				                                                    		             .thumbnailUrl(thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL))
				                                                    		             .createdAt(post.getCreatedAt())
				                                                    		             .build())
				                                          .collect(Collectors.toList());

		// 8. PageImpl 생성 (페이지당 뿌려줄 데이터, 페이지 size, 총 데이터 개수
		Page<MainPopularPostDTO> dtoPage = new PageImpl<>(dtoList, mainPopularPost.getPageable(), mainPopularPost.getTotalElements());

		// 9. response
		MainPopularPostPageResponseDTO response = MainPopularPostPageResponseDTO.fromPage(dtoPage);


		logger.info("MainServiceImpl getMainPopularPosts() End");
        return response;
	}

}
