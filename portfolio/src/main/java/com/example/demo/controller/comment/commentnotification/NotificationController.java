package com.example.demo.controller.comment.commentnotification;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.comment.commentnotification.CommentNotificationListResponseDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.service.comment.commentnotification.NotificationServiceImpl;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationServiceImpl notificationServiceImpl;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	//*************************************************** API Start ***************************************************//

	// 알림 목록 조회
	@GetMapping
	public ResponseEntity<?> getNotifications(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable,
			                                  @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController getNotifications() Start");

		// Request
		Long requestMemberId = customUserDetails.getMemberId();

		CommentNotificationListResponseDTO response = null;

		try {
			response = notificationServiceImpl.getNotifications(requestMemberId, pageable);
		} catch (NoSuchElementException e) {
			logger.error("NotificationController getNotifications() NoSuchElementException : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		logger.info("NotificationController getNotifications() End");
		return ResponseEntity.ok(response);
	}

	// 알림 읽음 처리
	@PostMapping("/{notificationId}/read")
	public ResponseEntity<?> markAsRead(@PathVariable(name = "notificationId")Long notificationId,
										@AuthenticationPrincipal CustomUserDetails customUserDetails) {
	
		logger.info("NotificationController markAsRead() Start");

		// Request
		Long requestMemberId = customUserDetails.getMemberId();

		try {
			notificationServiceImpl.markAsRead(notificationId, requestMemberId);
		} catch (NoSuchElementException e) {
			logger.error("NotificationController markAsRead() NoSuchElementException : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		logger.info("NotificationController markAsRead() End");
		// '200'과 함께 아무것도 메세지 안 보냄
		return ResponseEntity.ok().build();
	}

	// 안 읽은 알림개수 조회
	@GetMapping("/unread/count")
	public ResponseEntity<?> countUnreadNotifications(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("NotificationController countUnreadNotifications() Start");

		// Request
		Long requestMemberId = customUserDetails.getMemberId();

		Long response = null;

		try {
			response = notificationServiceImpl.countUnreadNotifications(requestMemberId);
		} catch (NoSuchElementException e) {
			logger.error("NotificationController countUnreadNotifications() NoSuchElementException : {}",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}

		logger.info("NotificationController countUnreadNotifications() End");
		return ResponseEntity.ok(response);
	}

	//*************************************************** Service End ***************************************************//

}
