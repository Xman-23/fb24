package com.example.demo.service.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.postImage.PostImage;
import com.example.demo.dto.post.PostCreateRequestDTO;
import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.postimage.ImageOrderDTO;
import com.example.demo.dto.postimage.PostImageResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
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
	public static final Long NOTICE_BOARD_ID = 1L;

	// 파일 업로드 경로 ('application.properties'에서 경로처리)
	@Value("${file.upload.base-path}")
	private String basePath;

	// 인기 게시글 좋아요 기준
	@Value("${post.popular.likeThreshold}")
	private int popularLikeThreshold;;
	
	// 인기 게시글 날짜 기준
	@Value("${post.popular.dayLimit}")
	private int popularDayLimit;

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
			logger.error("PostServiceImpl extractLocalPathFromUrl() IllegalArgumentException Error");
			throw new IllegalArgumentException("잘못된 이미지 URL입니다.");
		}

		// 'DB'기준으로 저장된 imageUrl = "/images/abc123.png"
		// String basePath = "C:/upload/image";

		// imageUrl = "/images/...." (/.... : 파일명).png
		// -> replaceFirst("/images", "") -> /....(파일명).png
		String fileName = imageUrl.replaceFirst("/images", "");

		// '/'을 제거한 파일명만 있어야함 ex) /abc.png -> abc.png
		if(fileName.startsWith("/")) {
			fileName = fileName.substring(1);
		}

		// 운영체제에 맞게 조합 ('Paths.get' 알아서 경로 맞춰줌)
		Path fullPath = Paths.get(basePath, fileName);

		return fullPath.toString(); //OS에 맞게 경로 문자열 반환
	}

	private void deleteImageFile(String imageUrl) {
	    String imagePath = this.extractLocalPathFromUrl(imageUrl);
	    File file = new File(imagePath);
	    if (file.exists() && file.canRead()) {
	        boolean deleted = file.delete();
	        if (!deleted && file.exists()) {
	            logger.warn("이미지 파일 삭제 실패: {}", file.getAbsolutePath());
	        }
	    }
	}

	//*************************************************** Service START ***************************************************//	

	// 게시글 생성 Service
	@Override
	@Transactional
	public PostResponseDTO createPost(PostCreateRequestDTO postCreateRequestDTO, Long authorId, String userNickname) {
	
		logger.info("PostServiceImpl createPost() Start");

		// Request
		Long dtoBoardId = postCreateRequestDTO.getBoardId();
		String dtoTitle = UriUtils.decode(postCreateRequestDTO.getTitle(), StandardCharsets.UTF_8);
		String dtoContent= UriUtils.decode(postCreateRequestDTO.getContent(), StandardCharsets.UTF_8);
		Long dtoAuthorId = authorId;
		boolean dtoIsNotice = postCreateRequestDTO.isNotice();
		PostStatus postStatus = PostStatus.ACTIVE;
		LocalDateTime createdAt = LocalDateTime.now();
		LocalDateTime updatedAt = LocalDateTime.now();
		int commentCounter = 0; //새로운 글이므로 '0'
		List<MultipartFile> images = postCreateRequestDTO.getImages();

		logger.info("PostServiceImpl updatePost() dtoIsNotice : {}",dtoIsNotice);

		Board board = boardRepository.findById(dtoBoardId)
				                     .orElseThrow(() -> {
				                    	 logger.error("PostServiceImpl createPost() NoSuchElementException Error : {}");
				                    	 return new NoSuchElementException("게시판이 존재하지 않습니다.");
				                     });

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


		/**
		 * 'List<Image>'를 'Stream<Image>' 변환 시 NullPointerException 방지를 위한 유효성 체크입니다.
		 * '이미지 필드'를 요청에서 누락하면, DTO의 이미지 필드는 null이 됩니다.
		 * '이미지 필드'는 있지만 내용이 없으면 빈 리스트([])가 전달됩니다.
		 * 빈 리스트([])는 null이 아니며, isEmpty() 호출 시 true를 반환합니다.
		 * 다만, 빈 리스트가 아니더라도 내부 요소가 null이거나 비어있는 파일일 수 있으므로,
		 * 실제 유효한 파일인지 추가 필터링(filter) 작업이 필요
		 */
		if(images == null || images.isEmpty()) {
			logger.info("PostServiceImpl createPost() : 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, commentCounter, userNickname);
		}


		// 실제 유효한 파일인지 추가 필터링(filter) 작업
		// 이미지 파일이 'null'이 아니거나, '빈 리스트([])' 필터링
		List<MultipartFile> validFiles = images.stream()
				                               .filter(file -> file != null && !file.isEmpty())
				                               .toList();

		// 유효한 파일이 하나도 없으면 업로드 스킵
		if (validFiles.isEmpty()) {
		    logger.info("PostServiceImpl createPost() : 유효한 이미지 파일 없음, 업로드 스킵");
		    return PostResponseDTO.convertToPostResponseDTO(response, commentCounter, userNickname);
		}

		// 이미지 업로드
		try {
			List<String> failedFiles = this.savePostImages(postId, validFiles);
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

		logger.info("PostServiceImpl createPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(response, commentCounter, userNickname);
	}

	// 이미지 생성 Service
	@Override
	@Transactional
	public List<String> savePostImages(Long postId, List<MultipartFile> files) {

		logger.info("PostServiceImpl savePostImages() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostServiceImpl savePostImages() IllegalArgumentException Error");
			throw new IllegalArgumentException("입력값이 유효하지 않습니다.");
		}


		Post post = postRepository.findById(postId)
		                          .orElseThrow(() -> {
		                        	 logger.error("PostServiceImpl savePostImages() NoSuchElementException Error : {}");
		                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
		                          });

		// 이미지 업로드 실패
		List<String> failedFiles = new ArrayList<>();
		// 이미지 정렬을 위한 변수
		int order = post.getImages().size();

	    List<String> uploadedFilePaths = new ArrayList<>();

		/** 
		 *	DB 원자성(한번에 트랜잭션이 되는지 안되는지),
		 *	DB 일관성(DB와 서버 파일이 매칭 안될시에) 'RollBack' 
		*/  
		// 1) 모든 파일 업로드 먼저 진행
		try {

			for(MultipartFile file : files){
				
				if(!ImageFile.isValidFile(file)) {
					logger.error("PostServiceImpl savePostImages() IllegalArgumentException : 유효하지 않은 파일이 포함되어 있습니다.");
					failedFiles.add(file.getOriginalFilename());
					throw new IllegalArgumentException("유효하지 않은 파일이 포함되어 있습니다.");
				}
				
				String url = fileService.uploadToLocal(file);

				if(url == null || url.equals("")) {
					throw new RuntimeException("파일 업로드 실패");
				}

				uploadedFilePaths.add(url);

				// 2) DB에 이미지 정보 저장
				PostImage image = new PostImage();
				image.setPost(post);
				image.setImageUrl(url);
				image.setOrderNum(order++);
				image.setCreatedAt(LocalDateTime.now());

				// JPA 양방향 관계 연결
				post.addImage(image);

			}
		}catch (RuntimeException e) {
			logger.error("PostServiceImpl savePostImages() RuntimeException Error : {}", e.getMessage() , e);
	        for (String url : uploadedFilePaths) {
	            this.deleteImageFile(url);
	        }
			throw e; // 예외 다시 던져서 롤백 유도
		} catch (Exception e) {
			logger.error("PostServiceImpl savePostImages() Exception Error : {}", e.getMessage() , e);
	        for (String url : uploadedFilePaths) {
	            this.deleteImageFile(url);
	        }
			throw e; // 예외 다시 던져서 롤백 유도
		}
 
		logger.info("PostServiceImpl savePostImages() Success End");
		// 실패 파일 문구 반환
		return failedFiles;
	}

	// 게시판 수정 Service
	@Override
	@Transactional
	public PostResponseDTO updatePost(Long postId, PostUpdateRequestDTO postUpdateRequestDTO, Long authorId, String userNickname) {

		logger.info("PostServiceImpl updatePost() Start");

		// Request
		String dtoTitle = UriUtils.decode(postUpdateRequestDTO.getTitle(), StandardCharsets.UTF_8);
		String dtoContent = UriUtils.decode(postUpdateRequestDTO.getContent(), StandardCharsets.UTF_8);
		boolean dtoIsNotice = postUpdateRequestDTO.isNotice();
		LocalDateTime updatedAt = LocalDateTime.now();
		List<MultipartFile> images = postUpdateRequestDTO.getImages();
		
		// 'JPA'가 '게시글 조회(findBy)'를 시작으로 '게시글 엔티티' 영속성을 유지
		Post post= postRepository.findById(postId)
		                         .orElseThrow(() -> {
		                        	 logger.error("PostServiceImpl updatePost() NoSuchElementException Error : {}");
		                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
		                         });
		logger.info("PostServiceImpl updatePost() dtoIsNotice : {}",dtoIsNotice);
		Long boardId = post.getBoard().getBoardId();
		Member member = memberRepository.findById(authorId)
		                                .orElseThrow(() -> {
				                        	 logger.error("PostServiceImpl getPost() NoSuchElementException Error : {}");
				                        	 return new NoSuchElementException("회원이 존재하지 않습니다."); 
		                                });

		Role role =member.getRole();

		// 만약 수정하려는 게시판ID가 공지 게시판ID라면
		if (boardId.equals(NOTICE_BOARD_ID)) {
		    if (!role.equals(Role.ROLE_ADMIN)) {
		        logger.error("PostController PostServiceImpl updatePost() : 공지게시글은 관리자만 작성 가능합니다.");
		        throw new SecurityException("공지게시판은 관리자만 수정할 수 있습니다.");
		    }
		}


		// 'DB 게시글 작성자ID'
		Long postAuthorId =post.getAuthorId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("PostServiceImpl updatePost() SecurityException Error : {} DB의 작성자ID와 Request 작성자ID가 일치하지 않습니다..");
			throw new SecurityException("작성자만 수정할 수 있습니다.");
		}

		// 영속 상태인 '게시글 DB 업데이트 Start'
		post.setTitle(dtoTitle);
		post.setContent(dtoContent);
		post.setNotice(dtoIsNotice);
		post.setUpdatedAt(updatedAt);
		// 영속 상태인 '게시글 DB 업데이트 End'

		// 댓글 갯수
		int commentCount = commentRepository.countByPostPostId(postId);

		// 'Image'를 'Stream'시 'NullPointException'방지를 위한 유효성 체크
		if(images == null || images.isEmpty()) {
			logger.info("PostServiceImpl updatePost() : 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, commentCount, userNickname);
		}

		// 이미지 파일 유효성 체크
		List<MultipartFile> validFiles = images.stream()
				                               .filter(file -> file != null && !file.isEmpty())
				                               .toList();

		// 유효한 파일이 하나도 없으면 업로드 스킵
		if (validFiles.isEmpty()) {
		    logger.info("PostServiceImpl updatePost() : 유효한 이미지 파일 없음, 업로드 스킵");
		    return PostResponseDTO.convertToPostResponseDTO(post, commentCount, userNickname);
		}

		// 새 이미지 업로드 시도
		List<String> failedFiles = new ArrayList<>();

		// 새로운 이미지 저장
		// 이미지 업로드
		try {
			failedFiles = this.savePostImages(postId, validFiles);
			// 업로드 실패 파일 문구가 있다면
			if(!failedFiles.isEmpty()) {
				//로그 찍기
				logger.warn("업로드 실패한 이미지 파일들: {}" ,failedFiles);
			}
		} catch (IllegalArgumentException e) {
			logger.error("PostServiceImpl updatePost()  IllegalArgumentException Error : {}", e.getMessage() ,e);
		} catch (NoSuchElementException e) {
			logger.error("PostServiceImpl updatePost()  NoSuchElementException Error : {}", e.getMessage() ,e);
		} catch (Exception e) {
			logger.error("PostServiceImpl updatePost()  Exception Error : {}", e.getMessage() ,e);
			throw e; //롤백 유도
		}

		logger.info("PostServiceImpl updatePost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, commentCount, userNickname);
	}

	// 게시글 삭제 Service
	@Override
	@Transactional
	public void deletePost(Long postId, Long authorId, boolean isDeleteImages) {

		logger.info("PostServiceImpl deletePost() Start");

		// Request
		Post post = postRepository.findById(postId)
								  .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl deletePost() NoSuchElementException Error : {}");
			                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
								  });


		Long boardId = post.getBoard().getBoardId();
		Member member = memberRepository.findById(authorId)
		                                .orElseThrow(() -> {
				                        	 logger.error("PostServiceImpl deletePost() NoSuchElementException Error : {}");
				                        	 return new NoSuchElementException("회원이 존재하지 않습니다."); 
		                                });

		Role role =member.getRole();

		// 만약 수정하려는 게시판ID가 공지 게시판ID라면
		if (boardId.equals(NOTICE_BOARD_ID)) {
		    if (!role.equals(Role.ROLE_ADMIN)) {
		        logger.error("PostController PostServiceImpl deletePost() : 공지게시판은 관리자만 삭제할 수 있습니다.");
		        throw new SecurityException("공지게시판은 관리자만 삭제할 수 있습니다.");
		    }
		}

		// 'DB 게시글 작성자ID'
		Long postAuthorId = post.getAuthorId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("PostServiceImpl deletePost() SecurityException Error : {} DB의 작성자ID와 Request 작성자ID가 일치하지 않습니다..");
			throw new SecurityException("작성자만 수정할 수 있습니다.");
		}

		LocalDateTime updatedAt = LocalDateTime.now();

		// 논리적 삭제(O), 물리적 삭제(X) 즉, DB에는 남아있음
		post.setStatus(PostStatus.DELETED);
		post.setUpdatedAt(updatedAt);

		if(isDeleteImages) {
			this.deletePostImages(postId);
		}

		logger.info("PostServiceImpl deletePost() Success End");
	}

	// 실제 파일 삭제
	@Override
	public void deletePostImages(Long postId) {

		logger.info("PostServiceImpl deletePostImges() Start");

		List<PostImage> images= postImageRepository.findByPostPostId(postId);

		for(PostImage image : images) {

			String imagePath = this.extractLocalPathFromUrl(image.getImageUrl());
			File file = new File(imagePath);

			if(file.exists() && file.canRead()) {
			    boolean deleted = file.delete();
			    if (!deleted && file.exists()) { // 한번 더 확인해서 파일이 실제 삭제  되었을때
			        logger.warn("이미지 파일 삭제 실패: {}", file.getAbsolutePath());
			    }
			}

			postImageRepository.delete(image);
		}

		logger.info("PostServiceImpl deletePostImges() Success End");
	}

	// 이미지 단건 삭제 Service
	@Override
	@Transactional
	public void deleteSingleImage(Long postId, Long imageId, Long requestAuthorId) {

	    logger.info("PostServiceImpl deleteSingleImage() Start");

	    if (!PostValidation.isPostId(postId)) {
	        logger.error("PostServiceImpl deleteSingleImage() IllegalArgumentException Error");
	        throw new IllegalArgumentException("입력값이 유효하지 않습니다.");
	    }

	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> {
	                            	 logger.error("PostServiceImpl deleteSingleImage() NoSuchElementException Error : {}");
	                            	 return new NoSuchElementException("게시글을 찾을 수 없습니다.");
	                              });

	    if (!post.getAuthorId().equals(requestAuthorId)) {
	        throw new SecurityException("작성자만 이미지를 삭제할 수 있습니다.");
	    }

	    PostImage image = postImageRepository.findById(imageId)
	                                         .orElseThrow(() -> {
	                                        	 logger.error("PostServiceImpl deleteSingleImage() NoSuchElementException Error : {}");
	                                        	 return new NoSuchElementException("이미지를 찾을 수 없습니다");
	                                         });

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

	    // 가장 중요 변경 코드 (orphanRemoval 트리거를 위해 컬렉션에서 제거)
	    post.getImages().remove(image);

	    logger.info("PostServiceImpl deleteSingleImage() Success End");
	}

	// 이미지 모두 삭제 Service
	@Override
	@Transactional
	public void deleteAllImages(Long postId, Long requestAuthorId) {

	    logger.info("PostServiceImpl deleteAllImages() Start");

	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

	    if (!post.getAuthorId().equals(requestAuthorId)) {
	        throw new SecurityException("작성자만 이미지를 삭제할 수 있습니다.");
	    }

	    List<PostImage> images = new ArrayList<>(post.getImages()); // 복사

	    for (PostImage image : images) {
	        this.deleteImageFile(image.getImageUrl()); // 실제 파일 삭제
	    }

	    post.getImages().removeAll(images); // 영속성 제거 → orphanRemoval 트리거

	    logger.info("PostServiceImpl deleteAllImages() Success End");

	}

	// 게시글 단건 조회 Service
	@Override
	public PostResponseDTO getPost(Long postId) {

		logger.info("PostServiceImpl getPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl getPost() NoSuchElementException Error : {}");
			                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
				                  });

		Member member = memberRepository.findById(post.getAuthorId())
		                                .orElseThrow(() -> {
				                        	 logger.error("PostServiceImpl getPost() NoSuchElementException Error : {}");
				                        	 return new NoSuchElementException("회원이 존재하지 않습니다."); 
		                                });

		String userNickname = member.getNickname();

		// 댓글 총 갯수 카운터
		int commentCount = commentRepository.countByPostPostId(postId);

		logger.info("PostServiceImpl getPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, commentCount, userNickname);
	}

	// 자식게시판 게시글 목록 조회 (ACTIVE + 공지글 제외)
	@Override
	public Page<PostListResponseDTO> getPostsByBoard(Long boardId, Pageable pageable) {

		logger.info("PostServiceImpl getPostsByBoard() Start");

		Board board = boardRepository.findById(boardId)
				                     .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl getPostsByBoard() NoSuchElementException Error : {}");
			                        	 return new NoSuchElementException("게시판이 존재하지 않습니다."); 
				                     });

		// Page<T> : 내부에 List<T>를 가지고 있는 자료구조
		Page<Post> posts = postRepository.findByBoardAndStatusAndIsNoticeFalse(board, PostStatus.ACTIVE, pageable);

		logger.info("PostServiceImpl getPostsByBoard() Success End");

			   //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
			   //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return posts.map(post -> {
			// 'post'는 'Post'객체를 가르키는 참조 변수이며, 'Page'에 담긴 하나하나의 'Post'객체를 의미한다.
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			Member member = memberRepository.findById(post.getAuthorId())
                                            .orElseThrow(() -> {
                                            	logger.error("PostServiceImpl getPost() NoSuchElementException Error : {}");
                                            	return new NoSuchElementException("회원이 존재하지 않습니다."); 
                                            });
			String userNickname = member.getNickname();
			return PostListResponseDTO.fromEntity(post, reactionCount, userNickname);
		});
	}

	// 자식게시판 정렬(최신순, 좋아요 수)
	@Override
	public Page<PostListResponseDTO> getPostsByBoardSorted(Long boardId, String sortBy, Pageable pageable) {

	    logger.info("PostServiceImpl getPostsByBoardSorted() Start");

	    Board board = boardRepository.findById(boardId)
	                                 .orElseThrow(() -> {
	                                	 logger.error("PostServiceImpl getPostsByBoardSorted() NoSuchElementException Error : {}");
	                                	 return new NoSuchElementException("게시판이 존재하지 않습니다.");
	                                 });

	    Page<Post> posts = null;

	    if ("popular".equalsIgnoreCase(sortBy)) {
	        // 인기순 정렬: 좋아요 + 댓글 수 내림차순, 그 다음 최신순 정렬 (PostRepository에 쿼리 추가 필요)
	        posts = postRepository.findPopularPostsByBoard(board, pageable);
	    } else {
	        // 최신순 정렬: 활성 + 공지글 제외, 최신순 (기존 메서드 활용)
	        posts = postRepository.findByBoardAndStatusAndIsNoticeFalseOrderByCreatedAtDesc(board, PostStatus.ACTIVE, pageable);
	    }

	    logger.info("PostServiceImpl getPostsByBoardSorted() Success End");

	    return posts.map(post -> {
	    	int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
	        Member member = memberRepository.findById(post.getAuthorId())
	                                        .orElseThrow(() -> {
	                                        	logger.error("PostServiceImpl getPostsByBoardSorted() NoSuchElementException : 회원이 존재하지 않습니다.");
	                                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
	                                        });
	        String userNickname = member.getNickname();
	        return PostListResponseDTO.fromEntity(post, reactionCount, userNickname);
	    });
	}

	// 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE상태) Service
	@Override
	public Page<PostListResponseDTO> searchPostsByKeyword(String keyword, Pageable pageable) {

		logger.info("PostServiceImpl searchPostsByKeyword() Start");

		// 키워드 유효성 검사
		if (!PostValidation.isValidString(keyword)) {
		    return Page.empty(pageable); // 빈 결과 반환
		}

		// 'ACTIVE' 상태의 '게시글'의 '제목 또는 본문내용'을 '대소문자' 상관 없이 '검색' 
		Page<Post> posts = postRepository.searchByKeyword(keyword, pageable);

		logger.info("PostServiceImpl searchPostsByKeyword() Start");

		       //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
		       //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return posts.map(post -> {
			// 'post'는 'Post'객체를 가르키는 참조 변수이며, 'Page'에 담긴 하나하나의 'Post'객체를 의미한다.
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			Member member = memberRepository.findById(post.getAuthorId())
                                            .orElseThrow(() -> {
                                            	logger.error("PostServiceImpl getPost() NoSuchElementException Error : {}");
                                            	return new NoSuchElementException("회원이 존재하지 않습니다."); 
                                            });
			String userNickname = member.getNickname();
			return PostListResponseDTO.fromEntity(post, reactionCount, userNickname);
		});
	}

	// 작성자별 게시글 조회 Service
	@Override
	public Page<PostListResponseDTO> getPostsByAuthorNickname(String nickname, Pageable pageable) {

		logger.info("PostServiceImpl getPostsByAuthorNickname() Start");

		Member member = memberRepository.findByNickname(nickname)
									    .orElseThrow(() -> {
									    	logger.error("PostServiceImpl getPostsByAuthorNickname() NoSuchElementException Error : {}");
									    	return new NoSuchElementException("작성자를 찾을 수 없습니다.");
									    });

		// Request
		Long memberId = member.getId();
		String userNickname = member.getNickname();

		Page<Post> posts = postRepository.findByAuthorIdAndStatus(memberId,PostStatus.ACTIVE,pageable);


		logger.info("PostServiceImpl getPostsByAuthorNickname() Success End");

		return posts.map(post -> {
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount, userNickname);
		});
	}

	// 조회수 증가 중복 방지 Service
	@Override
	@Transactional
	public void increaseViewCount(Long postId, String userIdentifier) {
	    logger.info("PostServiceImpl increaseViewCount() Start");

	    String key = postId + ":" + userIdentifier;
	    long now = System.currentTimeMillis();

	    viewCountCache.asMap().compute(key, (k, lastAccess) -> {
	        if (lastAccess == null || now - lastAccess > VIEW_COUNT_EXPIRATION) {
	            int updatedRows = postRepository.incrementViewCount(postId);
	            if (updatedRows == 0) {
	            	logger.error("PostServiceImpl increaseViewCount() NoSuchElementException Error : {}");
	                throw new NoSuchElementException("게시글이 존재하지 않습니다.");
	            }
	            return now; // 캐시 최신화
	        }
	        return lastAccess; // 유효기간 안 됐으면 기존 시간 유지
	    });

	    logger.info("PostServiceImpl increaseViewCount() Success End");
	}

	// 핀 설정/해제 Service
	@Override
	@Transactional
	public void togglePinPost(Long postId, boolean pin) {

		logger.info("PostServiceImpl togglePinPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
				                	  logger.error("PostServiceImpl togglePinPost() NoSuchElementException Error : {}");
				                	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
				                  });

		post.setPinned(pin);

		logger.info("PostServiceImpl togglePinPost() Success End");
	}

	// 공지 게시판 공지글 조회 Service
	@Override
	public Page<PostListResponseDTO> getAllNotices(Pageable pageable) {

		logger.info("PostServiceImpl getAllNotices() Start");

		Page<Post> posts = postRepository.findNoticePosts(NOTICE_BOARD_ID, PostStatus.ACTIVE, pageable);

		logger.info("PostServiceImpl getAllNotices() Success End");

		return posts.map(post -> {
			int reactionCount = postReactionRepository.countByPostPostId(post.getPostId());
			return PostListResponseDTO.fromEntity(post, reactionCount);
		});
	}

	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기 Service
	@Override
	public List<PostListResponseDTO> getTop3PinnedNoticesByBoard() {

		logger.info("PostServiceImpl getTop3PinnedNoticesByBoard() Start");

		Board board = boardRepository.findById(NOTICE_BOARD_ID)
		                             .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		List<Post> posts = postRepository.findTop3ByBoardAndIsPinnedTrueAndIsNoticeTrueAndStatusOrderByCreatedAtDesc(board, PostStatus.ACTIVE);

		logger.info("PostServiceImpl getTop3PinnedNoticesByBoard() Success End");
		return posts.stream()
				    .map(post -> PostListResponseDTO.fromEntity(post, postReactionRepository.countByPostPostId(post.getPostId())))
				    .collect(Collectors.toList());
	}

	// 이미지 목록 조회 Service
	@Override
	public List<PostImageResponseDTO> getPostImages(Long postId) {

		// 게시글 존재 여부 확인
		Post post = postRepository.findById(postId)
		                          .orElseThrow(() -> {
		                        	  logger.error("PostServiceImpl getPostImages() NoSuchElementException Error : {}");
		                        	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
		                          });

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
	public List<PostImageResponseDTO> updateImageOrder(Long postId, List<ImageOrderDTO> orderList, Long requestAuthorId) {

	    logger.info("PostServiceImpl updateImageOrder() Start");

	    Post post = postRepository.findById(postId)
	                              .orElseThrow(() -> {
	                            	logger.error("PostServiceImpl updateImageOrder() NoSuchElementException Error : {}");
	                            	return new NoSuchElementException("게시글을 찾을 수 없습니다.");  
	                              });

	    if (!post.getAuthorId().equals(requestAuthorId)) {
	        throw new SecurityException("작성자만 이미지 순서를 변경할 수 있습니다.");
	    }

	    // 이미지 순서 변경 Request
	    Map<Long, Integer> orderMap = orderList.stream()
	    		                               .collect(Collectors.toMap(ImageOrderDTO :: getImageId , // ImageOrderDTO의 getImageId = key
	    		                            		   					 ImageOrderDTO :: getOrderNum)); // ImageOrderDTO의 getOrderNum = value

	    // 'JPA'의해 영속성 상태
	    List<PostImage> images = postImageRepository.findByPostPostId(postId);

	    for (PostImage image  : images) {
	    	if(orderMap.containsKey(image.getImageId())) {
	    		// 이미지 순서 변경
	    		image.setOrderNum(orderMap.get(image.getImageId()));
	    	}
	    }

	    logger.info("PostServiceImpl updateImageOrder() Success End");
	    return images.stream()
	    		     .map(image -> PostImageResponseDTO.builder()
	    		    		                           .imageId(image.getImageId())
	    		    		                           .imageUrl(image.getImageUrl())
	    		    		                           .orderNum(image.getOrderNum())
	    		    		                           .build())
	    		     // 변경된 각 이미지의 'getOrderNum'를 가져와 순서 비교
	    		     .sorted(Comparator.comparingInt(PostImageResponseDTO :: getOrderNum))
	    		     .collect(Collectors.toList());
	    		                                       
	}

	// 자식게시판 모든 게시글 + 좋아요 많은 게시글 보여주기
	@Override
	public Page<PostListResponseDTO> getPostsByParentBoard(Long parentBoardId, Pageable pageable) {

	    logger.info("PostServiceImpl getPostsByParentBoard() Start");

	    // 부모 게시판 존재 확인
	    Board parentBoard = boardRepository.findById(parentBoardId)
	                                       .orElseThrow(() -> {
	                                    	 logger.error("PostServiceImpl NoSuchElementException() Error : 부모 게시판이 존재하지 않습니다.");
	                                    	 return new NoSuchElementException("부모 게시판이 존재하지 않습니다.");
	                                       });

	    // 자식 게시판 ID들 조회
	    List<Board> childBoards = boardRepository.findByParentBoard_BoardId(parentBoardId);

	    if (childBoards.isEmpty()) {
	    	// 자식 게시판이 없을경우 빈페이지 반환
	        return Page.empty(pageable);
	    }

	    // 자식 게시판 ID 'List' 만들기
	    List<Long> boardIds = childBoards.stream()
	                                     .map(Board::getBoardId)
	                                     .collect(Collectors.toList());

	    // 인기글 최근 2일 기준 시간 계산
	    LocalDateTime recentThreshold = LocalDateTime.now().minusDays(popularDayLimit);

	    // 상단 인기 게시글 먼저 가져오기 (좋아요 50개 이상 + '오늘'기준으로 최근 3일)
	    List<Post> popularPosts = postRepository.findTopPopularPostsByBoards(boardIds, 
	    																	 popularLikeThreshold, 
	    																	 recentThreshold, 
	    																	 PageRequest.of(0, 3)); // 상단 인기글 고정은 3개 제한

	    // 해당 게시판들의 게시글 가져오기
	    Page<Post> posts = postRepository.findActiveNonNoticePostsByBoardIds(boardIds, pageable);

	    // 상단 인기 게시글 DTO 리스트
	    List<PostListResponseDTO> topPopularDtos = popularPosts.stream()
                                                               .map(post -> PostListResponseDTO.fromEntity(post, 
                                                                                                           postReactionRepository.countByPostPostId(post.getPostId()), 
                                                                                                           memberRepository.findById(post.getAuthorId())
                                                                                                                           .map(member -> member.getNickname()).orElse(null)
                                                                                                          ))
                                                               .collect(Collectors.toList());

	    // 일반 게시글 DTO 리스트
	    List<PostListResponseDTO> nomalDtos = posts.stream()
	    		                                   .map(post -> PostListResponseDTO.fromEntity(post, 
	    		                                		                                       postReactionRepository.countByPostPostId(post.getPostId()), 
	    		                                		                                       memberRepository.findById(post.getAuthorId())
	    		                                		                                                       .map(member -> member.getNickname()).orElse(null)
	    		                                		                                      ))
	    		                                   .collect(Collectors.toList());
	    
	    // 게시글 합치기
	    List<PostListResponseDTO> combined = new ArrayList<>();

	    // 첫 페이지('pageNumber == 0')일때에만 상단 인기글 3개 고정
	    if (pageable.getPageNumber() == 0) {
	        combined.addAll(topPopularDtos);
	    }
	    combined.addAll(nomalDtos);

	    logger.info("PostServiceImpl getPostsByParentBoard() Success End");

	    // 인터페이스 'Page'의 구현체인 new 'PageImpl' 반환 (List<>(), pageable, 총 게시글(데이터 갯수))
	    // 'pageable'의 'size'가 10이므로, 한페이지당 10개 씩 데이터를 보여줌(상단 인기글 3개 + 일반글 7개)
	    // 총 데이터 갯수(총 일반게시글 + 상단 인기글 3개) 만큼 페이징 처리 ex) 일반 게시글 100 + 상단 인기글 3 = 103 of 11페이지
		return new PageImpl<>(combined, pageable, posts.getTotalElements() + topPopularDtos.size());
	}

	// 부모게시판 최신순, 인기순 정렬
	@Override
	public Page<PostListResponseDTO> getPostsByParentBoard(Long parentBoardId, String sortBy, Pageable pageable) {

		logger.info("PostServiceImpl getPostsByParentBoard() Start");

		// 부모 게시판 가져오기(JPA 영속성 상태)
		Board parentBoard =boardRepository.findById(parentBoardId)
				                          .orElseThrow(() -> {
				                        	  logger.error("PostServiceImpl NoSuchElementException() Error : 부모 게시판이 존재하지 않습니다.");
				                        	  return new NoSuchElementException("부모 게시판이 존재하지 않습니다.");
				                          });

		// 자식 게시판들 가져오기(JPA 영속성 상태)
		List<Board> childBoards = boardRepository.findByParentBoard_BoardId(parentBoardId);

		if(childBoards.isEmpty()) {
			return Page.empty(pageable);
		}

		// 자식 게시판 ID 가져오기
		List<Long> childBoardIds= childBoards.stream()
		                                     .map(childBoard -> childBoard.getBoardId())
		                                     .collect(Collectors.toList());

		// 인기글 2일 기준으로 시간 계산
		LocalDateTime recentThreshold = LocalDateTime.now().minusDays(popularDayLimit);

	    // 상단 인기 게시글 먼저 가져오기 (좋아요 50개 이상 + '오늘'기준으로 최근 3일)
	    List<Post> popularPosts = postRepository.findTopPopularPostsByBoards(childBoardIds, 
	    																	 popularLikeThreshold, 
	    																	 recentThreshold, 
	    																	 PageRequest.of(0, 3)); // 상단 인기글 고정은 3개 제한

	    Page<Post> posts;
	
	    if ("popular".equalsIgnoreCase(sortBy)) {
	        // 좋아요순 정렬 (댓글은 제외) + 좋아요수 동률이면 최신순 정렬
	        posts = postRepository.findActiveNonNoticePostsByBoardIdsOrderByLikesDescCreatedAtDesc(childBoardIds, pageable);
	    } else {
	        // 최신순 정렬 (기본)
	        posts = postRepository.findByBoardIds(childBoardIds, pageable);
	    }

	    // 상단 인기 게시글 DTO 리스트
	    List<PostListResponseDTO> topPopularDtos = popularPosts.stream()
                                                               .map(post -> PostListResponseDTO.fromEntity(post, 
                                                                                                           postReactionRepository.countByPostPostId(post.getPostId()), 
                                                                                                           memberRepository.findById(post.getAuthorId())
                                                                                                                           .map(member -> member.getNickname()).orElse(null)
                                                                                                          ))
                                                               .collect(Collectors.toList());

	    // 일반 게시글 DTO 리스트
	    List<PostListResponseDTO> nomalDtos = posts.stream()
	    		                                   .map(post -> PostListResponseDTO.fromEntity(post, 
	    		                                		                                       postReactionRepository.countByPostPostId(post.getPostId()), 
	    		                                		                                       memberRepository.findById(post.getAuthorId())
	    		                                		                                                       .map(member -> member.getNickname()).orElse(null)
	    		                                		                                      ))
	    		                                   .collect(Collectors.toList());
	    
	    // 게시글 합치기
	    List<PostListResponseDTO> combined = new ArrayList<>();

	    // 첫 페이지('pageNumber == 0')일때에만 상단 인기글 3개 고정
	    if (pageable.getPageNumber() == 0) {
	        combined.addAll(topPopularDtos);
	    }
	    combined.addAll(nomalDtos);

		logger.info("PostServiceImpl getPostsByParentBoard() Success End");

		return new PageImpl<>(combined, pageable, posts.getTotalElements() + topPopularDtos.size());
	}

	//*************************************************** Service END ***************************************************

}
