package com.example.demo.service.comment.commentnotification;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentnotification.CommentNotification;
import com.example.demo.domain.comment.commentnotification.commentnotificationenums.CommentNotificationType;
import com.example.demo.domain.member.Member;
import com.example.demo.dto.comment.commentnotification.CommentNotificationListResponseDTO;
import com.example.demo.dto.comment.commentnotification.CommentNotificationResponseDTO;
import com.example.demo.repository.comment.CommentRepository;
import com.example.demo.repository.comment.commentnotification.CommentNotificationRepository;
import com.example.demo.repository.member.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

	private final CommentNotificationRepository notificationRepository;
	private final MemberRepository memberRepository;
	private final CommentRepository commentRepository;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    //*************************************************** Service START ***************************************************//

	// 댓글 알림 목록 조회(페이징)
	@Override
	public CommentNotificationListResponseDTO getNotifications(Long memberId, Pageable pageable) {

		logger.info("NotificationServiceImpl getNotifications() Start");

		Member member = memberRepository.findById(memberId)
				                        .orElseThrow(() -> {
				                        	logger.error("NotificationServiceImpl getNotifications NoSuchElementException : 회원이 존재하지 않습니다.");
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		Page<CommentNotification> notificationsPage = notificationRepository.findByReceiver(member, pageable);
		//											  Page<Notification>-> getContent() = List<Notification> -> stream() = Stream<Notification>
		List<CommentNotificationResponseDTO> notifications = notificationsPage.getContent().stream()
				                                                                    .map(CommentNotificationResponseDTO :: toDto)
				                                                                    .collect(Collectors.toList());
		

		logger.info("NotificationServiceImpl getNotifications() End");
		return CommentNotificationListResponseDTO.builder()
				                          .notifications(notifications) // 페이지에 포함될 알림 목록(데이터)
				                          .pageNumber(notificationsPage.getNumber()) // 현재 페이지 번호
				                          .pageSize(notificationsPage.getSize()) // 한 페이지당 보여줄 데이터 사이즈
				                          .totalElements(notificationsPage.getTotalElements()) // 총 알림 데이터
				                          .totalPages(notificationsPage.getTotalPages()) // 전체 페이지 수
				                          .build();
				                          
	}

	// 댓글 알림 읽음 처리
	@Override
	public void markAsRead(Long notificationId, Long memberId) {

		logger.info("NotificationServiceImpl markAsRead() Start");

		Member member = memberRepository.findById(memberId)
				                        .orElseThrow(() -> {
				                        	logger.error("NotificationServiceImpl markAsRead() NoSuchElementException : 회원이 존재하지 않습니다.");
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		CommentNotification notification = notificationRepository.findByNotificationIdAndReceiver(notificationId, member)
		                                                  .orElseThrow(() -> {
		                                                	  logger.error("NotificationServiceImpl markAsRead() SecurityException : 권한이 없습니다.");
		                                                	  return new SecurityException("권한이 없습니다.");
		                                                  });

		// 알림 읽음 처리
		if(!notification.isRead()) {
			notification.setRead(true);
			notificationRepository.save(notification);
		}

		logger.info("NotificationServiceImpl markAsRead() End");
	}

	// 읽지 않은 댓글 알림 개수 조회
	@Override
	public long countUnreadNotifications(Long memberId) {

		logger.info("NotificationServiceImpl countUnreadNotifications() Start");

		Member member = memberRepository.findById(memberId)
                                        .orElseThrow(() -> {
                                        	logger.error("NotificationServiceImpl countUnreadNotifications() NoSuchElementException : 회원이 존재하지 않습니다.");
                                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
                                        });

		logger.info("NotificationServiceImpl countUnreadNotifications() End");
		return notificationRepository.countByReceiverAndIsReadFalse(member);
	}

	@Override
	@Transactional
	public void createNotification(Long receiverId, Long commentId, CommentNotificationType type, String content) {

		logger.info("NotificationServiceImpl createNotification() Start");

		Member member = memberRepository.findById(receiverId)
				                        .orElseThrow(() -> {
				                        	logger.error("NotificationServiceImpl createNotification() NoSuchElementException : 회원이 존재하지 않습니다.");
				                        	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		Comment comment = null;

		// 'commentId'의 존재 이유 알림 댓글을 누를시 댓글이 작성된 게시글로 이동하기 위해
		if(commentId != null) {
			comment = commentRepository.findById(commentId)
					                   .orElseThrow(() -> {
					                	   logger.error("NotificationServiceImpl createNotification() NoSuchElementException : 댓글이 존재하지 않습니다.");
					                	   return new NoSuchElementException("댓글이 존재하지 않습니다.");
					                   });
		}

		CommentNotification notification = CommentNotification.builder()
				                                .receiver(member)
				                                .comment(comment)
				                                .notificationType(type)
				                                .isRead(false)
				                                .content(content)
				                                .createdAt(LocalDateTime.now())
				                                .build();
		notificationRepository.save(notification);

		logger.info("NotificationServiceImpl createNotification() End");
	}

	//*************************************************** Service End ***************************************************//

}
