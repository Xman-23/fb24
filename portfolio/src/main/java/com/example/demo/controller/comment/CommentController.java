package com.example.demo.controller.comment;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.example.demo.dto.comment.CommentPageResponseDTO;
import com.example.demo.dto.comment.CommentRequestDTO;
import com.example.demo.dto.comment.CommentResponseDTO;
import com.example.demo.dto.comment.CommentUpdateRequestDTO;
import com.example.demo.dto.comment.commentreport.CommentReportRequestDTO;
import com.example.demo.dto.comment.commentreport.CommentReportResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.comment.CommentService;
import com.example.demo.validation.comment.CommentValidation;
import com.example.demo.validation.post.PostValidation;
import com.example.demo.validation.string.WordValidation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 'Json'으로 요청, 응답
@RestController 
@RequiredArgsConstructor // 'final' 생성자 생성
@RequestMapping("/comments") // API 주소
public class CommentController {

	// private 외부 , final 내부 에서 '데이터 불변' 유지
	private final CommentService commentService;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    //*************************************************** API START ***************************************************//

	// 1. 댓글 생성 API엔드포인트
	@PostMapping
	public ResponseEntity<?> createComment(@RequestBody @Valid CommentRequestDTO commentRequestDTO,
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

		logger.info("CommentController createComment() Start");

		if(bindingResult.hasErrors()) {
			logger.error("CommentController createComment() Error : 'CommentRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		Long requestAuthorId = customUserDetails.getMemberId();

		logger.info("CommentController createComment() customUserDetails.getMemberId() : {} " , customUserDetails.getMemberId());
		logger.info("CommentController createComment() requestAuthorId : {} " , requestAuthorId);

		CommentResponseDTO response = null;

		try {
			response = commentService.createComment(commentRequestDTO, requestAuthorId);
		}  catch (NoSuchElementException e) {
			logger.error("CommentController createComment() NoSuchElementException Error : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		if(response == null) {
			logger.error("CommentController createComment() INTERNAL_SERVER_ERROR Error : 'response' = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("CommentController createComment() End");
		return ResponseEntity.ok(response);
	}

	// 2. 댓글 수정 API엔드포인트
	@PatchMapping("/{commentId}")
	public ResponseEntity<?> updateComment (@PathVariable(name = "commentId") Long commentId,
			                                @RequestBody @Valid CommentUpdateRequestDTO commentUpdateRequestDTO,
			                                BindingResult bindingResult,
			                                @AuthenticationPrincipal CustomUserDetails customUserDetails){

		logger.info("CommentController updateComment() Start");

		if(bindingResult.hasErrors()) {
			logger.error("CommentController updateComment() Error : 'CommentUpdateRequestDTO가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		if(!CommentValidation.isValidCommentId(commentId)) {
			logger.error("CommentController updateComment() IllegalArgumentException Error : 'commentId'가 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}


		Long requestAuthorId = customUserDetails.getMemberId();
		String requestNewContent = UriUtils.decode(commentUpdateRequestDTO.getContent(), StandardCharsets.UTF_8);

		CommentResponseDTO response = null;

		try {
			response = commentService.updateComment(commentId, requestNewContent, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("CommentController updateComment() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("CommentController updateComment() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("CommentController updateComment() IllegalArgumentException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		if(response == null) {
			logger.error("CommentController updateComment() INTERNAL_SERVER_ERROR Error : 'response' = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("CommentController updateComment() End");
		return ResponseEntity.ok(response);
	}

	// 3. 댓글 삭제 API엔드포인트
	@DeleteMapping("/{commentId}")
	public ResponseEntity<?> deleteComment (@PathVariable(name = "commentId") Long commentId,
											@AuthenticationPrincipal CustomUserDetails customUserDetails) {


		logger.info("CommentController deleteComment() Start");

		if(!CommentValidation.isValidCommentId(commentId)) {
			logger.error("CommentController deleteComment() IllegalArgumentException Error : {}");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		Long requestAuthorId = customUserDetails.getMemberId();

		CommentResponseDTO response = null;

		try {
			response = commentService.deleteComment(commentId, requestAuthorId);
		} catch (SecurityException e) {
			logger.error("CommentController deleteComment() SecurityException Error : {}", e.getMessage(), e);
			// 작성자 외에는 게시글을 수정할 수 없음 -> 접근권한 없음(403)
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (NoSuchElementException e) {
			logger.error("CommentController deleteComment() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		if(response == null) {
			logger.error("CommentController deleteComment() INTERNAL_SERVER_ERROR Error : 'response' = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("CommentController deleteComment() End");
		return ResponseEntity.ok(response);
	}

	// 4. 댓글 신고 API엔드포인트
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
	@PostMapping("/{commentId}/report")
	public ResponseEntity<?> reportComment(@PathVariable(name = "commentId") Long commentId,
			                               @RequestBody@Valid CommentReportRequestDTO commentReportRequestDTO,
			                               BindingResult bindingResult,
			                               @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("CommentController reportComment() Start");

		if(bindingResult.hasErrors()) {
			logger.error("CommentController reportComment() Error : 'CommentReportRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		String reason = commentReportRequestDTO == null ? null : commentReportRequestDTO.getReason().trim();

		// 신고이유가 10글자 미만, 비속어 포함되면 신고내용이 유효하지 않음.
		if(reason.length() < 10 && !WordValidation.containsForbiddenWord(reason)) {
			logger.error("CommentController reportComment() reason : 신고 내용이 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("신고 내용이 유효하지 않습니다.");
		}
		Long requestReporterId = customUserDetails.getMemberId();

		CommentReportResponseDTO response = null;

		try {
			response = commentService.reportComment(commentId, requestReporterId, reason);
		} catch (NoSuchElementException e) {
			logger.error("CommentController reportComment() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("CommentController reportComment() IllegalStateException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		logger.info("CommentController reportComment() End");
		return ResponseEntity.ok(response);
	}

	// 5. 댓글 트리구조 조회 API엔드포인트
	@GetMapping("/post/{postId}")
	public ResponseEntity<?> getCommentsTreeByPost (@PathVariable(name = "postId") Long postId,
			                                        @RequestParam(value = "sortBy", defaultValue = "normal") String sortBy,
			                                        @PageableDefault(size = 10) Pageable pageable) {

		logger.info("CommentController getCommentsTreeByPost() Start");

		if(!PostValidation.isPostId(postId)) {
			logger.error("CommentController getCommentsTreeByPost() IllegalArgumentException Error : 'postId'가 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		if(!CommentValidation.isValidSortBy(sortBy)) {
			logger.error("CommentController getCommentsTreeByPost() IllegalArgumentException Error : 'sortBy'가 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		CommentPageResponseDTO response = null;

		try {
			response = commentService.getCommentsTreeByPost(postId, sortBy,pageable);
		}  catch (NoSuchElementException e) {
			logger.error("CommentController getCommentsTreeByPost() NoSuchElementException Error : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		if(response == null) {
			logger.error("CommentController getCommentsTreeByPost() INTERNAL_SERVER_ERROR Error : 'response' = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("CommentController getCommentsTreeByPost() End");
		return ResponseEntity.ok(response);
	}

    //*************************************************** API End ***************************************************//
	
}
