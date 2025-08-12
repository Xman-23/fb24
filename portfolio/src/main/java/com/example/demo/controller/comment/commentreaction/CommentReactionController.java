package com.example.demo.controller.comment.commentreaction;

import java.util.NoSuchElementException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.comment.commentreaction.CommentReactionRequestDTO;
import com.example.demo.dto.comment.commentreaction.CommentReactionResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.comment.commentreaction.CommentReactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/commentreactions")
@RequiredArgsConstructor
public class CommentReactionController {

	private final CommentReactionService commentReactionService;

	private static final Logger logger = LoggerFactory.getLogger(CommentReactionController.class);

	//*************************************************** API Start ***************************************************//

	// 댓글 반응 추가/변경/취소(삭제) API엔드포인트
	@PostMapping("/{commentId}/reaction")
	public ResponseEntity<?> reactionToComment(@PathVariable(name = "commentId") Long commentId,
			                                @AuthenticationPrincipal CustomUserDetails customUserDetails,
			                                @RequestBody @Valid CommentReactionRequestDTO commentReactionRequestDTO,
			                                BindingResult bindingResult) {

		logger.info("CommentReactionController reactionToComment() Start");

		if(bindingResult.hasErrors()) {
			logger.error("CommentReactionController reactionToComment() badRequest : 'PostReactionRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		Long memberId = customUserDetails.getMemberId();

		CommentReactionResponseDTO response = null;

		try {
			response = commentReactionService.reactionToComment(commentId, memberId, commentReactionRequestDTO);
		} catch (NoSuchElementException e) {
			logger.error("CommentReactionController reactionToComment() NoSuchElementException : {}" ,e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("CommentReactionController reactionToComment() IllegalStateException : {}" ,e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		if(response == null) {
			logger.error("CommentReactionController reactionToComment() INTERNAL_SERVER_ERROR : 서버에러");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("CommentReactionController reactionToComment() End");
		return ResponseEntity.ok(response);
	}

	//*************************************************** API End ***************************************************//
}
