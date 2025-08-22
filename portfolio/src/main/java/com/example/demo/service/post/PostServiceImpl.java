package com.example.demo.service.post;

import java.io.File;




import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.example.demo.domain.board.Board;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.post.postimage.PostImage;
import com.example.demo.domain.post.postreaction.postreactionenums.PostReactionType;
import com.example.demo.domain.post.postreport.PostReport;
import com.example.demo.dto.post.PostNoticeBoardResponseDTO;
import com.example.demo.dto.post.PostParentBoardPostPageResponseDTO;
import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.dto.post.PostCreateRequestDTO;
import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostPageResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.post.postimage.ImageOrderDTO;
import com.example.demo.dto.post.postimage.PostImageResponseDTO;
import com.example.demo.repository.board.BoardRepository;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.post.PostRepository;
import com.example.demo.repository.post.PostRepository.PostAggregate;
import com.example.demo.repository.post.postimage.PostImageRepository;
import com.example.demo.repository.post.postreaction.PostReactionRepository;
import com.example.demo.repository.post.postreport.PostReportRepository;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.service.post.postimage.PostImageService;
import com.example.demo.validation.post.PostValidation;
import com.example.demo.validation.postimage.PostImageValidation;
import com.example.demo.validation.string.WordValidation;
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
	private final PostReportRepository postReportRepository;
	private final PostReactionRepository postReactionRepository;

	private final PostImageService fileService;
	private final NotificationService notificationService;

	// 공지게시판 BoardID = '1'로 고정
	public static final Long NOTICE_BOARD_ID = 1L;
	// 인기순 으로 정렬
	private static final String SORT_POPULAR = "popular";
    // 상단 공지글 3개 제한 변수
    private static final Pageable LIMIT_SIZE = PageRequest.of(0,3);
    // 좋아요
    private static final PostReactionType LIKE = PostReactionType.LIKE;
    // 싫어요
    private static final PostReactionType DISLIKE = PostReactionType.DISLIKE;

    // 이미지 없을시 대체할 기본 이미지
    private final String DEFAULT_THUMBNAIL_URL = "/images/default-thumbnail.jpg";

	// 파일 업로드 경로 ('application.properties'에서 경로처리)
	@Value("${file.upload.base-path}")
	private String basePath;

	// 인기 게시글 좋아요 기준
	@Value("${post.popular.likeThreshold}")
	private int popularLikeThreshold;
	
	// 인기 게시글 날짜 기준
	@Value("${post.popular.dayLimit}")
	private int popularDayLimit;

	// 인기 게시글 '좋아요' - '싫어요' 기준
	@Value("${post.popular.postNetLikeThreshold}")
	private int popularNetLikeThreshold;

	// 인기글 가져올 갯수
	@Value("${post.popular.limit}")
	private int popularLimit;

	// 부모게시판 인기글 날짜 기준
	@Value("${post.popular.parentDayLimit}")
	private int parentPopularDayLimit;

    // 게시글 신고 제한
    @Value("${post.report.threshold}")
    private Long reportThreshold;


    // 키: "postId:userId" 또는 "postId:ip"
    private final Cache<String, Long> viewCountCache = Caffeine.newBuilder()
    														   .expireAfterWrite(10, TimeUnit.MINUTES) // 10분 후 자동만료
    														   .maximumSize(10_000)	// 최대 1만개 저장
    														   .build();

    // 중복 방지 시간 간격 (밀리초), 예: 10분 = 600000ms
    private final long VIEW_COUNT_EXPIRATION = 10 * 60 * 1000;

	private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);

	//*************************************************** Method START ***************************************************//

	public String extractLocalPathFromUrl(String imageUrl) {

		logger.info("PostServiceImpl extractLocalPathFromUrl() Start");

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

		logger.info("PostServiceImpl extractLocalPathFromUrl() End");
		return fullPath.toString(); //OS에 맞게 경로 문자열 반환
	}

	private void deleteImageFile(String imageUrl) {
	    logger.info("PostServiceImpl deleteImageFile() Start");

	    try {
	        String imagePath = this.extractLocalPathFromUrl(imageUrl);
	        File file = new File(imagePath);

	        if (file.exists() && file.canRead()) {
	            boolean deleted = file.delete();
	            if (!deleted) {
	                logger.warn("이미지 파일 삭제 실패: {}", file.getAbsolutePath());
	            }
	        } else {
	            logger.warn("파일이 존재하지 않거나 읽을 수 없음: {}", file.getAbsolutePath());
	        }

	    } catch (IllegalArgumentException e) {
	        logger.warn("PostServiceImpl deleteImageFile() IllegalArgumentException : {}", e.getMessage(), e);
	        throw e;
	    } catch (Exception e) {
	        logger.error("PostServiceImpl deleteImageFile() RuntimeException : 파일 삭제 중 예외 발생");
	        throw new RuntimeException("파일 삭제 중 예외 발생");
	    } finally {
	        logger.info("PostServiceImpl deleteImageFile() End");
	    }
	}

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

	// 댓글 집계 Map 생성 메서드
	private Map<Long, Long> getCommentCountMap(List<Long> postIds) {

		if(postIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<CommentRepository.PostCommentCount> commentCounts = commentRepository.countCommentsByPostIds(postIds);

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
		List<MultipartFile> images = postCreateRequestDTO.getImages();

		if(!WordValidation.containsForbiddenWord(dtoTitle)) {
			logger.error("PostServiceImpl createPost() IllegalArgumentException : 게시글 제목에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("게시글 제목에 비속어가 포함되어있습니다.");
		}

		if(!WordValidation.containsForbiddenWord(dtoContent)) {
			logger.error("PostServiceImpl createPost() IllegalArgumentException : 게시글 내용에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("게시글 내용에 비속어가 포함되어있습니다.");
		}

		Board board = boardRepository.findById(dtoBoardId)
				                     .orElseThrow(() -> {
				                    	 logger.error("PostServiceImpl createPost() NoSuchElementException dtoBoardId : {}", dtoBoardId);
				                    	 return new NoSuchElementException("게시판이 존재하지 않습니다.");
				                     });

		Member member = memberRepository.findById(dtoAuthorId)
				                        .orElseThrow(() -> {
				                        	logger.error("PostServiceImpl createPost() NoSuchElementException dtoAuthorId : {}", dtoAuthorId);
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		// '게시글 생성 Setting'
		Post post = Post.builder()
				        .board(board)
				        .title(dtoTitle)
				        .content(dtoContent)
				        .author(member)
				        .isNotice(dtoIsNotice)
				        .status(postStatus)
				        .createdAt(createdAt)
				        .updatedAt(updatedAt)
				        .build();

		// 게시글이 'INSERT' 되기전에 등급을 올림
		// 만약 게시글은 올라갔지만, 이미지가 에서 예외 발생시,
		// '@Transactional'어노테이션에 의해서 등급 점수가 롤백되므로 안전함. 
		member.insertPostScore(); // 등급 점수 올리기

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
		if(!PostImageValidation.isValidImages(images)) {
			logger.info("PostServiceImpl createPost() : 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, 
															0, 
															userNickname,
															0,
															0);
		}


		// 실제 유효한 파일인지 추가 필터링(filter) 작업
		// 이미지 파일이 'null'이 아니거나, '빈 리스트([])' 필터링
		List<MultipartFile> validFiles = images.stream()
				                               .filter(file -> file != null && !file.isEmpty())
				                               .toList();

		// 유효한 파일이 하나도 없으면 업로드 스킵
		if (validFiles.isEmpty()) {
		    logger.info("PostServiceImpl createPost() : 유효한 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, 
															0, 
															userNickname,
															0,
															0);
		}

		// 이미지 업로드
		try {
			this.savePostImages(postId, validFiles);
		} catch (IllegalArgumentException e) {
			logger.error("PostServiceImpl createPost()  IllegalArgumentException  : {}", e.getMessage() ,e);
			// 예외를 발생시켜 메소드를 비정상적으로 종료,
			// 메소드가 비정상적으로 종료되었으모르, '@Transactional'어노테이션에 의해 롤백 유도
			throw e;
		} catch (IllegalStateException e) {
			// 예외를 발생시켜 메소드를 비정상적으로 종료,
			// 메소드가 비정상적으로 종료되었으모르, '@Transactional'어노테이션에 의해 롤백 유도
			logger.error("PostServiceImpl createPost()  IllegalStateException  : {}", e.getMessage() ,e);
			throw e;
		}catch (RuntimeException e) {
			logger.error("PostServiceImpl createPost()  RuntimeException : {}", e.getMessage() ,e);
			// 예외를 발생시켜 메소드를 비정상적으로 종료,
			// 메소드가 비정상적으로 종료되었으모르, '@Transactional'어노테이션에 의해 롤백 유도
			throw e;
		}

		logger.info("PostServiceImpl createPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, 
														0, 
														userNickname,
														0,
														0);
	}

	// 이미지 생성 Service
	@Override
	@Transactional
	public void savePostImages(Long postId, List<MultipartFile> files) {

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


		// 이미지 정렬을 위한 변수
		// 만약 이미지가 중간에 삭제되면은 orderNum이 중복될 수 있으므로,
		// 이미지가 없을때는 '0'을 반환, 이미지가 있다면 다음 이미지의 'orderNum'을 위해 '+1'씩 증가
		int order = post.getImages().stream() // -> post -> List<PostImage> -> Stream<PostImage>
				                    .mapToInt(PostImage :: getOrderNum) // Stream<Intger>
				                    .max() // Stream<Integer>중에 가장 큰 수 반환 후 '무조건 +1'
				                    .orElse(-1) +1; // 만약 이미지가 없다면 '-1'을 반환후 '무조건 +1'

			/**
			 *	DB 원자성(한번에 트랜잭션이 되는지 안되는지),
			 *	DB 일관성(DB와 서버 파일이 매칭 안될시에) 'RollBack' 
			 */
			for(MultipartFile file : files){
				
				if(!PostImageValidation.isValidFile(file)) {
					logger.error("PostServiceImpl savePostImages() IllegalArgumentException : 유효하지 않은 파일이 포함되어 있습니다.");
					throw new IllegalArgumentException("유효하지 않은 파일이 포함되어 있습니다.");
				}

				String url = null;

				try {
					url = fileService.uploadToLocal(file);
				} catch (IllegalArgumentException e) {
					logger.error("PostServiceImpl savePostImages() IllegalArgumentException : {}", e.getMessage(), e);
					// 이미지 업로드중에, 예외가 발생하면은 업로드 완료된 이미지들 삭제
					this.deleteImageFile(url);
					// 그 후, 예외를 발생시켜 롤백 유도
					throw e; // 폭탄 돌리기 시작
				} catch (IllegalStateException e) {
					logger.error("PostServiceImpl savePostImages() IOException : {}", e.getMessage(), e);
					// 이미지 업로드중에, 예외가 발생하면은 업로드 완료된 이미지들 삭제
					this.deleteImageFile(url);
					// 그 후, 예외를 발생시켜 롤백 유도
					throw e; // 폭탄 돌리기 시작
				} catch (RuntimeException e) {
					logger.error("PostServiceImpl savePostImages() RuntimeException : {}", e.getMessage(), e);
					// 이미지 업로드중에, 예외가 발생하면은 업로드 완료된 이미지들 삭제
					this.deleteImageFile(url);
					// 그 후, 예외를 발생시켜 롤백 유도
					throw e; // 폭탄 돌리기 시작
				}

				if(url == null) {
					// 'url'이 null 일경우 메소드 종료
					return;
				}

				PostImage image = new PostImage();
				image.setPost(post);
				image.setImageUrl(url);
				// 여러장 이미지 업로드시 다음 orderNum을 위한 '후위증감연산자'
				image.setOrderNum(order++);
				image.setCreatedAt(LocalDateTime.now());

				// JPA 양방향 관계 연결
				post.addImage(image);

			}
		logger.info("PostServiceImpl savePostImages() Success End");
	}

	// 게시판 수정 Service
	@Override
	@Transactional
	public PostResponseDTO updatePost(Long postId, PostUpdateRequestDTO postUpdateRequestDTO, Long authorId, String userNickname) {

		logger.info("PostServiceImpl updatePost() Start");

		// DB 변경여부
		boolean dbSetting = false;

		// Request
		String dtoTitle = UriUtils.decode(postUpdateRequestDTO.getTitle(), StandardCharsets.UTF_8);
		String dtoContent = UriUtils.decode(postUpdateRequestDTO.getContent(), StandardCharsets.UTF_8);
		boolean dtoIsNotice = postUpdateRequestDTO.isNotice();
		LocalDateTime updatedAt = LocalDateTime.now();
		List<MultipartFile> images = postUpdateRequestDTO.getImages();

		if(!WordValidation.containsForbiddenWord(dtoTitle)) {
			logger.error("PostServiceImpl updatePost() IllegalArgumentException : 게시글 제목에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("게시글 제목에 비속어가 포함되어있습니다.");
		}		

		if(!WordValidation.containsForbiddenWord(dtoContent)) {
			logger.error("PostServiceImpl updatePost() IllegalArgumentException : 게시글 내용에 비속어가 포함되어있습니다.");
			throw new IllegalArgumentException("게시글 내용에 비속어가 포함되어있습니다.");
		}		

		// 'JPA'가 '게시글 조회(findBy)'를 시작으로 '게시글 엔티티' 영속성을 유지
		Post post= postRepository.findById(postId)
		                         .orElseThrow(() -> {
		                        	 logger.error("PostServiceImpl updatePost() NoSuchElementException Error : {}");
		                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
		                         });

		// DB
		String dbTitle = post.getTitle();
		String dbContent = post.getContent();
		boolean dbIsNotice = post.isNotice();

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
		Long postAuthorId =post.getAuthor().getId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("PostServiceImpl updatePost() SecurityException Error : DB의 작성자ID와 Request 작성자ID가 일치하지 않습니다.");
			throw new SecurityException("작성자만 수정할 수 있습니다.");
		}

		// 영속 상태인 '게시글 DB 업데이트 Start'
		if(!dtoTitle.isEmpty() && !dtoTitle.equals(dbTitle)) {
			post.setTitle(dtoTitle);
			dbSetting = true;
		}

		if(!dtoContent.isEmpty() && !dtoContent.equals(dbContent)) {
			post.setContent(dtoContent);
			dbSetting = true;
		}

		if(dtoIsNotice != dbIsNotice) {
			post.setNotice(dtoIsNotice);
			dbSetting = true;
		}

		// 만약 이미지 파일도 없고, DB에 단 한번도 업데이트 되지 않았다면
		if(!PostImageValidation.isValidImages(images) && dbSetting == false) {
			logger.error("PostServiceImpl updatePost() IllegalArgumentException : 수정이 단 한번도 이뤄지지 않음.");
			throw new IllegalArgumentException("잘못된 접근 입니다.");
		}

		// 만약 한번이라도 DB가 업데이트 되었다면
		if(dbSetting == true) {
			// 수정 날짜를 현재 날짜로 변경
			post.setUpdatedAt(updatedAt);
		}
		
		// 댓글 갯수
		int commentCount = commentRepository.countByPostPostId(postId);

		//좋아요, 싫어요 갯수
		int likeCount = postReactionRepository.countByPostPostIdAndReactionType(postId, LIKE);
		int disLikeCount = postReactionRepository.countByPostPostIdAndReactionType(postId, DISLIKE);

		// 'Image'를 'Stream'시 'NullPointException'방지를 위한 유효성 체크
		if(!PostImageValidation.isValidImages(images)) {
			logger.info("PostServiceImpl updatePost() : 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, 
															commentCount, 
															userNickname,
															likeCount,
															disLikeCount);
		}

		// 이미지 파일 유효성 체크
		List<MultipartFile> validFiles = images.stream()
				                               .filter(file -> file != null && !file.isEmpty())
				                               .toList();

		// 유효한 파일이 하나도 없으면 업로드 스킵
		if (validFiles.isEmpty()) {
		    logger.info("PostServiceImpl updatePost() : 유효한 이미지 파일 없음, 업로드 스킵");
			return PostResponseDTO.convertToPostResponseDTO(post, 
															commentCount, 
															userNickname,
															likeCount,
															disLikeCount);
		}

		// 영속 상태인 '게시글 DB 업데이트 End'

		try {
			// 새로운 이미지 저장
			this.savePostImages(postId, validFiles);
		} catch (IllegalArgumentException e) {
			logger.error("PostServiceImpl updatePost()  IllegalArgumentException Error : {}", e.getMessage() ,e);
			// 예외를 발생시켜 메소드를 비정상적으로 종료되었으므로,
			// @Transactional'어노테이션에 의해 이미지 파일 , 게시글 테이블 롤백 유도 
			throw e;
		} catch (NoSuchElementException e) {
			logger.error("PostServiceImpl updatePost()  NoSuchElementException Error : {}", e.getMessage() ,e);
			// 예외를 발생시켜 메소드를 비정상적으로 종료되었으므로,
			// @Transactional'어노테이션에 의해 이미지 파일 , 게시글 테이블 롤백 유도 
			throw e;
		} catch (Exception e) {
			logger.error("PostServiceImpl updatePost()  Exception Error : {}", e.getMessage() ,e);
			// 예외를 발생시켜 메소드를 비정상적으로 종료되었으므로,
			// @Transactional'어노테이션에 의해 이미지 파일 , 게시글 테이블 롤백 유도 
			throw e; //롤백 유도
		}

		logger.info("PostServiceImpl updatePost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, 
														commentCount, 
														userNickname,
														likeCount,
														disLikeCount);
	}

	// 게시글 삭제 Service
	@Override
	@Transactional
	public void deletePost(Long postId, Long authorId, boolean isDeleteImages) {

		logger.info("PostServiceImpl deletePost() Start");

		// Request
		Post post = postRepository.findById(postId)
								  .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl deletePost() NoSuchElementException Error : 게시글이 존재하지 않습니다.");
			                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
								  });


		Long boardId = post.getBoard().getBoardId();
		Member member = memberRepository.findById(authorId)
		                                .orElseThrow(() -> {
				                        	 logger.error("PostServiceImpl deletePost() NoSuchElementException Error : 회원이 존재하지 않습니다.");
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
		Long postAuthorId = post.getAuthor().getId();

		// '작성자ID' 비교
		if(!Objects.equals(postAuthorId, authorId)) {
			logger.error("PostServiceImpl deletePost() SecurityException Error : DB의 작성자ID와 Request 작성자ID가 일치하지 않습니다..");
			throw new SecurityException("작성자만 삭제할 수 있습니다.");
		}

		LocalDateTime updatedAt = LocalDateTime.now();

		// 게시물 삭제시 등급점수 '-1' 차감
		member.deletePostScore();

		// 논리적 삭제(O), 물리적 삭제(X) 즉, DB에는 남아있음
		post.setStatus(PostStatus.DELETED);
		post.setUpdatedAt(updatedAt);

		// 게시글 리액션 DB 모두 삭제
		postReactionRepository.deleteByPost(post);

		if(isDeleteImages) {
			try {
				this.deletePostImages(postId);
			} catch (RuntimeException e) {
				logger.error("PostServiceImpl deletePost RuntimeException : ",e.getMessage(),e);
				throw e;
			}
			
		}

		logger.info("PostServiceImpl deletePost() Success End");
	}

	// 실제 파일 삭제
	@Override
	@Transactional
	public void deletePostImages(Long postId) {

		logger.info("PostServiceImpl deletePostImages() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
				                	  logger.error("PostServiceImpl deletePostImages : 회원이 존재하지 않습니다.");
				                	  throw new IllegalArgumentException("회원이 존재하지 않습니다.");
				                  });

		List<PostImage> images= postImageRepository.findByPostPostId(postId);

		for(PostImage image : images) {

			String imagePath = image.getImageUrl();

			// 서버 이미지 삭제
	        try {
	            this.deleteImageFile(imagePath);
	        } catch(RuntimeException e) {
	            logger.error("PostServiceImpl deletePostImages RuntimeException() : 이미지 파일 삭제 실패: {} - {}", imagePath, e.getMessage());
	            // 이미지 파일 삭제중 예외가 발생하면 DB와 서버의 이미지가 서로 일관되게 유지되어야 하므로 롤백 
	            throw e;
	        }

			// DB 이미지 삭제
	        // postImageRepository.delete(image);
		    post.getImages().remove(image);
		}

		logger.info("PostServiceImpl deletePostImages() Success End");
	}

	// 게시글 신고
	@Override
	@Transactional
	public String reportPost(Long postId, Long reporterId, String reason) {

		logger.info("PostServiceImpl reportPost() Start");

		// 신고당한 게시글 가져오기 Entity
		// 'JPA'의해 영속성 상태
		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
				                	  logger.error("PostServiceImpl reportPost() NoSuchElementException : 게시글이 존재하지 않습니다.");
				                	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
				                  });

		Member member = memberRepository.findById(post.getAuthor().getId())
				                        .orElseThrow(() -> {
				                        	logger.error("PostServiceImpl reportPost() NoSuchElementException postId : {} ", postId);
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		// 삭제된 게시글은 신고 X
		if(post.getStatus() == PostStatus.DELETED) {
			logger.error("PostServiceImpl reportPost() IllegalStateException : 삭제된 게시글은 신고할 수 없습니다.");
			throw new IllegalStateException("삭제된 게시글은 신고할 수 없습니다.");
		}

		if(post.getAuthor().getId().equals(reporterId)) {
			logger.error("PostServiceImpl reportPost() IllegalStateException : 본인이 본인의 게시글을 신고할 수 없습니다.");
			throw new IllegalStateException("본인이 본인의 게시글을 신고할 수 없습니다.");
		}

		// 게시글 신고 중복 방지(게시글 신고 테이블에 '해당'게시글을 신고한 회원이 존재하는지 여부 체그
		boolean alreadyReported = postReportRepository.existsByPostAndReporterId(post, reporterId);
	
		if(alreadyReported) {
			logger.error("PostServiceImpl reportPost() IllegalStateException : 이미 신고한 게시글입니다.");
			throw new IllegalStateException("이미 신고한 게시글입니다.");
		}

		// 신고 저장
		PostReport report =  PostReport.builder()
				                       .post(post) // 신고할 게시글
				                       .reporterId(reporterId) // 신고자의ID(PK)
				                       .reason(reason) // 신고 이유
				                       .build();

		postReportRepository.save(report);

		// 신고 누적 카운트 증가
		post.incrementReportCount();

		// 신고 갯수 가져오기
		Long reportCount = post.getReportCount();

		logger.info("PostServiceImpl reportPost() reportCount : {}", reportCount);

		// 신고를 35번 당할시 상태변경 (report_count(DB) == 40)
		// 그리고(AND(&&)), 게시글이 'ACTIVE'이고, 공지글이 아닌 게시글만 'BLOCKED"
		if(Objects.equals(reportCount, this.reportThreshold) 
		   && post.getStatus().equals(PostStatus.ACTIVE)
		   && post.isNotice() == false) {
			// 게시글이 신고로인해 'BLOCKED' 될시 회원점수 20점 차감
			member.benPostScore();
			// 'post'가 영속성상태이므로 값이 변경되면 알아서 DB변경 시도
			post.setStatus(PostStatus.BLOCKED);
			// 게시글 리액션 DB 모두 삭제
			postReactionRepository.deleteByPost(post);
			// 게시글이 'BLOCKED'될시 게시글 작성자에게 알림 보내기
			notificationService.notifyPostWarned(post);
		}

		logger.info("PostServiceImpl reportPost() End");
		return "게시글 신고가 접수되었습니다.";
	}

	// 이미지 목록 조회 Service
	@Override
	public List<PostImageResponseDTO> getPostImages(Long postId) {

		logger.info("PostServiceImpl getPostImages() Start");

		// 게시글 존재 여부 확인
		Post post = postRepository.findById(postId)
		                          .orElseThrow(() -> {
		                        	  logger.error("PostServiceImpl getPostImages() NoSuchElementException Error : {}");
		                        	  return new NoSuchElementException("게시글이 존재하지 않습니다.");
		                          });

		List<PostImage> images = postImageRepository.findByPostPostId(postId);

		logger.info("PostServiceImpl getPostImages() End");
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
	                            	logger.error("PostServiceImpl updateImageOrder() NoSuchElementException Error : 게시글을 찾을 수 없습니다.");
	                            	return new NoSuchElementException("게시글을 찾을 수 없습니다.");  
	                              });

	    if (!post.getAuthor().getId().equals(requestAuthorId)) {
	    	logger.error("PostServiceImpl updateImageOrder() SecurityException : 작성자만 이미지 순서를 변경할 수 있습니다.");
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

	    if (!post.getAuthor().getId().equals(requestAuthorId)) {
	    	logger.error("PostServiceImpl deleteSingleImage() SecurityException : 작성자만 이미지를 삭제할 수 있습니다.");
	        throw new SecurityException("작성자만 이미지를 삭제할 수 있습니다.");
	    }

	    PostImage image = postImageRepository.findById(imageId)
	                                         .orElseThrow(() -> {
	                                        	 logger.error("PostServiceImpl deleteSingleImage() NoSuchElementException Error : {}");
	                                        	 return new NoSuchElementException("이미지를 찾을 수 없습니다");
	                                         });

	    String imageUrl = image.getImageUrl();

	    try {
	        this.deleteImageFile(imageUrl);
	    } catch (RuntimeException e) {
	    	logger.error("PostServiceImpl deleteSingleImages RuntimeException() : 이미지 파일 삭제 실패: {} - {}", imageUrl, e.getMessage());
	        throw e;
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

	    if (!post.getAuthor().getId().equals(requestAuthorId)) {
	    	logger.error("PostServiceImpl deleteAllImages() SecurityException : 작성자만 이미지를 삭제할 수 있습니다.");
	        throw new SecurityException("작성자만 이미지를 삭제할 수 있습니다.");
	    }

	    List<PostImage> images = new ArrayList<>(post.getImages()); // 복사

	    for (PostImage image : images) {
	        try {
	            this.deleteImageFile(image.getImageUrl());// 실제 파일 삭제
	        } catch (RuntimeException e) {
	        	logger.error("PostServiceImpl deleteSingleImages RuntimeException() : 이미지 파일 삭제 실패: {} - {}", image.getImageUrl(), e.getMessage());
	            throw e;
	        }
	    }

	    post.getImages().removeAll(images); // 영속성 제거 → orphanRemoval 트리거

	    logger.info("PostServiceImpl deleteAllImages() Success End");
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

	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기 Service
	@Override
	public List<PostListResponseDTO> getTop3PinnedNoticesByBoard() {

		logger.info("PostServiceImpl getTop3PinnedNoticesByBoard() Start");

		Board board = boardRepository.findById(NOTICE_BOARD_ID)
		                             .orElseThrow(() -> new NoSuchElementException("게시판이 존재하지 않습니다."));

		Pageable pageable = LIMIT_SIZE;
	
		List<Post> posts = postRepository.findTop3PinnedByBoard(board, pageable);

		List<Long> postIds = posts.stream()
				                  .map(post -> post.getPostId())
				                  .collect(Collectors.toList());

		Map<Long, Long> likeCountMap = this.getLikeCountMap(postIds);
		Map<Long, String> nicknameMap = this.getNicknameMap(posts);

		logger.info("PostServiceImpl getTop3PinnedNoticesByBoard() Success End");
		return posts.stream()
				    .map(post -> PostListResponseDTO.fromEntity(post, 
				    		                                    likeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
				    		                                    nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음")
				    		                                   ))
				    .collect(Collectors.toList());
	}

	// 게시글 단건 조회 Service
	@Override
	public PostResponseDTO getPost(Long postId) {

		logger.info("PostServiceImpl getPost() Start");

		Post post = postRepository.findById(postId)
				                  .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl getPost() NoSuchElementException Error : 게시글이 존재하지 않습니다.");
			                        	 return new NoSuchElementException("게시글이 존재하지 않습니다."); 
				                  });

		String userNickname = post.getAuthor().getNickname();

		// 단건 게시글 댓글, 좋아요, 싫어요 집계 조회
		PostAggregate postAggregate = postRepository.findPostAggregateByPostId(postId)
				                                    .orElseGet(() -> { 
				                                    	logger.error("PostServiceImpl getPost() postId : {} ", postId);
				                                    	return new PostAggregate() {
				                                    		@Override
				                                    		public Long getPostId() {
				                                    			return postId;
				                                    		}
				                                    		@Override
				                                    		public Long getCommentCount() {
				                                    			return 0L;
				                                    		}
				                                    		@Override
				                                    		public Long getLikeCount() {
				                                    			return 0L;
				                                    		}
				                                    		@Override
				                                    		public Long getDislikeCount() {
				                                    			return 0L;
				                                    		}
				                                    	};
				                                    });

		logger.info("PostServiceImpl getPost() Success End");
		return PostResponseDTO.convertToPostResponseDTO(post, 
														postAggregate.getCommentCount().intValue(), 
														userNickname,
														postAggregate.getLikeCount().intValue(),
														postAggregate.getDislikeCount().intValue());
	}

	// 자식게시판 게시글 목록 조회 (ACTIVE + 공지글 제외)
	@Override
	public PostPageResponseDTO getPostsByBoard(Long boardId, Pageable pageable) {

		logger.info("PostServiceImpl getPostsByBoard() Start");

		Board board = boardRepository.findById(boardId)
				                     .orElseThrow(() -> {
			                        	 logger.error("PostServiceImpl getPostsByBoard() NoSuchElementException boardId : {} ",boardId);
			                        	 return new NoSuchElementException("게시판이 존재하지 않습니다."); 
				                     });


	    // 인기글 담을 List
	    List<PostListResponseDTO> topPopularPostsDto = null;
	    // 인기글 최근 2일 기준 시간 계산
	    LocalDateTime recentThreshold = LocalDateTime.now().minusDays(popularDayLimit);
	    // 현재 페이지 
	    int currentPage = pageable.getPageNumber();

	    // 첫 페이지만 인기글 보여주기
	    if(currentPage == 0) {
	    	// (좋아요 50 이상, 순좋아요 15 이상, 최근 2일 이내 게시글 4개 추출)
	    	List<Post> topPopularPosts = postRepository.findTopPostsByBoardWithNetLikes(board.getBoardId(),
	    			                                                                    popularLikeThreshold,
	    			                                                                    popularNetLikeThreshold,
	    			                                                                    recentThreshold,
	    			                                                                    popularLimit
	    			                                                                    );

	    	List<Long> topPostIds = topPopularPosts.stream()
	    			                               .map(post -> post.getPostId())
	    			                               .collect(Collectors.toList());

	    	// 집계 조회 (좋아요, 댓글, 작성자 닉네임)
	    	Map<Long, Long> topLikeCountMap = this.getLikeCountMap(topPostIds);
	    	Map<Long, Long> topCommentCountMap = this.getCommentCountMap(topPostIds);
	    	Map<Long, String> topNicknameMap = this.getNicknameMap(topPopularPosts);

	    	topPopularPostsDto = topPopularPosts.stream()
	    			                            .map(post -> PostListResponseDTO.fromEntity(post, 
	    			                            		                                    topLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    			                            		                                    topNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	    			                            		                                    topCommentCountMap.getOrDefault(post.getPostId(), 0L).intValue()
	    			                            		                                   ))
	    			                            .collect(Collectors.toList());
	    }

	    /* 
	     * 일반 게시글 가져오기 
		 * Page(현재 페이지에 포함될 게시글 리스트, 페이지당 보여줄 개수, 전체 게시글 수)
		 * => 전체 댓글 수 기준으로 페이징 처리를 수행
		 **/
	    Page<Post> postPage = postRepository.findByBoardAndStatusAndIsNoticeFalseOrderByCreatedAtDesc(board, PostStatus.ACTIVE, pageable);

	    List<Long> postIds = postPage.stream()
	    		                     .map(post -> post.getPostId())
	    		                     .collect(Collectors.toList());

    	// 집계 조회 (좋아요, 댓글, 작성자 닉네임)
    	Map<Long, Long> normalLikeCountMap = this.getLikeCountMap(postIds);
    	Map<Long, Long> normalCommentCountMap = this.getCommentCountMap(postIds);
    	Map<Long, String> normalNicknameMap = this.getNicknameMap(postPage.getContent());

	    // 일반 게시글만 DTO 변환(Page<Post> -> Page<PostListResponseDTO>)
	    // 'Page'는 'Stream'반환 없이 'map'을 이용해 바로 'Page'반환 가능
	    Page<PostListResponseDTO> normalPostPage = postPage.map(post -> PostListResponseDTO.fromEntity(post, 
	    		                                                                                       normalLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    		                                                                                       normalNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	    		                                                                                       normalCommentCountMap.getOrDefault(post.getPostId(), 0L).intValue()
	    		                                                                                      ));

		logger.info("PostServiceImpl getPostsByBoard() Success End");

			   //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
			   //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return PostPageResponseDTO.fromPage(topPopularPostsDto, normalPostPage);
	}

	// 자식게시판 정렬(최신순, 좋아요 수)
	@Override
	public PostPageResponseDTO getPostsByBoardSorted(Long boardId, String sortBy, Pageable pageable) {

	    logger.info("PostServiceImpl getPostsByBoardSorted() Start");

	    Board board = boardRepository.findById(boardId)
	                                 .orElseThrow(() -> {
	                                	 logger.error("PostServiceImpl getPostsByBoardSorted() NoSuchElementException boardId : {}",boardId);
	                                	 return new NoSuchElementException("게시판이 존재하지 않습니다.");
	                                 });

	    // 인기글 담을 List
	    List<PostListResponseDTO> topPopularPostsDto = null;
	    // 인기글 최근 2일 기준 시간 계산
	    LocalDateTime recentThreshold = LocalDateTime.now().minusDays(popularDayLimit);
	    // 현재 페이지 
	    int currentPage = pageable.getPageNumber();

	    // 첫 페이지만 인기글 보여주기
	    if(currentPage == 0) {
	    	// (좋아요 50 이상, 순좋아요 15 이상, 최근 2일 이내 게시글 4개 추출)
	    	List<Post> topPopularPosts = postRepository.findTopPostsByBoardWithNetLikes(board.getBoardId(),
	    			                                                                    popularLikeThreshold,
	    			                                                                    popularNetLikeThreshold,
	    			                                                                    recentThreshold,
	    			                                                                    popularLimit
	    			                                                                    );

	    	List<Long> topPostIds = topPopularPosts.stream()
	    			                               .map(post -> post.getPostId())
	    			                               .collect(Collectors.toList());

	    	// 집계 조회 (좋아요, 댓글, 작성자 닉네임)
	    	Map<Long, Long> topLikeCountMap = this.getLikeCountMap(topPostIds);
	    	Map<Long, Long> topCommentCountMap = this.getCommentCountMap(topPostIds);
	    	Map<Long, String> topNicknameMap = this.getNicknameMap(topPopularPosts);

	    	topPopularPostsDto = topPopularPosts.stream()
	    			                            .map(post -> PostListResponseDTO.fromEntity(post, 
	    			                            		                                    topLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    			                            		                                    topNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	    			                            		                                    topCommentCountMap.getOrDefault(post.getPostId(), 0L).intValue()
	    			                            		                                   ))
	    			                            .collect(Collectors.toList());
	    }

	    Page<Post> postPage = null;

	    if (SORT_POPULAR.equalsIgnoreCase(sortBy)) {
	        // 인기순 정렬: 좋아요 + 댓글 수 내림차순, 그 다음 최신순 정렬 (PostRepository에 쿼리 추가 필요)
	    	postPage = postRepository.findPopularPostsByBoard(board, pageable);
	    } else {
	        // 최신순 정렬: 활성 + 공지글 제외, 최신순 (기존 메서드 활용)
	    	postPage = postRepository.findByBoardAndStatusAndIsNoticeFalseOrderByCreatedAtDesc(board, PostStatus.ACTIVE, pageable);
	    }

	    List<Long> postIds = postPage.stream()
                                     .map(post -> post.getPostId())
                                     .collect(Collectors.toList());

    	// 집계 조회 (좋아요, 댓글, 작성자 닉네임)
    	Map<Long, Long> normalLikeCountMap = this.getLikeCountMap(postIds);
    	Map<Long, Long> normalCommentCountMap = this.getCommentCountMap(postIds);
    	Map<Long, String> normalNicknameMap = this.getNicknameMap(postPage.getContent());

	    Page<PostListResponseDTO> normalPostPage = postPage.map(post -> PostListResponseDTO.fromEntity(post, 
	    		                                                                                       normalLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    		                                                                                       normalNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	    		                                                                                       normalCommentCountMap.getOrDefault(post.getPostId(), 0L).intValue()
	    		                                                                                      ));

	    logger.info("PostServiceImpl getPostsByBoardSorted() Success End");

	    return PostPageResponseDTO.fromPage(topPopularPostsDto, normalPostPage);
	}

	// 게시글 키워드 검색 (제목 또는 본문에 키워드 포함 + ACTIVE상태) Service
	@Override
	public MainPostPageResponseDTO searchPostsByKeyword(String keyword, Pageable pageable) {

		logger.info("PostServiceImpl searchPostsByKeyword() Start");

		// 'ACTIVE' 상태의 '게시글(들)'의 '제목 또는 본문내용'을 '대소문자' 상관 없이 '검색' 
		Page<Post> posts = postRepository.searchByKeyword(keyword, pageable);

		List<Long> postIds = posts.stream()
				                  .map(post -> post.getPostId())
				                  .collect(Collectors.toList());

		// 집계 조회 (좋아요, 댓글 수, 닉네임, 대표이미지)
		Map<Long, Long> likeCountMap = this.getLikeCountMap(postIds);
		Map<Long, Long> commentCountMap = this.getCommentCountMap(postIds);
		Map<Long, String> nicknameMap = this.getNicknameMap(posts.getContent());
		Map<Long, String> thumbnailMap = this.getThumbnails(postIds);

		Page<PostListResponseDTO> dtoPage = posts.map(post -> PostListResponseDTO.fromEntity(post, 
																							 likeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
																							 nicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
																							 commentCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
																							 thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)
				                                                                            )); 

		logger.info("PostServiceImpl searchPostsByKeyword() End");

		       //Stream<T>와 달리, Page<T>는 데이터를 'map'을 이용히여,
		       //'가공(원하는 자료형으로 변환)' 할 수 있다. 
		return MainPostPageResponseDTO.fromPage(dtoPage);
	}

	// 작성자별 게시글 조회 Service
	@Override
	public MainPostPageResponseDTO getPostsByAuthorNickname(String nickname, Pageable pageable) {

		logger.info("PostServiceImpl getPostsByAuthorNickname() Start");

		Member member = memberRepository.findByNickname(nickname)
									    .orElseThrow(() -> {
									    	logger.error("PostServiceImpl getPostsByAuthorNickname() NoSuchElementException nickname : {}", nickname);
									    	return new NoSuchElementException("작성자를 찾을 수 없습니다.");
									    });

		// 작성자가 작성한 게시글들 가져오기
		Page<Post> posts = postRepository.findByAuthorAndStatus(member,PostStatus.ACTIVE,pageable);

		List<Long> postIds = posts.stream()
								  .map(post -> post.getPostId())
								  .collect(Collectors.toList());

		// 집계 조회(좋아요, 댓글수, 대표이미지)
		Map<Long, Long> likeCountMap = this.getLikeCountMap(postIds);
		Map<Long, Long> commentCountMap = this.getCommentCountMap(postIds);
		Map<Long, String> thumbnailMap = this.getThumbnails(postIds); 

		Page<PostListResponseDTO> dtoPage = posts.map(post -> PostListResponseDTO.fromEntity(post, 
				                                                                             likeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
				                                                                             nickname,
				                                                                             commentCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
				                                                                             thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)
				                                                                            )); 

		logger.info("PostServiceImpl getPostsByAuthorNickname() Success End");

		return MainPostPageResponseDTO.fromPage(dtoPage);
	}

	// 공지 게시판 공지글 조회 Service
	@Override
	public PostNoticeBoardResponseDTO getAllNotices(Pageable pageable) {

		logger.info("PostServiceImpl getAllNotices() Start");

	    // 현재 페이지
	    int currentPage = pageable.getPageNumber();

	    // 첫 페이지에 고정된 공지글 3개 변수
	    List<PostListResponseDTO> topNotices= null;
	
	    // 첫 페이지만 상단 고정 공지글 3개 보여주기
	    if(currentPage == 0) {
		    // 1. 상단 고정 공지글 조회(고정된 공지글 최대 3개 (내림차순))
	    	List<Post> pinnedNotices = postRepository.findTop3FixedNotices(LIMIT_SIZE);
	
	    	List<Long> pinnedNoticesIds = pinnedNotices.stream()
	    			                                   .map(post -> post.getPostId())
	    			                                   .collect(Collectors.toList());

	    	// 집계 조회(좋아요, 닉네임)
	    	Map<Long ,Long> pinLikeCountMap = this.getLikeCountMap(pinnedNoticesIds);
	    	Map<Long, String> pinNicknameMap = this.getNicknameMap(pinnedNotices);

	    	topNotices = pinnedNotices.stream()
                                      .map(post -> PostListResponseDTO.fromEntity(post, 
                                    		                                      pinLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
                                    		                                      pinNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음")
                                    		                                     ))
                                      .collect(Collectors.toList());
	    }

	    // 2. 공지 게시판 가져오기
	    Page<Post> noticePosts = postRepository.findNoticePosts(pageable);

	    List<Long> noticePostsIds = noticePosts.stream()
	    		                               .map(post -> post.getPostId())
	    		                               .collect(Collectors.toList());
    	// 집계 조회(좋아요, 닉네임)
    	Map<Long ,Long> normalLikeCountMap = this.getLikeCountMap(noticePostsIds);
    	Map<Long, String> normalNicknameMap = this.getNicknameMap(noticePosts.getContent());
  
    	
	    // Page<PostListResponseDTO> 변환
	    Page<PostListResponseDTO> noticePostsDto = noticePosts.map(post -> PostListResponseDTO.fromEntity(post, 
                																						  normalLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
                																						  normalNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음")
                																						 ));

		logger.info("PostServiceImpl getAllNotices() Success End");

		return PostNoticeBoardResponseDTO.from(topNotices, noticePostsDto);
	}

	// 부모게시판(자식게시판 인기글 보여주기(60일기반으로, 가중치계산하여 내림차순)
	@Override
	public PostParentBoardPostPageResponseDTO getPostsByParentBoard(Long parentBoardId, Pageable pageable) {

	    logger.info("PostServiceImpl getPostsByParentBoard() Start");

	    // 현재 페이지
	    int currentPage = pageable.getPageNumber();

	    // 첫페이지에 고정된 공지글 3개 변수
	    List<PostListResponseDTO> topNotices= null;
	
	    if(currentPage == 0) {
		    // 1. 상단 고정 공지글 조회(고정된 공지글 최대 3개 (내림차순))
	    	List<Post> pinnedNotices = postRepository.findTop3FixedNotices(LIMIT_SIZE);
	
	    	// 1-2. 상단 고정글(들)ID 조회
	    	List<Long> pinnedNoticesIds = pinnedNotices.stream()
	    			                                   .map(post -> post.getPostId())
	    			                                   .collect(Collectors.toList());

	    	Map<Long, Long> pinnedLikeCountMap = this.getLikeCountMap(pinnedNoticesIds);
	    	Map<Long, String> pinnedNicknameMap = this.getNicknameMap(pinnedNotices);

	    	topNotices = pinnedNotices.stream()
                                     .map(post -> PostListResponseDTO.fromEntity(post, 
                                    		                                     pinnedLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
                                    		                                     pinnedNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음")
                                    		                                    ))
                                     .collect(Collectors.toList());
	    }

	    // 자식 게시판 ID들 구하기
	    List<Long> childBoardIds = boardRepository.findChildBoardIds(parentBoardId);

	    // 2. 자식 게시판 인기글 (기간 제한 + 가중치)
	    Page<Post> popularPostsPage = postRepository.findPopularPostsByWeightedScore(childBoardIds, 
	    		                                                                     popularLikeThreshold, 
	    		                                                                     popularNetLikeThreshold, 
	    		                                                                     parentPopularDayLimit, 
	    		                                                                     pageable);

	   // 2-1. 인기글(들)의ID 조회

	    List<Long> popularPostIds = popularPostsPage.stream()
	    		                                    .map(post -> post.getPostId())
	    		                                    .collect(Collectors.toList());

	    Map<Long, Long> popularLikeCountMap = getLikeCountMap(popularPostIds);
	    Map<Long, String> popularNicknameMap = getNicknameMap(popularPostsPage.getContent());
	    Map<Long, Long> popularCommentCountMap = getCommentCountMap(popularPostIds);
	    Map<Long, String> thumbnailMap = getThumbnails(popularPostIds);

	    Page<PostListResponseDTO> popularPostsDto = popularPostsPage.map(post -> PostListResponseDTO.fromEntity(post, 
	    																										popularLikeCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    																										popularNicknameMap.getOrDefault(post.getAuthor().getId(), "알 수 없음"),
	    																		                                popularCommentCountMap.getOrDefault(post.getPostId(), 0L).intValue(),
	    																		                                thumbnailMap.getOrDefault(post.getPostId(), DEFAULT_THUMBNAIL_URL)
	    																 									   ));

	    logger.info("PostServiceImpl getPostsByParentBoard() Success End");
		return PostParentBoardPostPageResponseDTO.fromDTO(topNotices, popularPostsDto);
	}

	// 게시글 배치 삭제 
	// 조건 : 수정일자 기준으로 5년 지나고, 조회수가 100이하이며, 
	//       공지글이 아니며, ACTIVE상태인 게시글, pin으로 고정된 글 X
	// 결과 : 삭제된 게시글 수(레코드) 반환
	@Transactional
	public int deleteDeadPost (LocalDateTime cutDate, int maxViewCount) {

		logger.info("PostServiceImpl deleteDeadPost() Start");
		logger.info("PostServiceImpl deleteDeadPost() Success End");
		return postRepository.deleteDeadPost(cutDate, maxViewCount);
	}

	// 공지글 배치 삭제 
	// 조건 : 수정일자 기준으로 5년 지나고, 공지글(notice = true), ACTIVE인 게시글 
	// 결과 : 삭제된 게시글 수(레코드) 반환
	@Transactional
	public int deleteDeadNoticePost (LocalDateTime cutDate) {

		logger.info("PostServiceImpl deleteDeadPost() Start");
		logger.info("PostServiceImpl deleteDeadPost() Success End");
		return postRepository.deleteDeadNoticePost(cutDate);
	}

	//*************************************************** Service END ***************************************************

}
