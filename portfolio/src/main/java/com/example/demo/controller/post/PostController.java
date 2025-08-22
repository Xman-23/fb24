package com.example.demo.controller.post;


import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
import org.springframework.web.util.UriUtils;

import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.dto.post.PostNoticeBoardResponseDTO;
import com.example.demo.dto.post.PostParentBoardPostPageResponseDTO;
import com.example.demo.dto.post.PostPinUpdateRequestDTO;
import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.dto.comment.commentreport.CommentReportRequestDTO;
import com.example.demo.dto.post.PostCreateRequestDTO;
import com.example.demo.dto.post.PostDeleteRequestDTO;
import com.example.demo.dto.post.PostListResponseDTO;
import com.example.demo.dto.post.PostPageResponseDTO;
import com.example.demo.dto.post.PostResponseDTO;
import com.example.demo.dto.post.PostUpdateRequestDTO;
import com.example.demo.dto.post.postimage.ImageOrderDTO;
import com.example.demo.dto.post.postimage.PostImageResponseDTO;
import com.example.demo.dto.post.postreport.PostReportRequestDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.post.PostService;
import com.example.demo.service.post.PostServiceImpl;
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
    /**테스트 완료*/
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
			logger.error("PostController createPost() Error : 'PostCreateRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// Request
		PostCreateRequestDTO requestDTO = postCreateRequestDTO;

		// '공지게시글'은 '관리자'만 작성 가능
		if(requestDTO.getBoardId().equals(PostServiceImpl.NOTICE_BOARD_ID)) {
			if(customUserDetails.getRole() != Role.ROLE_ADMIN) {
				logger.error("PostController createPost Error : 공지게시글은 관리자만 작성 가능합니다.");
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("공지게시글은 관리자만 작성 가능합니다.");
			}
		}

		Long requestAuthorId = customUserDetails.getMemberId();
		String requestUserNickName = customUserDetails.getNickname();
		

		PostResponseDTO response = null;

		try {
			response  = postService.createPost(requestDTO, requestAuthorId, requestUserNickName);
		} catch (NoSuchElementException e) {
			logger.error("PostController createPost() NoSuchElementException Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("PostController createPost() IllegalStateException : {}",e.getMessage(),e );
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("PostController createPost() IllegalArgumentException : {}",e.getMessage(),e );
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (RuntimeException e) {
			logger.error("PostController createPost() RuntimeException : {}",e.getMessage(),e );
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		if(response == null) {
			logger.error("PostController createPost() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController createPost() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시글 수정 API엔드포인트
	/**테스트 완료*/
	@PatchMapping("/{postId}")
	public ResponseEntity<?> updatePost(@PathVariable(name = "postId") Long postId,
										@ModelAttribute@Valid PostUpdateRequestDTO postUpdateRequestDTO,
										BindingResult bindingResult,
										@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController updatePost() Start");


		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController updatePost() BAD_REQUEST Error : 입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		if(bindingResult.hasErrors()) {
			logger.error("PostController updatePost() BAD_REQUEST Error : 입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestPostId = postId;
		Long requestAuthorId = customUserDetails.getMemberId();
		String requestUserNickName = customUserDetails.getNickname();
		PostUpdateRequestDTO requestDTO = postUpdateRequestDTO;
		
		// Response
		PostResponseDTO response = null;

		try {
			response = postService.updatePost(requestPostId, requestDTO, requestAuthorId, requestUserNickName);
		} catch (SecurityException e) {
			logger.error("PostController updatePost() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController updatePost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController updatePost() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		if(response == null) {
			logger.error("PostController updatePost() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController updatePost() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시글 삭제 API엔드포인트
	/**테스트 완료*/
	@DeleteMapping("/{postId}")
	public ResponseEntity<?> deletePost(@PathVariable(name = "postId") Long postId,
										@RequestBody PostDeleteRequestDTO postDeleteRequestDTO,
										@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController deletePost() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("PostController deletePost() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Request
		Long requestPostId = postId;
		Long requestAuthorId = customUserDetails.getMemberId();
		boolean requestIsDeleteImages = postDeleteRequestDTO.isDeleteImages();

		try {
			postService.deletePost(requestPostId, requestAuthorId, requestIsDeleteImages);
		} catch (SecurityException e) {
			logger.error("PostController deletePost() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController deletePost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (RuntimeException e) {
			logger.error("PostController deletePost() RuntimeException Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController deletePost() Success End");
		// 반환할 데이터가 없고, 성공만 알리면 되는 경우,
		// HTTP 200 OK 상태를 보내되, 응답 본문(body)은 비워서(noContent()) 반환한다
		return ResponseEntity.noContent().build();
	}

	// 게시글 신고 API엔드포인트
	/**
	 * 신고 API의 HTTP 메서드 선택 기준
	 *
	 * 1. 단순 컬럼 변경만 수행하는 경우
	 *    - 예: reportCount 값을 단순히 1 증가시키는 경우
	 *    - 이때는 리소스 일부를 수정하는 것이므로 PATCH가 적절하다.
	 *
	 * 2. 여러 도메인(=여러 service) 로직이 복합적으로 수행되는 경우
	 * 		reportCount 증가 + 상태(HIDDEN) 변경(= 댓글 신고 서비스)
	 * 		알림 생성 + 신고 이력 저장 등(= 댓글 알림 생성 서비스)
	 *    - => reportCount 증가 + 상태(HIDDEN) 변경 + 알림 생성 + 신고 이력 저장 등
	 *    - 이러한 복합 행위는 단순 필드 수정이 아니라, '신고'라는 새로운 행위를 생성하는 것에 해당함.
	 *    - 따라서 POST 메서드를 사용하는 것이 RESTful 설계에 부합한다.
	 *
	 * 결론:
	 * - 단순 수정만 있다면 PATCH,
	 * - 복합적인 행위(도메인 이벤트 발생 등)를 동반한다면 POST를 사용.
	 */
	@PostMapping("/{postId}/report")
	public ResponseEntity<?> reportPost(@PathVariable(name = "postId") Long postId,
			                            @RequestBody@Valid PostReportRequestDTO postReportRequestDTO,
			                            BindingResult bindingResult,
			                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("PostController reportPost() Start");

		if(bindingResult.hasErrors()) {
			logger.error("PostController reportPost() Error : 'PostReportRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		String reason = postReportRequestDTO == null ? null : postReportRequestDTO.getReason();
		Long requestReporterId = customUserDetails.getMemberId();

		String response = null;

		try {
			response = postService.reportPost(postId, requestReporterId, reason);
		} catch (NoSuchElementException e) {
			logger.error("PostController reportPost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("PostController reportPost() IllegalStateException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		logger.info("PostController reportPost() End");
		return ResponseEntity.ok(response);
	}

	// 이미지 목록 조회 API엔드포인트
	/**테스트 완료*/
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

		if(response == null) {
			logger.error("PostController getPostImages() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostImages() Success End");
		return ResponseEntity.ok(response);
	}

	// 이미지 정렬 순서 조정 API엔드포인트
	/**테스트 완료*/
	@PatchMapping("/{postId}/images/order")
	public ResponseEntity<?> updateImageOrder(@PathVariable(name = "postId") Long postId,
											  @RequestBody List<ImageOrderDTO> orderList,
											  @AuthenticationPrincipal CustomUserDetails customUserDetails){

		logger.info("PostController updateImageOrder() Start");

		Long requestAuthorId = customUserDetails.getMemberId();

		List<PostImageResponseDTO> response = null;
		
		try {
			response = postService.updateImageOrder(postId, orderList, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("PostController deleteImage() updateImageOrder : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController deleteImage() Exception : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController updateImageOrder() Success End");
		return ResponseEntity.ok(response);
	}

	// 이미지 단건 삭제 API엔드 포인트
	/**테스트 완료*/
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
			logger.error("PostController deleteImage() SecurityException : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("PostController deleteImage() NoSuchElementException : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (RuntimeException e) {
			logger.error("PostController deleteImage() RuntimeException : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController deleteImage() Success End");
		return ResponseEntity.ok("이미지가 정상적으로 삭제되었습니다.");
	}

	// 이미지 모두 삭제 API엔드포인트
	/**테스트 완료*/
	@DeleteMapping("/{postId}/images")
	public ResponseEntity<?> deleteAllImages(@PathVariable(name = "postId") Long postId,
	                                         @AuthenticationPrincipal CustomUserDetails customUserDetails) {
	    logger.info("PostController deleteAllImages() Start");

	    try {
	        postService.deleteAllImages(postId, customUserDetails.getMemberId());
	    } catch (SecurityException e) {
	        logger.error("PostController deleteAllImages() SecurityException : {}", e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
	    } catch (NoSuchElementException e) {
	        logger.error("PostController deleteAllImages() NoSuchElementException: {}", e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	    } catch (RuntimeException e) {
	        logger.error("PostController deleteAllImages() RuntimeException : {}", e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
	    }

	    logger.info("PostController deleteAllImages() Success End");
	    return ResponseEntity.ok("모든 이미지 삭제 완료");
	}

	// 조회수 증가
	/**테스트 완료*/
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
	/**테스트 완료*/
	@PatchMapping("/{postId}/pin")
	// 관리자 권한만 가능
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<?> togglePin(@PathVariable(name = "postId") Long postId,
									   @RequestBody PostPinUpdateRequestDTO pinUpdateRequestDTO,
									   @AuthenticationPrincipal CustomUserDetails customUserDetails) {

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

	// 핀 설정된 공지사항 3개 최신순으로 조회
	/**테스트 완료*/
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

		if(response == null) {
			logger.error("PostController getTopPinnedNoticesByBoard() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getTopPinnedNoticesByBoard() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시글 단건 조회 API엔드포인트
	/**테스트 완료*/
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

		if(response == null) {
			logger.error("PostController getPost() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPost() Success End");
		return ResponseEntity.ok(response);
	}

	// 자식 게시판 게시글 목록 조회 (공지글 제외)
	/**테스트 완료*/
	@GetMapping("/board/{boardId}")
	public ResponseEntity<?> getPostsByBoard(@PathVariable(name = "boardId") Long boardId,
											 @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getPostsByBoard() Start");

		if(!BoardValidation.isValidBoardId(boardId)) {
			logger.error("PostController getPostsByBoard() BAD_REQUEST Error : " + "입력값이 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// Response
		PostPageResponseDTO response = null;

		try {
			response = postService.getPostsByBoard(boardId, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByBoard() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByBoard() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}


		if(response == null) {
			logger.error("PostController getPostsByBoard() INTERNAL_SERVER_ERROR : 서버에러");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByBoard() Success End");
		return ResponseEntity.ok(response);
	}

	// 자식게시판 정렬
	/**테스트 완료*/
	@GetMapping("/boards/{boardId}/posts/sorted")
	public ResponseEntity<?> getPostsByBoardSorted(@PathVariable(name = "boardId") Long boardId,
												   @RequestParam(name = "sortBy", defaultValue = "latest")String sortBy,
												   @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {

		logger.info("PostController getPostsByBoardSorted() Start");

		if(!BoardValidation.isValidBoardId(boardId)) {
			logger.error("PostController getPostsByBoard() BAD_REQUEST Error : 입력값이 유효하지 않습니다." );
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		PostPageResponseDTO response = null;

	    try {
	    	response = postService.getPostsByBoardSorted(boardId, sortBy, pageable);
	    } catch (NoSuchElementException e) {
	        logger.error("PostController getPostsByBoardSorted() NotFound: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	    } catch (Exception e) {
	        logger.error("PostController getPostsByBoardSorted() Error: {}", e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
	    }

		if(response == null) {
			logger.error("PostController getPostsByBoardSorted() INTERNAL_SERVER_ERROR : 서버에러");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

	    logger.info("PostController getPostsByBoardSorted() End");
	    return ResponseEntity.ok(response);
	}

	// 전체 공지글(공지 게시판용) API엔드포인트
	/**테스트 완료*/
	@GetMapping("/notices")
	public ResponseEntity<?> getAllNotices (@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		logger.info("PostController getAllNotices() Start");

		// Response
		PostNoticeBoardResponseDTO response = null;

		try {
			response = postService.getAllNotices(pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getAllNotices() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getAllNotices() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		if(response == null) {
			logger.error("PostController getAllNotices() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getAllNotices() Success End");
		return ResponseEntity.ok(response);
	}

	// 게시글 키워드 검색 API엔드포인트
	/**테스트 완료*/
	@GetMapping("/search")
	public ResponseEntity<?> searchPosts (@RequestParam(name = "keyword") String keyword,
			                              @PageableDefault(size = 10) Pageable pageable) {

		logger.info("PostController searchPosts() Start");

		logger.info("keywordUT : {}", keyword);
		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String keywordUTF8 = UriUtils.decode(keyword, StandardCharsets.UTF_8);

		logger.info("keywordUTF8 : {}", keywordUTF8);

		if(!PostValidation.isValidString(keywordUTF8)) {
			logger.warn("PostServiceImpl searchPostsByKeyword() : 'keyword'가 유효하지 않습니다.");
			return ResponseEntity.ok(PostPageResponseDTO.fromPage(Collections.emptyList(), Page.empty(pageable))); //빈페이지
		}

		MainPostPageResponseDTO response = postService.searchPostsByKeyword(keywordUTF8, pageable);

		if(response == null) {
			logger.error("PostController getAllNotices() INTERNAL_SERVER_ERROR : 서버에러");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController searchPosts() Success End");
		return ResponseEntity.ok(response);
	}

	// 작성자별 게시글 조회 API엔드포인트
	/**테스트 완료*/
	@GetMapping("/author/{nickname}")
	public ResponseEntity<?> getPostsByAuthor(@PathVariable(name = "nickname") String nickname,
			                                  @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {

		logger.info("PostController getPostsByAuthor() Start");

		// 한글 깨짐 방지를 위한 UTF_8 인코더
		String nicknameUTF8 = UriUtils.decode(nickname, StandardCharsets.UTF_8);

		if(!PostValidation.isValidString(nicknameUTF8)) {
			logger.warn("PostServiceImpl getPostsByAuthor() : 'nickname'이 유효하지 않습니다.");
			return ResponseEntity.ok(PostPageResponseDTO.fromPage(Collections.emptyList(), Page.empty(pageable))); //빈페이지
		}

		MainPostPageResponseDTO response = null;

		try {
			response = postService.getPostsByAuthorNickname(nicknameUTF8, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByAuthor() NoSuchElementException  : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByAuthor() Exception Error : {}","서버 에러",e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		if(response == null) {
			logger.error("PostController getPostsByAuthor() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByAuthor() Success End");
		return ResponseEntity.ok(response);
	}

	// 부모 게시판으로 자식 게시판 게시글 모두 보기 API 엔드포인트
	/**테스트 완료*/
	@GetMapping("/boards/{parentBoardId}/posts")
	public ResponseEntity<?> getPostsByParentBoard(@PathVariable(name = "parentBoardId") Long parentBoardId,
			                                       @PageableDefault(size = 10) Pageable pageable) {

		logger.info("PostController getPostsByParentBoard() Start");

		PostParentBoardPostPageResponseDTO response = null;

		try {
			response = postService.getPostsByParentBoard(parentBoardId, pageable);
		} catch (NoSuchElementException e) {
			logger.error("PostController getPostsByParentBoard() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			logger.error("PostController getPostsByParentBoard() Exception Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		if(response == null) {
			logger.error("PostController getPostsByParentBoard() INTERNAL_SERVER_ERROR : {}");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostController getPostsByParentBoard() SuccessEnd");
		return ResponseEntity.ok(response);
	}

	//*************************************************** API End ***************************************************//

}