package com.example.demo.service.post;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.postImage.PostImage;
import com.example.demo.dto.post.PostCreateRequestDTO;
import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.postimage.ImageOrderDTO;
import com.example.demo.dto.postimage.PostImageResponseDTO;
import com.example.demo.repository.board.BoardRepository;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.repository.postimage.PostImageRepository;
import com.example.demo.repository.postreaction.PostReactionRepository;
import com.example.demo.service.file.FileService;
import com.example.demo.validation.file.ImageFile;
import com.example.demo.validation.post.PostValidation;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // 'final', '@NonNull' 필드 생성자 생성
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

	private final MemberRepository memberRepository;
	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final BoardRepository boardRepository;
	private final CommentRepository commentRepository;
	private final PostReactionRepository postReactionRepository;
	private final FileService fileService;

	// 공지게시판 BoardID = '1'로 고정
	private static final Long NOTICE_BOARD_ID = 1L;

    // 키: "postId:userId" 또는 "postId:ip"
    private final Cache<String, Long> viewCountCache = Caffeine.newBuilder()
    														   .expireAfterWrite(10, TimeUnit.MINUTES) // 10분 후 자동만료
    														   .maximumSize(10_000)	// 최대 1만개 저장
    														   .build();

    // 중복 방지 시간 간격 (밀리초), 예: 10분 = 600000ms
    private final long VIEW_COUNT_EXPIRATION = 10 * 60 * 1000;

	private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

	public String extractLocalPathFromUrl(String imageUrl) {

		// '이미지 URL'이 'null' 이거나 "/images"로 시작하지 않으면 예외 처리 
		if(imageUrl == null || !imageUrl.startsWith("/images")) {
			throw new IllegalArgumentException("잘못된 이미지 URL입니다.");
		}

		// 'DB'기준으로 저장된 imageUrl = "/images/abc123.png"
		String basePath = "C:/upload/image";

		// C:/upload/image/abc123.png
		return basePath + imageUrl.replace("/images", "");
	}

	//*************************************************** Service START ***************************************************//	

	// 게시글 생성 Service
	@Override
	@Transactional
	public PostResponseDTO createPost(PostCreateRequestDTO postCreateRequestDTO, Long authorId) {
	
		logger.info("PostServiceImplment createPost() Start");

		// Request
		Long dtoBoardId = postCreateRequestDTO.getBoardId();
		String dtoTitle = postCreateRequestDTO.getTitle();
		String dtoContent= postCreateRequestDTO.getContent();
		Long dtoAuthorId = authorId;
		boolean dtoIsNotice = postCreateRequestDTO.isNotice();
		PostStatus postStatus = PostStatus.ACTIVE;
		LocalDateTime createdAt = LocalDateTime.now();
		LocalDateTime updatedAt = LocalDateTime.now();
		int commentCounter = 0; //새로운 글이므로 '0'
		List<MultipartFile> images = postCreateRequestDTO.getImages();

		Board board = boardRepository.findById(dtoBoardId)
				                     .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		// '게시글 생성 Setting'
		Post post = new Post();
		post.setBoard(board);
		post.setTitle(dtoTitle);
		post.setContent(dtoContent);
		post.setAuthorId(dtoAuthorId);
		post.setNotice(dtoIsNotice);
		post.setStatus(postStatus);
		post.setCreatedAt(createdAt);
		post.setUpdatedAt(updatedAt);

		// 'post' 객체를 DB에 저장하고, 저장된 Post 엔티티를 반환
		Post response = postRepository.save(post);

		Long postId = response.getPostId();

		// 이미지 업로드
		if(images !=null && !images.isEmpty()) {
			try {
				List<String> failedFiles = this.savePostImages(postId, images);
				// 업로드 실패 파일 문구가 있다면
				if(!failedFiles.isEmpty()) {
					//로그 찍기
					logger.warn("업로드 실패한 이미지 파일들: {}" ,failedFiles);
				}
			} catch (IllegalArgumentException e) {
				logger.error("PostServiceImpl createPost()  IllegalArgumentException Error : {}", e.getMessage() ,e);
			} catch (NoSuchElementException e) {
				logger.error("PostServiceImpl createPost()  NoSuchElementException Error : {}", e.getMessage() ,e);
			} catch (Exception e) {
				logger.error("PostServiceImpl createPost()  Exception Error : {}", e.getMessage() ,e);
			}
		}
	
		logger.info("PostServiceImplment createPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(response, commentCounter);
	}

	// 이미지 생성 Service
	@Override
	@Transactional
	public List<String> savePostImages(Long postId, List<MultipartFile> files) {

		logger.info("PostServiceImplment savePostImages() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostServiceImplment savePostImages() IllegalArgumentException Error");
			throw new IllegalArgumentException("입력값이 유효하지 않습니다.");
		}

		Post post = postRepository.findById(postId)
		                          .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		// 이미지 정렬을 위한 변수
		int order = 0;
		// 이미지 업로드 실패
		List<String> failedFiles = new ArrayList<>();
		

		for(MultipartFile file : files) {

			if(!ImageFile.isValidFile(file)) {
				failedFiles.add(file.getOriginalFilename());
				continue;
			}

			String url = null;

			try {
				// 파일 업로드 후 'url' 반환
				url = fileService.uploadToLocal(file);
				if(url == null) {
					failedFiles.add(file.getOriginalFilename());
					continue;
				}
			} catch (RuntimeException e) {
				logger.error("PostServiceImplment savePostImages() RuntimeException Error : {}", e.getMessage() , e);
				failedFiles.add(file.getOriginalFilename());
			} catch (Exception e) {
				logger.error("PostServiceImplment savePostImages() RuntimeException Error : {}", e.getMessage() , e);
				failedFiles.add(file.getOriginalFilename());
				continue;
			}

			PostImage image = new PostImage();
			image.setPost(post);
			image.setImageUrl(url);
			image.setOrderNum(order++);
			image.setCreatedAt(LocalDateTime.now());

			postImageRepository.save(image);
		}

		logger.info("PostServiceImplment savePostImages() Success End");
		// 실패 파일 문구 반환
		return failedFiles;
	}

	// 게시판 수정 Service
	@Override
	@Transactional
	public PostResponseDTO updatePost(Long postId, PostUpdateRequestDTO postUpdateRequestDTO, Long authorId) {

		logger.info("PostServiceImplment updatePost() Start");

		// Request
		String dtoTitle = postUpdateRequestDTO.getTitle();
		String dtoContent = postUpdateRequestDTO.getContent();
		boolean dtoIsNotice = postUpdateRequestDTO.isNotice();
		LocalDateTime updatedAt = LocalDateTime.now();
		List<MultipartFile> images = postUpdateRequestDTO.getImages();
		
		
		Post post= postRepository.findById(postId)
		                         .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		// 'DB 게시글 작성자ID'
		Long postAuthorId =post.getAuthorId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("DB의 작성자ID와 Request 작성자ID가 일치하지 않습니다..");
			throw new SecurityException("작성자만 수정할 수 있습니다.");
		}

		// '게시글 수정 Setting'
		post.setTitle(dtoTitle);
		post.setContent(dtoContent);
		post.setNotice(dtoIsNotice);
		post.setUpdatedAt(updatedAt);

		// 기존 이미지 삭제
		deletePostImages(postId);

		// 새로운 이미지 저장
		// 이미지 업로드
		if(images !=null && !images.isEmpty()) {
			try {
				List<String> failedFiles = this.savePostImages(postId, images);
				// 업로드 실패 파일 문구가 있다면
				if(!failedFiles.isEmpty()) {
					//로그 찍기
					logger.warn("업로드 실패한 이미지 파일들: {}" ,failedFiles);
				}
			} catch (IllegalArgumentException e) {
				logger.error("PostServiceImpl createPost()  IllegalArgumentException Error : {}", e.getMessage() ,e);
			} catch (NoSuchElementException e) {
				logger.error("PostServiceImpl createPost()  NoSuchElementException Error : {}", e.getMessage() ,e);
			} catch (Exception e) {
				logger.error("PostServiceImpl createPost()  Exception Error : {}", e.getMessage() ,e);
			}
		}
		

		int commentCount = commentRepository.countByPostPostId(postId);

		logger.info("PostServiceImplment updatePost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, commentCount);
	}

	// 게시글 삭제 Service
	@Override
	@Transactional
	public void deletePost(Long postId, Long authorId) {

		logger.info("PostServiceImplment deletePost() Start");

		// Request
		Post post = postRepository.findById(postId)
								  .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		// 'DB 게시글 작성자ID'
		Long postAuthorId = post.getAuthorId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("DB의 작성자ID와 Request 작성자ID가 다릅니다.");
			throw new SecurityException("작성자만 삭제할 수 있습니다.");
		}

		LocalDateTime updatedAt = LocalDateTime.now();

		// 논리적 삭제(O), 물리적 삭제(X) 즉, DB에는 남아있음
		post.setStatus(PostStatus.DELETED);
		post.setUpdatedAt(updatedAt);

		logger.info("PostServiceImplment deletePost() Success End");
	}

	// 실제 파일 삭제 로직
	@Override
	public void deletePostImages(Long postId) {

		logger.info("PostServiceImplment deletePostImges() Start");

		List<PostImage> images= postImageRepository.findByPostPostId(postId);

		for(PostImage image : images) {

			String imagePath = this.extractLocalPathFromUrl(image.getImageUrl());
			File file = new File(imagePath);

			if(file.exists() && file.canRead()) {
			    boolean deleted = file.delete();
			    if (!deleted) {
			        logger.warn("이미지 파일 삭제 실패: {}", file.getAbsolutePath());
			    }
			}

			postImageRepository.delete(image);
		}

		logger.info("PostServiceImplment deletePostImges() Success End");
	}

	// 게시글 단건 조회 Service
	@Override
	public PostResponseDTO getPost(Long postId) {

		logger.info("PostServiceImplment getPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		// 댓글 총 갯수 카운터
		int commentCount = commentRepository.countByPostPostId(postId);

		logger.info("PostServiceImplment getPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, commentCount);
	}

	// 특정 게시판(자유게시판,정보게시판 등) 게시글 목록 조회 (ACTIVE + 공지글 제외)
	@Override
	public Page<PostListResponseDTO> getPostsByBoard(Long boardId, Pageable pageable) {

		logger.info("PostServiceImplment getPostsByBoard() Start");

		Board board = boardRepository.findById(boardId)
				                     .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		// Page<T> : 내부에 List<T>를 가지고 있는 자료구조
		Page<Post> posts = postRepository.findByBoardAndStatusAndIsNoticeFalse(board, PostStatus.ACTIVE, pageable);

		logger.info("PostServiceImplment getPostsByBoard() Success End");

			   //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
			   //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return posts.map(post -> {
			// 'post'는 'Post'객체를 가르키는 참조 변수이며, 'Page'에 담긴 하나하나의 'Post'객체를 의미한다.
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE상태) Service
	@Override
	public Page<PostListResponseDTO> searchPostsByKeyword(String keyword, Pageable pageable) {

		logger.info("PostServiceImplment searchPostsByKeyword() Start");

		// 키워드 유효성 검사
		if (!PostValidation.isValidString(keyword)) {
		    return Page.empty(pageable); // 빈 결과 반환
		}

		// 'ACTIVE' 상태의 '게시글'의 '제목 또는 본문내용'을 '대소문자' 상관 없이 '검색' 
		Page<Post> posts = postRepository.findByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(PostStatus.ACTIVE, 
				                                                                                                         keyword, 
				                                                                                                         PostStatus.ACTIVE, 
				                                                                                                         keyword, pageable);

		logger.info("PostServiceImplment searchPostsByKeyword() Start");

		       //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
		       //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return posts.map(post -> {
			// 'post'는 'Post'객체를 가르키는 참조 변수이며, 'Page'에 담긴 하나하나의 'Post'객체를 의미한다.
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 최신순/인기순 정렬 (좋아요 + 댓글 수를 기준으로 내림차순으로 정렬한 후 생성일자로 다시한번 내림차순)	Service
	@Override
	public Page<PostListResponseDTO> getPostsSorted(String sortBy, Pageable pageable) {

		logger.info("PostServiceImplment getPostsSorted() Start");

		Page<Post> posts = null;

		if("popular".equalsIgnoreCase(sortBy)) {
			posts = postRepository.findPopularPosts(pageable);
		}else {
			posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.ACTIVE,pageable);
		}

		logger.info("PostServiceImplment getPostsSorted() Success End");

		return posts.map(post -> {
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 작성자별 게시글 조회 Service
	@Override
	public Page<PostListResponseDTO> getPostsByAuthorNickname(String nickname, Pageable pageable) {

		logger.info("PostServiceImplment getPostsByAuthorNickname() Start");

		Member member = memberRepository.findByNickname(nickname)
									    .orElseThrow(() -> new NoSuchElementException("작성자를 찾을 수 없습니다."));

		// Request
		Long memberId = member.getId();

		Page<Post> posts = postRepository.findByAuthorIdAndStatus(memberId,PostStatus.ACTIVE,pageable);


		logger.info("PostServiceImplment getPostsByAuthorNickname() Success End");

		return posts.map(post -> {
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 조회수 증가 중복 방지 Service
	@Override
	@Transactional
	public void increaseViewCount(Long postId, String userIdentifier) {
	    logger.info("PostServiceImplment increaseViewCount() Start");

	    String key = postId + ":" + userIdentifier;
	    long now = System.currentTimeMillis();

	    viewCountCache.asMap().compute(key, (k, lastAccess) -> {
	        if (lastAccess == null || now - lastAccess > VIEW_COUNT_EXPIRATION) {
	            int updatedRows = postRepository.incrementViewCount(postId);
	            if (updatedRows == 0) {
	                throw new NoSuchElementException("게시글이 존재하지 않습니다.");
	            }
	            return now; // 캐시 최신화
	        }
	        return lastAccess; // 유효기간 안 됐으면 기존 시간 유지
	    });

	    logger.info("PostServiceImplment increaseViewCount() Success End");
	}

	// 핀 설정/해제 Service
	@Override
	@Transactional
	public void togglePinPost(Long postId, boolean pin) {

		logger.info("PostServiceImplment togglePinPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		post.setPinned(pin);

		logger.info("PostServiceImplment togglePinPost() Success End");
	}

	// 공지 게시판 공지글 조회 Service
	@Override
	public Page<PostListResponseDTO> getAllNotices(Pageable pageable) {

		logger.info("PostServiceImplment getAllNotices() Start");

		Board board = boardRepository.findById(NOTICE_BOARD_ID)
		                             .orElseThrow(() -> new NoSuchElementException("공지 게시판이 존재하지 않습니다."));

		Page<Post> posts = postRepository.findByBoardAndIsNoticeTrueAndStatus(board, PostStatus.ACTIVE, pageable);

		logger.info("PostServiceImplment getAllNotices() Success End");

		return posts.map(post -> {
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기 Service
	@Override
	public List<PostListResponseDTO> getTop3PinnedNoticesByBoard() {

		logger.info("PostServiceImplment getTop3PinnedNoticesByBoard() Start");

		Board board = boardRepository.findById(NOTICE_BOARD_ID)
		                              .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		List<Post> posts = postRepository.findTop3ByBoardAndIsPinnedTrueAndIsNoticeTrueAndStatusOrderByCreatedAtDesc(board, PostStatus.ACTIVE);

		logger.info("PostServiceImplment getTop3PinnedNoticesByBoard() Success End");
		return posts.stream()
				    .map(post -> PostListResponseDTO.fromEntity(post, postReactionRepository.countByPostPostId(post.getPostId())))
				    .collect(Collectors.toList());
	}

	// 이미지 목록 조회 Service
	@Override
	public List<PostImageResponseDTO> getPostImages(Long postId) {

		// 게시글 존재 여부 확인
		Post post = postRepository.findById(postId)
		                          .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

		 List<PostImage> images = postImageRepository.findByPostPostId(postId);

		return images.stream()
				     .map(image -> new PostImageResponseDTO(image.getImageId(), 
				    		 								image.getImageUrl(), 
				    		 								image.getOrderNum()))
				     .collect(Collectors.toList());
	}

	// 이미지 정렬 순서 조정 Service
	@Override
	@Transactional
	public void updateImageOrder(Long postId, List<ImageOrderDTO> orderList, Long requestAuthorId) {

	    logger.info("PostServiceImplment updateImageOrder() Start");

	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

	    if (!post.getAuthorId().equals(requestAuthorId)) {
	        throw new SecurityException("작성자만 이미지 순서를 변경할 수 있습니다.");
	    }

	    Map<Long, Integer> orderMap = orderList.stream()
	    		                               .collect(Collectors.toMap(ImageOrderDTO :: getImageId , 
	    		                            		   					 ImageOrderDTO :: getOrderNum));

	    List<PostImage> images = postImageRepository.findByPostPostId(postId);

	    for (PostImage image  : images) {
	    	if(orderMap.containsKey(image.getImageId())) {
	    		image.setOrderNum(orderMap.get(image.getImageId()));
	    	}
	    }

	    logger.info("PostServiceImplment updateImageOrder() Success End");
	}

	// 이미지 단건 삭제 Service
	@Override
	@Transactional
	public void deleteSingleImage(Long postId, Long imageId, Long requestAuthorId) {

	    logger.info("PostServiceImplment deleteSingleImage() Start");

	    if (!PostValidation.isPostId(postId)) {
	        logger.error("PostServiceImplment deleteSingleImage() IllegalArgumentException Error");
	        throw new IllegalArgumentException("입력값이 유효하지 않습니다.");
	    }

	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

	    if (!post.getAuthorId().equals(requestAuthorId)) {
	        throw new SecurityException("작성자만 이미지를 삭제할 수 있습니다.");
	    }

	    PostImage image = postImageRepository.findById(imageId)
	                                         .orElseThrow(() -> new NoSuchElementException("이미지를 찾을 수 없습니다"));

	    String imageUrl = image.getImageUrl();

	    // 실제 파일 삭제
	    String path = this.extractLocalPathFromUrl(imageUrl);
	    File file = new File(path);

	    if (file.exists() && file.canRead()) {
	        boolean deleted = file.delete();
	        if(!deleted) {
	        	logger.warn("이미지 파일 삭제 실패: {}", file.getAbsolutePath());
	        }
	    }

	    // DB 삭제도 필요하다면 아래 추가
	    postImageRepository.delete(image);

	    logger.info("PostServiceImplment deleteSingleImage() Success End");
	}

	@Override
	public Page<PostListResponseDTO> getPostsByParentBoard(Long parentBoardId, Pageable pageable) {

	    logger.info("PostServiceImplment getPostsByParentBoard() Start");

	    // 부모 게시판 존재 확인
	    Board parentBoard = boardRepository.findById(parentBoardId)
	        .orElseThrow(() -> new NoSuchElementException("부모 게시판이 존재하지 않습니다."));

	    // 자식 게시판 ID들 조회
	    List<Board> childBoards = boardRepository.findByParentId(parentBoardId);
	    List<Long> boardIds = childBoards.stream()
	                                     .map(Board::getBoardId)
	                                     .collect(Collectors.toList());

	    // 부모 자신도 포함
	    boardIds.add(parentBoardId);

	    // 해당 게시판들의 게시글 가져오기
	    Page<Post> posts = postRepository.findActiveNonNoticePostsByBoardIds(boardIds, pageable);

	    logger.info("PostServiceImplment getPostsByParentBoard() Success End");

	    return posts.map(post -> {
	        int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
	        return PostListResponseDTO.fromEntity(post, reactionCount);
	    });
	}

	//*************************************************** Service END ***************************************************

}
