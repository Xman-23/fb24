package com.example.demo.controller.post.postreaction;

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

import com.example.demo.controller.post.PostController;
import com.example.demo.dto.post.postreaction.PostReactionRequestDTO;
import com.example.demo.dto.post.postreaction.PostReactionResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.post.postreaction.PostReactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/postreactions")
@RequiredArgsConstructor
public class PostReactionController {

	// 서비스
	private final PostReactionService postReactionService;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(PostReactionController.class);

	//*************************************************** API Start ***************************************************//

	// 게시글 반응 추가/변경/취소(삭제) API엔드포인트
	@PostMapping("/{postId}/reaction")
	public ResponseEntity<?> reactionToPost(@PathVariable(name = "postId") Long postId,
			                                @AuthenticationPrincipal CustomUserDetails customUserDetails,
			                                @RequestBody @Valid PostReactionRequestDTO postReactionRequestDTO,
			                                BindingResult bindingResult) {

		logger.info("PostReactionController reactionToPost() Start");

		if(bindingResult.hasErrors()) {
			logger.error("PostReactionController reactionToPost() badRequest : 'PostReactionRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		Long memberId = customUserDetails.getMemberId();

		PostReactionResponseDTO response = null;

		try {
			response = postReactionService.reactionToPost(postId, memberId, postReactionRequestDTO);
		} catch (NoSuchElementException e) {
			logger.error("PostReactionController reactionToPost() : {}" ,e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		if(response == null) {
			logger.error("PostReactionController reactionToPost() INTERNAL_SERVER_ERROR : 서버에러");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("PostReactionController reactionToPost() End");
		return ResponseEntity.ok(response);
	}

	//*************************************************** API End ***************************************************//

}
