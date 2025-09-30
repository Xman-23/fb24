package com.example.demo.service.notification;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentreaction.CommentReaction;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.notification.Notification;
import com.example.demo.domain.notification.notificationenums.NotificationType;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreaction.PostReaction;
import com.example.demo.dto.notification.NotificationPageResponseDTO;
import com.example.demo.dto.notification.NotificationResponseDTO;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.notification.NotificationRepository;
import com.example.demo.repository.post.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;

	// 게시글 타입 알림 (좋아요, 싫어요)
	private static final List<NotificationType> POST_TYPES = List.of(NotificationType.POST_LIKE,
			                                                         NotificationType.POST_COMMENT,
			                                                         NotificationType.POST_WARNED_DELETED);

	// 댓글 타입 알림 (게시글 댓글, 대댓글, 경고로 인한 삭제)
	private static final List<NotificationType> COMMENT_TYPES = List.of(NotificationType.COMMENT_LIKE,
			                                                            NotificationType.CHILD_COMMENT,
			                                                            NotificationType.COMMENT_WARNED_DELETED);

	// 간단 알림 개수 
	private static final int NOTIFICATION_CURRENT_COUNT = 4;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

	//*************************************************** Service START ***************************************************//

	// 알림 생성 - 게시글 좋아요
	@Override
	public void notifyPostLike(PostReaction postReaction) {

		logger.info("NotificationServiceImpl notifyPostLike() Start");

		// 알림을 받는 사람(Receiver)ID
		Long receiverMemberId = postReaction.getPost().getAuthor().getId();
		// 알림을 받는 사람의 'Member'
		Member receiverMember = memberRepository.findById(receiverMemberId)
				                                .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyPostLike() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                                });

		// 알림을 보내는 사람 ID
		Long senderMemberId = postReaction.getUserId();
		// 알림을 보내는 사람의 'Member'
		Member senderMember = memberRepository.findById(senderMemberId)
                                              .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyPostLike() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
                                              });

		// 알림을 보내는 사람ID와 받는 사람의 ID가 같지 않다면
		if(!receiverMemberId.equals(senderMemberId)) {
			// 알림을 받는사람의 게시글 알림 설정여부가 'true'라면
			if(receiverMember.getNotificationSetting().isPostNotificationEnabled()) {
	
				Notification notification = Notification.builder()
						                                .receiver(receiverMember)
						                                .notificationType(NotificationType.POST_LIKE)
						                                .senderNickname(senderMember.getNickname())
						                                .notificationMessage("님이 회원님의 게시글을 좋아합니다.")
						                                .postId(postReaction.getPost().getPostId())
						                                .build();
				notificationRepository.save(notification);
			}
		}
		logger.info("NotificationServiceImpl notifyPostLike() End");
	}

	// 알림 생성 - 게시글 댓글 작성
	@Override
	public void notifyPostComment(Comment comment) {

		logger.info("NotificationServiceImpl notifyPostComment() Start");

		// 댓글, 대댓글 모두 포함
		Long receiverMemberId = comment.getPost().getAuthor().getId();
		// 알림을 받는 사람의 'Member'
		Member receiverMember = memberRepository.findById(receiverMemberId)
				                                .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyPostComment() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                                });
	
		Long senderMemberId = comment.getMember().getId();
		Member senderMember = memberRepository.findById(senderMemberId)
                                              .orElseThrow(() -> {
                                            	  logger.error("NotificationServiceImpl notifyPostComment() NoSuchElementException : 회원이 존재하지 않습니다.");
                                            	  return new NoSuchElementException("회원이 존재하지 않습니다.");
                                              });

		if(!receiverMemberId.equals(senderMemberId) ) {
			if(receiverMember.getNotificationSetting().isPostNotificationEnabled()) {
				Notification notification = Notification.builder()
						                                .receiver(receiverMember)
						                                .notificationType(NotificationType.POST_COMMENT)
						                                .senderNickname(senderMember.getNickname())
						                                .notificationMessage("님이 회원님의 게시글에 댓글을 작성하였습니다.")
						                                .postId(comment.getPost().getPostId())
						                                .commentId(comment.getCommentId())
						                                .build();
				notificationRepository.save(notification);
			}
		}
		logger.info("NotificationServiceImpl notifyPostComment() End");
	}

	// 알림 생성 - 게시글 경고 삭제 
	@Override
	public void notifyPostWarned(Post post) {
		logger.info("NotificationServiceImpl notifyPostWarned() Start");

		// 알림을 받을 게시글 작성자
		Long receiverId = post.getAuthor().getId();
		Member receiverMember = memberRepository.findById(receiverId)
				                                .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyPostWarned() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                                });

		if(receiverMember.getNotificationSetting().isPostNotificationEnabled()) {
			Notification notification = Notification.builder()
					                                .receiver(receiverMember)
					                                .notificationType(NotificationType.POST_WARNED_DELETED)
					                                .senderNickname("관리자")
					                                .notificationMessage("회원님의 게시글이 경고 누적으로 삭제되었습니다.")
					                                .postId(post.getPostId())
					                                .build();
			notificationRepository.save(notification);
			
		}
		logger.info("NotificationServiceImpl notifyPostWarned() End");
	}

	// 알림 생성 - 댓글 좋아요
	@Override
	public void notifyCommentLike(CommentReaction commentReaction) {

		logger.info("NotificationServiceImpl notifyCommentLike() Start");

		Long receiverMemberId = commentReaction.getComment().getMember().getId();
		// 알림을 받는 사람의 'Member'
		Member receiverMember = memberRepository.findById(receiverMemberId)
				                                .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyCommentLike() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                                });

		Long senderMemberId  = commentReaction.getUserId();
		Member senderMember = memberRepository.findById(senderMemberId)
                .							  orElseThrow(() -> {
              	  							  	logger.error("NotificationServiceImpl notifyCommentLike() NoSuchElementException : 회원이 존재하지 않습니다.");
              	  							  	return new NoSuchElementException("회원이 존재하지 않습니다.");
                });

		if(!receiverMemberId.equals(senderMemberId)) {
			if(receiverMember.getNotificationSetting().isCommentNotificationEnabled()) {
				Notification notification = Notification.builder()
						                                .receiver(receiverMember)
						                                .notificationType(NotificationType.COMMENT_LIKE)
						                                .senderNickname(senderMember.getNickname())
						                                .notificationMessage("님이 회원님의 댓글을 좋아합니다.")
						                                // '좋아요'를 누른 '댓글'이 작성된 '게시글ID' 가져오기
						                                .postId(commentReaction.getComment().getPost().getPostId())
						                                // '좋아요'를 누른 '댓글ID' 가져오기
						                                .commentId(commentReaction.getComment().getCommentId())
						                                .build();
				notificationRepository.save(notification);
			}
		}
		logger.info("NotificationServiceImpl notifyCommentLike() End");
	}

	// 알림 생성 - 대댓글
	@Override
	public void notifyChildComment(Comment childComment) {

		logger.info("NotificationServiceImpl notifyChildComment() Start");

		/* 
		   'childComment'는 대댓글(자식 댓글)이므로 반드시 parentComment가 존재해야 한다.
		    parentComment가 존재하지 않는다면, 이는 일반 댓글(부모 댓글)이므로
		    '내 댓글에 누군가가 단 대댓글' 이 아니므로 알림을 보낼 필요가 없다.
		*/
		if(childComment.getParentComment().getCommentId() == null) {
			// 메서드종료
			return;
		}

		Long receiverMemberId = childComment.getParentComment().getMember().getId();
		// '대댓글이 달린 내 댓글(부모댓글)'을 작성한 회원
		Member receiverMember = memberRepository.findById(receiverMemberId)
				                                .orElseThrow(() -> {
				                                	logger.error("NotificationServiceImpl notifyChildComment() NoSuchElementException : 회원이 존재하지 않습니다.");
				                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                                });
		// 대댓글(자식댓글)을 작성한 회원ID
		Long senderMemberId = childComment.getMember().getId();
		Member senderMember = memberRepository.findById(senderMemberId)
                							  .orElseThrow(() -> {
                								  logger.error("NotificationServiceImpl notifyChildComment() NoSuchElementException : 회원이 존재하지 않습니다.");
                								  return new NoSuchElementException("회원이 존재하지 않습니다.");
                							  });

		if(!receiverMemberId.equals(senderMemberId) ) {
			if(receiverMember.getNotificationSetting().isCommentNotificationEnabled()) {
				Notification notification = Notification.builder()
						                                .receiver(receiverMember)
						                                .notificationType(NotificationType.CHILD_COMMENT)
						                                .senderNickname(senderMember.getNickname())
						                                .notificationMessage("님이 회원님의 댓글에 답글을 달았습니다.")
						                                // '대댓글이 달린 내 댓글'이 '어떤 게시글'에 작성했는지 추적하기 위해서 필요
						                                .postId(childComment.getPost().getPostId())
						                                // '내 댓글(부모)'에 달린 대댓글이므로, 알림 클릭 시 부모 댓글(내 댓글) 위치로 이동하기 위해
						                                //  부모 댓글(내 댓글)의 ID를 commentId로 설정
						                                .commentId(childComment.getParentComment().getCommentId())
						                                .build();
				notificationRepository.save(notification);
			}
		}
		logger.info("NotificationServiceImpl notifyChildComment() End");
	}

	// 알림 생성 - 댓글 경고 삭제
	@Override
	public void notifyCommentWarned(Comment comment) {

		logger.info("NotificationServiceImpl notifyCommentWarned() Start");

		Long receiverId = comment.getMember().getId();
		Member receiverMember = memberRepository.findById(receiverId)
				                        .orElseThrow(() -> {
		                                	logger.error("NotificationServiceImpl notifyCommentWarned() NoSuchElementException : 회원이 존재하지 않습니다.");
		                                	return new NoSuchElementException("회원이 존재하지 않습니다.");
				                        });

		if(receiverMember.getNotificationSetting().isCommentNotificationEnabled()) {
			Notification notification = Notification.builder()
					                                .receiver(receiverMember)
					                                .notificationType(NotificationType.COMMENT_WARNED_DELETED)
					                                .senderNickname("관리자")
					                                .notificationMessage("회원님의 댓글이 경고 누적으로 삭제되었습니다.")
					                                .postId(comment.getPost().getPostId())
					                                .commentId(comment.getCommentId())
					                                .build();
			notificationRepository.save(notification);
			
		}
		logger.info("NotificationServiceImpl notifyCommentWarned() End");
	}

	// 게시글 최신 알림 4건
	@Override
	@Transactional(readOnly = true)
	public List<NotificationResponseDTO> getRecentPostNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl getRecentPostNotifications() Start");
		// size =4
		Pageable pageable = PageRequest.of(0, NOTIFICATION_CURRENT_COUNT);

		// Object[0] = Notification, Object[1] boardId
		List<Object[]> postTopNotifications = notificationRepository.findTopPostNotifications(receiverId, POST_TYPES, pageable);

		

		List<NotificationResponseDTO> result = postTopNotifications.stream()
				                                                   .map(obj -> {
																		// Object- > Notification(다운 캐스팅)
																	    Notification notification = (Notification) obj[0];
																	    // Object -> Long (다운 캐스팅)
																	    Long boardId = (Long) obj[1];
																	    return NotificationResponseDTO.fromDto(notification, boardId);
																	  })
				                                                   .collect(Collectors.toList());

		logger.info("NotificationServiceImpl getRecentPostNotifications() End");
		return result;
	}

	// 댓글 최신 알림 4건
	@Override
	@Transactional(readOnly = true)
	public List<NotificationResponseDTO> getRecentCommentNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl getRecentCommentNotifications() Start");
		// size = 4
		Pageable pageable = PageRequest.of(0, NOTIFICATION_CURRENT_COUNT);

		// Object[0] = Notification, Object[1] boardId
		List<Object[]> commentTopNotifications = notificationRepository.findTopCommentNotifications(receiverId, COMMENT_TYPES, pageable);

		List<NotificationResponseDTO> result = commentTopNotifications.stream()
																	  .map(obj -> {
																			// Object- > Notification(다운 캐스팅)
																		    Notification notification = (Notification) obj[0];
																		    // Object -> Long (다운 캐스팅)
																		    Long boardId = (Long) obj[1];
																		    return NotificationResponseDTO.fromDto(notification, boardId);
																		  })
																	  .collect(Collectors.toList());
		logger.info("NotificationServiceImpl getRecentCommentNotifications() End");
		return result;
	}

	// 게시글 알림 페이지
	@Override
	@Transactional(readOnly = true)
	public NotificationPageResponseDTO getPostNotifications(Long receiverId, Pageable pageable) {
		logger.info("NotificationServiceImpl getPostNotifications() Start");

		// size = 10
		// Object[0] = Notification, Object[1] boardId
		Page<Object[]> postPageNotifications = notificationRepository.findAllPostNotifications(receiverId, POST_TYPES, pageable);
	
		Page<NotificationResponseDTO> postPageNotificationsDto = postPageNotifications.map(obj -> {
																									// Object- > Notification(다운 캐스팅)
																								    Notification notification = (Notification) obj[0];
																								    // Object -> Long (다운 캐스팅)
																								    Long boardId = (Long) obj[1];
																								    return NotificationResponseDTO.fromDto(notification, boardId);
																								  });

		NotificationPageResponseDTO result = NotificationPageResponseDTO.fromPage(postPageNotificationsDto);
		logger.info("NotificationServiceImpl getPostNotifications() End");
		return result;
	}

	// 댓글 알림 페이지
	@Override
	@Transactional(readOnly = true)
	public NotificationPageResponseDTO getCommentNotifications(Long receiverId, Pageable pageable) {
		logger.info("NotificationServiceImpl getCommentNotifications() Start");
		// size = 10
		Page<Object[]> commentPageNotifications = notificationRepository.findAllCommentNotifications(receiverId, COMMENT_TYPES, pageable);
	
		Page<NotificationResponseDTO> postPageNotificationsDto = commentPageNotifications.map(obj -> {
																										// Object- > Notification(다운 캐스팅)
																									    Notification notification = (Notification) obj[0];
																									    // Object -> Long (다운 캐스팅)
																									    Long boardId = (Long) obj[1];
																									    return NotificationResponseDTO.fromDto(notification, boardId);
																									  });

		NotificationPageResponseDTO result = NotificationPageResponseDTO.fromPage(postPageNotificationsDto);
		logger.info("NotificationServiceImpl getCommentNotifications() End");
		return result;
	}

	// 게시글 + 댓글 총 알림개수
	@Override
	@Transactional(readOnly = true)
	public long countAllNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl countAllNotifications() Start");
		long result = notificationRepository.countAllByReceiver(receiverId);
		logger.info("NotificationServiceImpl countAllNotifications() End");
		return result;
	}

	// 게시글 알림 총 개수
	@Override
	@Transactional(readOnly = true)
	public long countPostNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl countPostNotifications() Start");
		long result = notificationRepository.countPostNotifications(receiverId, POST_TYPES);
		logger.info("NotificationServiceImpl countPostNotifications() End");
		return result;
	}

	// 읽지 않은 게시글 알림 총 개수
	@Override
	@Transactional(readOnly = true)
	public long countUnreadPostNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl countUnreadPostNotifications() Start");
		long result = notificationRepository.countUnreadPostNotifications(receiverId, POST_TYPES);
		logger.info("NotificationServiceImpl countUnreadPostNotifications() result: {}", result);
		logger.info("NotificationServiceImpl countUnreadPostNotifications() End");
		return result;
	}

	// 댓글 알림 총 개수
	@Override
	@Transactional(readOnly = true)
	public long countCommentNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl countCommentNotifications() Start");
		long result = notificationRepository.countCommentNotifications(receiverId, COMMENT_TYPES);
		logger.info("NotificationServiceImpl countCommentNotifications() End");
		return result;
	}
	
	// 읽지 않은 댓글 알림 총 개수
	@Override
	@Transactional(readOnly = true)
	public long countUnreadCommentNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl countCommentNotifications() Start");
		long result = notificationRepository.countUnreadCommentNotifications(receiverId, COMMENT_TYPES);
		logger.info("NotificationServiceImpl countUnreadCommentNotifications() result: {}", result);
		logger.info("NotificationServiceImpl countCommentNotifications() End");
		return result;
	}

	// 알림 모두 삭제
	@Override
	public void softDeleteAllNotifications(Long receiverId) {
		logger.info("NotificationServiceImpl softDeleteAllNotifications() Start");
		notificationRepository.softDeleteAllByReceiver(receiverId);
		logger.info("NotificationServiceImpl softDeleteAllNotifications() End");
	}

	// 알림 단건 논리적삭제
	@Override
	public int softDeleteNotification(Long receiverId, Long notificationId) {
		logger.info("NotificationServiceImpl softDeleteNotification() Start");
		// 알림 읽음 처리된 'UPDATE' 단건 행(한줄) 반환
		int result = notificationRepository.softDeleteByReceiverAndId(receiverId, notificationId);
		logger.info("NotificationServiceImpl softDeleteNotification() End");
		return result;
	}

	// 알림 모두 읽음 처리
	@Override
	public void markAllAsRead(Long receiverId) {
		logger.info("NotificationServiceImpl markAllAsRead() Start");
		notificationRepository.markAllAsRead(receiverId);
		logger.info("NotificationServiceImpl markAllAsRead() End");
	}

	// 알림 단건 읽음 처리
	@Override
	public void markAsRead(Long notificationId, Long receiverId) {
		logger.info("NotificationServiceImpl markAsRead() Start");
		notificationRepository.markAsRead(notificationId, receiverId);
		logger.info("NotificationServiceImpl notifyCommentWarned() End");
	}

	//*************************************************** Service End ***************************************************//

}
