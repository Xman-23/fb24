package com.example.demo.controller.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.notification.NotificationPageResponseDTO;
import com.example.demo.dto.notification.NotificationResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	//*************************************************** API START ***************************************************//

	// 게시글 최신 알림 4개 조회
	@GetMapping("/posts/recent")
	public ResponseEntity<?> getRecentPostNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController getRecentPostNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		List<NotificationResponseDTO> response = notificationService.getRecentPostNotifications(memberId);

		if(response == null) {
			logger.error("NotificationController getRecentPostNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController getRecentPostNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 댓글 최신 알림 4개 조회
	@GetMapping("/comments/recent")
	public ResponseEntity<?> getRecentCommentNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController getRecentCommentNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		List<NotificationResponseDTO> response = notificationService.getRecentCommentNotifications(memberId);

		if(response == null) {
			logger.error("NotificationController getRecentCommentNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController getRecentCommentNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 게시글 알림 전체 조회
	@GetMapping("/posts")
	public ResponseEntity<?> getPostNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails,
			                                      @PageableDefault(size = 10) Pageable pageable) {

		logger.info("NotificationController getPostNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		NotificationPageResponseDTO response = notificationService.getPostNotifications(memberId, pageable);

		if(response == null) {
			logger.error("NotificationController getPostNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController getPostNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 댓글 알림 전체 조회
	@GetMapping("/comments")
	public ResponseEntity<?> getCommentNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails,
            										 @PageableDefault(size = 10) Pageable pageable) {

		logger.info("NotificationController getCommentNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		NotificationPageResponseDTO response = notificationService.getCommentNotifications(memberId, pageable);

		if(response == null) {
			logger.error("NotificationController getCommentNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController getCommentNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 전체 알림 개수 조회
	@GetMapping("/count/all")
	public ResponseEntity<?> countAllNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController countAllNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		Long response = notificationService.countAllNotifications(memberId);

		if(response == null) {
			logger.error("NotificationController countAllNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController countAllNotifications() End");
		return ResponseEntity.ok(response);
		
	}

	// 게시글 알림 개수 조회
	@GetMapping("/count/posts")
	public ResponseEntity<?> countPostNotifications (@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController countPostNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		Long response = notificationService.countPostNotifications(memberId);

		if(response == null) {
			logger.error("NotificationController countPostNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원이 존재하지 않습니다.");
		}

		logger.info("NotificationController countPostNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 댓글 알림 개수 조회
	@GetMapping("/count/comments")
	public ResponseEntity<?> countCommentNotifications (@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController countCommentNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		Long response = notificationService.countCommentNotifications(memberId);

		if(response == null) {
			logger.error("NotificationController countCommentNotifications() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController countCommentNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 전체 알림 삭제 (논리적 삭제)
	@DeleteMapping("/delete/all")
	public ResponseEntity<?> softDeleteAllNotifications (@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController softDeleteAllNotifications() Start");

		Long memberId = customUserDetails.getMemberId();

		notificationService.softDeleteAllNotifications(memberId);

		logger.info("NotificationController softDeleteAllNotifications() End");
		return ResponseEntity.noContent().build();
	}

	// 단건 알림 삭제 (논리적 삭제)
	@DeleteMapping("/{notificationId}")
	public ResponseEntity<?> softDeleteNotification(@PathVariable("notificationId") Long notificationId,
			                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController softDeleteNotification() Start");

		Long memberId = customUserDetails.getMemberId();

		int response = notificationService.softDeleteNotification(memberId, notificationId);

		if(response != 1) {
			logger.error("NotificationController softDeleteNotification() INTERNAL_SERVER_ERROR : response = null");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("NotificationController softDeleteNotification() End");
		return ResponseEntity.noContent().build();
	}

	// 전체 알림 읽음 처리
	@PatchMapping("/read/all")
	public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController markAllAsRead() Start");

		Long memberId = customUserDetails.getMemberId();

		notificationService.markAllAsRead(memberId);

		logger.info("NotificationController markAllAsRead() End");
		return ResponseEntity.noContent().build();
	}

	// 단건 알림 읽음 처리
	@PatchMapping("/read/{notificationId}")
	public ResponseEntity<?> markAsRead(@PathVariable("notificationId") Long notificationId,
			                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController markAsRead() Start");

		Long memberId = customUserDetails.getMemberId();

		notificationService.markAsRead(notificationId, memberId);

		logger.info("NotificationController markAsRead() End");
		return ResponseEntity.noContent().build();
	}

	//*************************************************** API END ***************************************************//

}
