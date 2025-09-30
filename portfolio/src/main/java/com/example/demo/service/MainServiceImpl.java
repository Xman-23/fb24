package com.example.demo.service;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.comment.commentenums.CommentStatus;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.dto.post.PostBoardPostSearchPageResponseDTO;
import com.example.demo.dto.post.PostListResponseDTO;
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

	private final String DEFAULT_THUMBNAIL_URL = "/images/default/default-thumbnail.jpg";

    // 실시간 검색 5개 
    private static final Pageable SEARCH_SIZE = PageRequest.of(0, 5);

	private final CommentStatus COMMENT_ACTIVE = CommentStatus.ACTIVE;

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
	
	
	// 좋아요 집계 Map 생성 메서드
	private Map<Long, Long> getLikeCountMap(List<Long> postIds) {

		// 게시글 아이디가 비어있을경우
		if(postIds.isEmpty()) {
			// 빈 맵 반환
			return Collections.emptyMap();
		}

		List<PostReactionRepository.PostLikeReactionCount> reactionCounts = postReactionRepository.countLikeReactionsByPostIds(postIds);

		int initialCapacity = (int) (reactionCounts.size()/0.75f) +1;
		Map<Long, Long> likeCountMap = new HashMap<>(initialCapacity);

		reactionCounts.forEach(rc -> likeCountMap.put(rc.getPostId(), rc.getLikeCount()));

		return likeCountMap;
	}

	//*************************************************** Method Start ***************************************************//

	private MainPostPageResponseDTO emptyPage(Pageable pageable) {
		return MainPostPageResponseDTO.builder()
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

	// 댓글 집계 Map 생성 메서드
	private Map<Long, Long> getCommentCountMap(List<Long> postIds) {

		if(postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<CommentRepository.PostCommentCount> commentCounts = commentRepository.countCommentsByPostIds(postIds, COMMENT_ACTIVE);

		int initialCapacity = (int) (commentCounts.size()/0.75f) +1;
		Map<Long, Long> commentCountMap = new HashMap<>(initialCapacity);

		commentCounts.forEach(cc -> commentCountMap.put(cc.getPostId(), cc.getCommentCount()));

		return commentCountMap;
	}

	// 작성자 닉네임 Map 생성 메서드
	private Map<Long, String> getNicknameMap(List<Post> posts) {

		if(posts.isEmpty()) {
			return Collections.emptyMap();
		}

		Set<Long> authorIds = posts.stream()
				                   .map(post -> post.getAuthor().getId())
				                   .collect(Collectors.toSet());

		if(authorIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 해당 게시판의 모든 작성자 닉네임 가져오기
		List<Member> members = memberRepository.findAllById(authorIds);

		int initialCapacity = (int) (members.size()/0.75f) +1;
		Map<Long, String> nicknameMap = new HashMap<>(initialCapacity);

		members.forEach(m -> nicknameMap.put(m.getId(), m.getNickname()));

		return nicknameMap;
	}

	// 썸네일 이미지 가져오기
	private Map<Long, String> getThumbnails(List<Long> postIds) {

		if(postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<PostImageRepository.PostThumbnail> thumbnails = postImageRepository.findThumbnailsByPostIds(postIds);

		int initialCapacity = (int) (thumbnails.size()/0.75f) +1;
		Map<Long, String> thumbnailMap = new HashMap<>(initialCapacity);

	    // DB에서 가져온 값 매핑,
		// 만약 해당 게시글ID에 대표이미지가 없어 'null'로 키값을 셋팅될경우,
		// 삼항연산자로 대표이미지 세팅
	    thumbnails.forEach(th -> thumbnailMap.put(th.getPostId(), th.getImageUrl() != null ? th.getImageUrl() : DEFAULT_THUMBNAIL_URL));


		return thumbnailMap;
	}

	//*************************************************** Method End ***************************************************//

	//*************************************************** Service Start ***************************************************//	

	// 모든 자식게시판 인기글 조회 
	@Override
	public MainPostPageResponseDTO getMainPopularPosts(Pageable pageable) {

		logger.info("MainServiceImpl getMainPopularPosts() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		// 게시판이 존재 하지 않을경우
		if(allChildBoardIds.isEmpty()) {
			// 빈 페이지 반환
			return emptyPage(pageable);
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
			return emptyPage(pageable);
		}

		/**
		 * HashMap 초기 크기 설정
		 * HashMap은 부하율이라는게 존재, HashMap 데이터가 75프로 차지하게 된다면,
		 * HashMap은 자동으로 배열 크기를 늘리고, 데이터를 재배치하는 '리사이즈'를 수행
		 * 그러므로 '리사이즈'를 방지하기 위해서 HashMap의 초기 크기 (list.size/0.75f)+1를 설정
		 */
		// 3. 좋아요 일괄 조회
		List<PostReactionRepository.PostLikeReactionCount> reactionCounts = postReactionRepository.countLikeReactionsByPostIds(postIds);
		int likeInitialCapacity = (int) (reactionCounts.size()/0.75f)+1;
		Map<Long, Long> likeCountMap = new HashMap<>(likeInitialCapacity);
		reactionCounts.forEach(rc-> likeCountMap.put(rc.getPostId(), rc.getLikeCount()));

	    // 4. 댓글 수 일괄 조회 + 초기 용량 설정
	    List<CommentRepository.PostCommentCount> commentCounts = commentRepository.countCommentsByPostIds(postIds, COMMENT_ACTIVE);
	    int commentInitialCapacity = (int) (commentCounts.size() / 0.75f) + 1;
	    Map<Long, Long> commentCountMap = new HashMap<>(commentInitialCapacity);
	    commentCounts.forEach(cc -> commentCountMap.put(cc.getPostId(), cc.getCommentCount()));

	    // 5. 썸네일 이미지 URL 일괄 조회 + 초기 용량 설정
	    List<PostImageRepository.PostThumbnail> thumbnails = postImageRepository.findThumbnailsByPostIds(postIds);
	    int thumbnailInitialCapacity = (int) (thumbnails.size() / 0.75f) + 1;
	    Map<Long, String> thumbnailMap = new HashMap<>(thumbnailInitialCapacity);
	    // DB에서 가져온 값 매핑,
		// 만약 해당 게시글ID에 대표이미지가 없어 'null'로 키값을 셋팅될경우,
		// 삼항연산자로 대표이미지 세팅
	    thumbnails.forEach(th -> thumbnailMap.put(th.getPostId(), th.getImageUrl() != null ? th.getImageUrl() : DEFAULT_THUMBNAIL_URL));

	    // 6. 작성자 닉네임 일괄 조회 + 초기 용량 설정
	    Set<Long> authorIds = mainPopularPost.stream()
	                                         .map(post -> post.getAuthor().getId())
	                                         .collect(Collectors.toSet());

	    List<Member> members = memberRepository.findAllById(authorIds);
	    int nicknameInitialCapacity = (int) (members.size() / 0.75f) + 1;
	    Map<Long, String> nicknameMap = new HashMap<>(nicknameInitialCapacity);
	    members.forEach(m -> nicknameMap.put(m.getId(), m.getNickname()));

    	List<PostListResponseDTO> dtoList = mainPopularPost.stream()
    	                                                   .map(post -> PostListResponseDTO.fromEntity(post,
    	                                                                                               likeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
    	                                                                                               nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
    	                                                                                               commentCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
    	                                                                                               thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)
    	                                                                                               ))
    	                                                   .collect(Collectors.toList());

    	Page<PostListResponseDTO> dtoPage = new PageImpl<>(dtoList, mainPopularPost.getPageable(), mainPopularPost.getTotalElements());

    	logger.info("MainServiceImpl getMainPopularPosts() End");
    	return MainPostPageResponseDTO.fromPage(dtoPage);
	}

	// 메인 인기글 키워드 검색
	@Override
	public MainPostPageResponseDTO getMainPopularPostsSearch(String keyword, Pageable pageable) {

		logger.info("MainServiceImpl getMainPopularPostsSearch() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		// 게시판이 존재 하지 않을경우
		if(allChildBoardIds.isEmpty()) {
			// 빈 페이지 반환
			return emptyPage(pageable);
		}

		// 2. 인기글 조회(페이징 포함)
		Page<Post> mainPopularPost = mainPostRepository.findMainPopularPostsKeyword(allChildBoardIds, 
				                                                                    likeThreshold, 
				                                                                    netLikeThreshold, 
				                                                                    mainDayLimit,
				                                                                    keyword,
				                                                                    pageable);
		
	    // ID 리스트 및 Map 생성
	    List<Long> searchIds = mainPopularPost.stream()
	    								      .map(Post::getPostId)
	    								      .toList();

		// 인기글 ID가 없다면 빈페이지 반환
		if (searchIds.isEmpty()) {
			return emptyPage(pageable);
		}

	    
	    Map<Long, Long> likeMap = getLikeCountMap(searchIds);
	    Map<Long, String> nicknameMap = getNicknameMap(mainPopularPost.getContent());
	    Map<Long, Long> commentMap = getCommentCountMap(searchIds);
	    Map<Long, String> thumbnailMap = getThumbnails(searchIds);
	    
	    // DTO 변환
	    Page<PostListResponseDTO> searchDtoPage = mainPopularPost.map(post -> PostListResponseDTO.fromEntity(post,
	                    																					likeMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	                    																					commentMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)));

		logger.info("MainServiceImpl getMainPopularPostsSearch() End");

		return MainPostPageResponseDTO.fromPage(searchDtoPage);
	}

	// 메인 인기글 작성자 검색
	@Override
	public MainPostPageResponseDTO getMainPopularPostsAuthor(String nickname, Pageable pageable) {
		logger.info("MainServiceImpl getMainPopularPostsAuthor() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		// 게시판이 존재 하지 않을경우
		if(allChildBoardIds.isEmpty()) {
			// 빈 페이지 반환
			return emptyPage(pageable);
		}

	    Member member = memberRepository.findByNickname(nickname)
				                        .orElseThrow(() -> {
				                        	logger.info("MainServiceImpl getMainPopularPostsAuthor() NoSuchElementException: 회원이 존재하지 않습니다.");
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });
		// 2. 인기글 조회(페이징 포함)
		Page<Post> mainPopularPost = mainPostRepository.findMainPopularPostsAuthor(allChildBoardIds, 
				                                                                   likeThreshold, 
				                                                                   netLikeThreshold, 
				                                                                   mainDayLimit,
				                                                                   member.getId(),
				                                                                   pageable);
		
	    // ID 리스트 및 Map 생성
	    List<Long> searchIds = mainPopularPost.stream()
	    								      .map(Post::getPostId)
	    								      .toList();

		// 인기글 ID가 없다면 빈페이지 반환
		if (searchIds.isEmpty()) {
			return emptyPage(pageable);
		}

	    
	    Map<Long, Long> likeMap = getLikeCountMap(searchIds);
	    Map<Long, String> nicknameMap = getNicknameMap(mainPopularPost.getContent());
	    Map<Long, Long> commentMap = getCommentCountMap(searchIds);
	    Map<Long, String> thumbnailMap = getThumbnails(searchIds);
	    
	    // DTO 변환
	    Page<PostListResponseDTO> searchDtoPage = mainPopularPost.map(post -> PostListResponseDTO.fromEntity(post,
	                    																					likeMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	                    																					commentMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)));

		logger.info("MainServiceImpl getMainPopularPostsAuthor() End");

		return MainPostPageResponseDTO.fromPage(searchDtoPage);
	}

	// 실시간 검색 
	@Override
	public List<String> getMainPopularPostsAutoComplete(String keyword) {
		logger.info("MainServiceImpl getMainPopularPostsAutoComplete() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		Page<String> string =mainPostRepository.findMainPopularPostsAutoComplete(allChildBoardIds, 
				                                                                 likeThreshold, 
				                                                                 netLikeThreshold, 
				                                                                 mainDayLimit, 
				                                                                 keyword, 
				                                                                 SEARCH_SIZE);
	
		List<String> respose = string.getContent();

		logger.info("MainServiceImpl getMainPopularPostsAutoComplete() Start");
		return respose;
	}

	@Override
	public MainPostPageResponseDTO getMainPopularPostsAutoCompleteSearch(String title, Pageable pageable) {
		logger.info("MainServiceImpl getMainPopularPostsAutoCompleteSearch() Start");

		// 1. 모든 게시판 ID 조회 
		List<Long> allChildBoardIds = boardRepository.findAllChildBoardIds();

		// 게시판이 존재 하지 않을경우
		if(allChildBoardIds.isEmpty()) {
			// 빈 페이지 반환
			return emptyPage(pageable);
		}

		// 2. 인기글 조회(페이징 포함)
		Page<Post> mainPopularPost = mainPostRepository.findMainPopularPostsAutoCompleteSearch(allChildBoardIds, 
				                                                                               likeThreshold, 
				                                                                               netLikeThreshold, 
				                                                                               mainDayLimit,
				                                                                               title,
				                                                                               pageable);
		
	    // ID 리스트 및 Map 생성
	    List<Long> searchIds = mainPopularPost.stream()
	    								      .map(Post::getPostId)
	    								      .toList();

		// 인기글 ID가 없다면 빈페이지 반환
		if (searchIds.isEmpty()) {
			return emptyPage(pageable);
		}

	    
	    Map<Long, Long> likeMap = getLikeCountMap(searchIds);
	    Map<Long, String> nicknameMap = getNicknameMap(mainPopularPost.getContent());
	    Map<Long, Long> commentMap = getCommentCountMap(searchIds);
	    Map<Long, String> thumbnailMap = getThumbnails(searchIds);
	    
	    // DTO 변환
	    Page<PostListResponseDTO> searchDtoPage = mainPopularPost.map(post -> PostListResponseDTO.fromEntity(post,
	                    																					likeMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	                    																					commentMap.getOrDefault(post.getPostId(), 0L).intValue(),
	                    																					thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)));

		logger.info("MainServiceImpl getMainPopularPostsAutoCompleteSearch() End");

		return MainPostPageResponseDTO.fromPage(searchDtoPage);
	}

	//*************************************************** Service End ***************************************************//	

}
