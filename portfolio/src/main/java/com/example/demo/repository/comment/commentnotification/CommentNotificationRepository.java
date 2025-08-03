package com.example.demo.repository.comment.commentnotification;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.comment.Comment;
import com.example.demo.domain.comment.commentnotification.CommentNotification;
import com.example.demo.domain.member.Member;

@Repository
public interface CommentNotificationRepository extends JpaRepository<CommentNotification, Long> {

	// 특정 회원(receiver)의 알림 목록 페이징 조회
	@Query( "SELECT n "
		  + "  FROM CommentNotification n "
		  + " WHERE n.receiver = :receiver "
		  + " ORDER BY n.createdAt DESC "
		  )
	Page<CommentNotification> findByReceiver(@Param("receiver")Member receiver, Pageable pageable);

	// 읽지 않은 알림 개수 조회
	long countByReceiverAndIsReadFalse(Member receiver);

	// 특정 알림 읽음 처리할 떄 알림 ID와 receiver로 조회 (권한 체크 용)
	Optional<CommentNotification> findByNotificationIdAndReceiver(Long notificationId, Member receiver);


}
