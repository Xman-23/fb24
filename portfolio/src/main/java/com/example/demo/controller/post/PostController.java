package com.example.demo.controller.post;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.controller.member.MemberController;
import com.example.demo.domain.post.Post;
import com.example.demo.dto.post.PinUpdateRequestDTO;
import com.example.demo.dto.post.PostCreateRequestDTO;
import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.postimage.ImageOrderDTO;
import com.example.demo.dto.postimage.PostImageResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.post.PostService;
import com.example.demo.validation.board.BoardValidation;
import com.example.demo.validation.post.PostValidation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // 'Json'으로 요청,응답 
@RequestMapping("/posts")
@RequiredArgsConstructor // 'final', '@NonNull'필드 생성자 생성
public class PostController {

	// 외부, 내부 접근(X), 데이터 불변 유지
	private final PostService postService;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    // 클라이언트 IP 추출 메서드
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 IP가 있을 경우 첫 번째 IP만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

	//*************************************************** API Start ***************************************************//

	//게시글 생성 API엔드포인트
	@PostMapping						// '@ModelAttribute' = 파일 업로드 전용
	public ResponseEntity<?> createPost(@ModelAttribute @Valid PostCreateRequestDTO postCreateRequestDTO,
										BindingResult bindingResult,
									    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
		/*
		 * 유효성 검사 조건 요약 *
		 상황 | @Valid | @NotBlank | BindingResult | 결과 설명
		------------------------------------------------------
		 1.   |   X   |     X     |       X       | 유효성 검사 안 함 (단, JSON '{}(body)'자체가 없으면 400 터짐)
		 2.   |   X   |     O     |       X       | 유효성 검사 안 함 (@NotBlank는 작동하지 않음 → 무의미)
		 3.   |   O   |     O     |       X       | 유효성 검사 작동 → 조건 위반 시 Spring이 자동으로 400 Bad Request 반환
		 4.   |   O   |     O     |       O       | 유효성 검사 작동 + 실패 시 내가 직접 에러 메시지 처리 가능
		*/

		logger.info("PostController createPost() Start");

		if(bindingResult.hasErrors()) {
			logger.error("PostController createPost() Error :입력값이 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestAuthorId = customUserDetails.getMemberId();
		PostCreateRequestDTO requestDTO = postCreateRequestDTO;

		PostResponseDTO response = null;

		try {
			response  = postService.createPost(requestDTO, requestAuthorId);
		} catch (NoSuchElementException e) {
			logger.error("PostController createPost() NoSuchElementException Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController createPost() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController createPost() Success End");
		return ResponseEntity.ok(response);
	}

		// 게시글 수정 API엔드포인트
		@PatchMapping("/{postId}")
		public ResponseEntity<?> updatePost(@PathVariable(name = "postId") Long postId,
											@ModelAttribute @Valid PostUpdateRequestDTO postUpdateRequestDTO,
											BindingResult bindingResult,
											@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController updatePost() Start");

		if(bindingResult.hasErrors()) {
			logger.error("PostController updatePost() Error :입력값이 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController updatePost() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestPostId = postId;
		Long requestAuthorId = customUserDetails.getMemberId();
		PostUpdateRequestDTO requestDTO = postUpdateRequestDTO;
		
		// Response
		PostResponseDTO response = null;

		try {
			response = postService.updatePost(requestPostId, requestDTO, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("PostController updatePost() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController updatePost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController createPost() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController updatePost() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시글 삭제 API엔드포인트
	@DeleteMapping("/{postId}")
	public ResponseEntity<?> deletePost(@PathVariable(name = "postId") Long postId,
										@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController deletePost() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController deletePost() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestPostId = postId;
		Long requestAuthorId = customUserDetails.getMemberId();

		try {
			postService.deletePost(requestPostId, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("PostController deletePost() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController deletePost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController deletePost() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController deletePost() Success End");
		// HTTP 200 OK 상태를 보내되, 응답 본문(body)은 비워서 반환한다
		// 즉,반환할 데이터가 없고, 성공만 알리면 되는 경우
		return ResponseEntity.noContent().build();
	}

	// 게시글 단건 조회 API엔드포인트
	@GetMapping("/{postId}")
	public ResponseEntity<?> getPost(@PathVariable(name = "postId") Long postId) {

		logger.info("PostController getPost() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController getPost() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestPostId = postId;

		// Response
		PostResponseDTO response = null;

		try {
			response = postService.getPost(requestPostId);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPost() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPost() Success End");
		return ResponseEntity.ok(response);
	}

	// 특정 게시판(자유게시판, 정보게시판등등..)의 게시글 목록 조회 (공지글 제외)
	@GetMapping("/board/{boardId}")
	public ResponseEntity<?> getPostsByBoard(@PathVariable(name = "boardId") Long boardId,
											@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getPostsByBoard() Start");

		if(!BoardValidation.isValidBoardId(boardId)) {
			logger.error("PostController getPostsByBoard() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Response
		Page<PostListResponseDTO> response = null;

		try {
			response = postService.getPostsByBoard(boardId, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByBoard() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByBoard() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByBoard() Success End");
		
		return ResponseEntity.ok(response);
	}

	// 전체 공지글(공지 게시판용) API엔드포인트
	@GetMapping("/notices")
	public ResponseEntity<?> getAllNotices (@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getAllNotices() Start");

		// Response
		Page<PostListResponseDTO> response = null;

		try {
			response = postService.getAllNotices(pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getAllNotices() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getAllNotices() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getAllNotices() Success End");
		return ResponseEntity.ok(response);
	}

	@GetMapping("notices/pinned")
	public ResponseEntity<?> getTopPinnedNoticesByBoard() {

		logger.info("PostController getTopPinnedNoticesByBoard() Start");

		List<PostListResponseDTO> response = null;

		try {
			response = postService.getTop3PinnedNoticesByBoard();
		} catch (NoSuchElementException e) {
			logger.error("PostController getTopPinnedNoticesByBoard() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getTopPinnedNoticesByBoard() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getTopPinnedNoticesByBoard() Success End");
		return ResponseEntity.ok(response);

	}

	// 게시글 키워드 검색 API엔드포인트
	@GetMapping("/search")
	public ResponseEntity<?> searchPosts (@RequestParam(name = "keyword") String keyword,
			                              @PageableDefault(size = 10, sort = "createdAt" , direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController searchPosts() Start");

		if(!PostValidation.isValidString(keyword)) {

			Page<PostListResponseDTO > emptyPage = Page.empty(pageable);

			return ResponseEntity.ok(emptyPage); //빈페이지
		}
		Page<PostListResponseDTO> response = postService.searchPostsByKeyword(keyword, pageable);

		logger.info("PostController searchPosts() Success End");
		return ResponseEntity.ok(response);
	}

	// 최신순/인기순 정렬 API엔드포인트
	@GetMapping("/sort")
	public ResponseEntity<?> getSortedPosts(@RequestParam(defaultValue = "latest") String sortBy, 
			                                @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getSortedPosts() Start");

		Page<PostListResponseDTO> response = postService.getPostsSorted(sortBy, pageable);

		logger.info("PostController getSortedPosts() Success End");
		return ResponseEntity.ok(response);
	}

	// 작성자별 게시글 조회 API엔드포인트
	@GetMapping("/author/{nickname}")
	public ResponseEntity<?> getPostsByAuthor(@PathVariable(name = "nickname") String nickname,
			                                  @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {

		logger.info("PostController getPostsByAuthor() Start");

		if(!PostValidation.isValidString(nickname)) {

			Page<Pageable> emptyPage = Page.empty(pageable);

			return ResponseEntity.ok(emptyPage); //빈페이지
		}


		Page<PostListResponseDTO> response = null;

		try {
			response = postService.getPostsByAuthorNickname(nickname, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByAuthor() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByAuthor() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByAuthor() Success End");
		return ResponseEntity.ok(response);
	}

	// 조회수 증가
	@PatchMapping("/{postId}/view")
	public ResponseEntity<?> increaseViewCount(@PathVariable(name = "postId") Long postId,
											   @AuthenticationPrincipal CustomUserDetails customUserDetails,
											   HttpServletRequest request) {

		logger.info("PostController increaseViewCount() Start");

        String userIdentifier;

        if (customUserDetails != null) {
            // 로그인 유저면 userId 사용
            userIdentifier = String.valueOf(customUserDetails.getMemberId());
        } else {
            // 비로그인 유저면 IP 주소 사용
            userIdentifier = this.getClientIp(request);
        }

        try {
            postService.increaseViewCount(postId, userIdentifier);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
        }

		logger.info("PostController increaseViewCount() Success End");
		return ResponseEntity.ok().build();
	}

	// 핀 설정/해제 (관리자만 가능)
	@PatchMapping("/{postId}/pin")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<?> togglePin(@PathVariable(name = "postId") Long postId,
									   @RequestBody PinUpdateRequestDTO pinUpdateRequestDTO) {

		logger.info("PostController togglePin() Start");

		// Request
		boolean dtoPinned  = pinUpdateRequestDTO.isPinned();

		try {
			postService.togglePinPost(postId, dtoPinned);
		} catch (NoSuchElementException e) {
			logger.error("PostController togglePin() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController togglePin() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController togglePin() Success Start");
		return ResponseEntity.ok().build();
	}

	// 이미지 목록 조회 API엔드포인트
	@GetMapping("/{postId}/images")
	public ResponseEntity<?> getPostImages(@PathVariable(name = "postId") Long postId) {

		logger.info("PostController getPostImages() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController getPostImages() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		List<PostImageResponseDTO> response = null;

		try {
			response = postService.getPostImages(postId);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostImages() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostImages() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostImages() Success End");
		return ResponseEntity.ok(response);
	}

	// 이미지 정렬 순서 조정 API엔드포인트
	@PatchMapping("/{postId}/images/order")
	public ResponseEntity<?> updateImageOrder(@PathVariable Long postId,
											  @RequestBody List<ImageOrderDTO> orderList,
											  @AuthenticationPrincipal CustomUserDetails customUserDetails){

		logger.info("PostController updateImageOrder() Start");

		Long requestAuthorId = customUserDetails.getMemberId();

		
		try {
			postService.updateImageOrder(postId, orderList, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("PostController deleteImage() updateImageOrder Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController deleteImage() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController updateImageOrder() Success End");
		return ResponseEntity.ok("이미지 순서가 변경되었습니다.");
	}

	// 이미지 단건 삭제 API엔드 포인트
	// 'Delete'는 (@PathVariable, @RequestParam)으로 요청
	@DeleteMapping("/{postId}/images/{imageId}")
	public ResponseEntity<?> deleteImage(@PathVariable(name = "postId") Long postId,
										 @PathVariable(name = "imageId") Long imageId,
			                             @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController deleteImage() Start");

		Long requestAuthorId = customUserDetails.getMemberId();

		// ex) DELETE /posts/123/image?imageUrl=/images/abc.jpg
		try {
			postService.deleteSingleImage(postId, imageId, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("PostController deleteImage() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController deleteImage() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController deleteImage() Exception Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController deleteImage() Success End");
		return ResponseEntity.ok("이미지가 정상적으로 삭제되었습니다.");
	}

	// 부모 게시판으로 자식 게시판 게시글 모두 보기 API 엔드포인트
	@GetMapping("/boards/{parentBoardId}/posts")
	public ResponseEntity<?> getPostsByParentBoard(@PathVariable Long parentBoardId,
			                                                               @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getPostsByParentBoard() Start");

		Page<PostListResponseDTO> response = null;

		try {
			response = postService.getPostsByParentBoard(parentBoardId, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByParentBoard() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByParentBoard() Exception Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByParentBoard() SuccessEnd");
		return ResponseEntity.ok(response);
	}

	//*************************************************** API End ***************************************************//

}