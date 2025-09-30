package com.example.demo.repository.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.notification.Notification;
import com.example.demo.domain.notification.notificationenums.NotificationType;

@Repository // 'JPA'
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	// 읽지 않은 게시글 관련 알림 4개만 조회 (최신순)
	@Query(
		    "SELECT n, p.board.boardId " +
		    " FROM Notification n " +
		    " JOIN Post p ON n.postId = p.postId " +
		    " WHERE n.receiver.id = :receiverId " +
		    "   AND n.notificationType IN (:postTypes) " +
		    "   AND n.deleted = false " +
		    "   AND n.read = false " +
		    " ORDER BY n.createdAt DESC"
		)
	List<Object[]> findTopPostNotifications(@Param("receiverId") Long receiverId,
	                                        @Param("postTypes") List<NotificationType> postTypes,
	                                        Pageable pageable);

	// 읽지 않은 댓글 관련 알림 4개만 조회 (최신순)
	@Query(
		    "SELECT n, p.board.boardId " +
		    " FROM Notification n " +
		    " JOIN Post p ON n.postId = p.postId " +
		    " WHERE n.receiver.id = :receiverId " +
		    "   AND n.notificationType IN (:commentTypes) " +
		    "   AND n.deleted = false " +
		    "   AND n.read = false " +
		    " ORDER BY n.createdAt DESC"
		)
	List<Object[]> findTopCommentNotifications(@Param("receiverId") Long receiverId,
	                                           @Param("commentTypes") List<NotificationType> commentTypes,
	                                           Pageable pageable);

	// 전체 알림 개수
	@Query(
			"SELECT COUNT(n) "
		  + "  FROM Notification n "
		  + " WHERE n.receiver.id = :receiverId "
		  + "   AND n.deleted = false "
		  )
	long countAllByReceiver(@Param("receiverId") Long receiverId);

	@Query(
	        "SELECT COUNT(n) "
	      + "  FROM Notification n "
	      + " WHERE n.receiver.id = :receiverId "
	      + "   AND n.notificationType IN (:postTypes) "
	      + "   AND n.deleted = false "
	)
	long countPostNotifications(@Param("receiverId") Long receiverId,
	                               @Param("postTypes") List<NotificationType> postTypes);

	@Query(
	        "SELECT COUNT(n) "
	      + "  FROM Notification n "
	      + " WHERE n.receiver.id = :receiverId "
	      + "   AND n.notificationType IN (:postTypes) "
	      + "   AND n.deleted = false "
	      + "   AND n.read = false "
	)
	long countUnreadPostNotifications(@Param("receiverId") Long receiverId,
	                                  @Param("postTypes") List<NotificationType> postTypes);
    // 댓글 타입 알림 전체 개수
    @Query(
            "SELECT COUNT(n) "
          + "  FROM Notification n "
          + " WHERE n.receiver.id = :receiverId "
          + "   AND n.notificationType IN (:commentTypes) "
          + "   AND n.deleted = false "
    )
    long countCommentNotifications(@Param("receiverId") Long receiverId,
                                    @Param("commentTypes") List<NotificationType> commentTypes);

    // 읽지 않은 댓글 알림 개수
    @Query(
            "SELECT COUNT(n) "
          + "  FROM Notification n "
          + " WHERE n.receiver.id = :receiverId "
          + "   AND n.notificationType IN (:commentTypes) "
          + "   AND n.deleted = false "
          + "   AND n.read = false "
    )
    long countUnreadCommentNotifications(@Param("receiverId") Long receiverId,
                                         @Param("commentTypes") List<NotificationType> commentTypes);
	
	
	
	//----------------------------------------------------------------------------------------------------------------------
	// 알림 전체 삭제(논리적) 
	@Modifying(clearAutomatically = true) // 'UPDATE', 'DELETE'
	@Transactional
	@Query(
			"UPDATE Notification n "
		  + "   SET n.deleted = true, "
		  + "       n.updatedAt = CURRENT_TIMESTAMP "
		  + " WHERE n.receiver.id = :receiverId "
		  )
	void softDeleteAllByReceiver(@Param("receiverId") Long receiverId);

	// 개별 알림 삭제(논리적)
	// 수신자의 알림
	@Modifying(clearAutomatically = true) // 'UPDATE', 'DELETE'
	@Transactional
	@Query(
			"UPDATE Notification n "
		  + "   SET n.deleted = true, "
		  + "       n.updatedAt = CURRENT_TIMESTAMP "
		  + " WHERE n.receiver.id = :receiverId "
		  + "   AND n.notificationId = :notificationId "
		  )
	int softDeleteByReceiverAndId(@Param("receiverId") Long receiverId,
			                      @Param("notificationId") Long notificationId);

	// 전체 읽음 처리
	@Modifying(clearAutomatically = true) // 'UPDATE', 'DELETE
	@Transactional
	@Query(
			"UPDATE Notification n "
		  + "   SET n.read = true "
	      + " WHERE n.receiver.id = :receiverId "
	      + "   AND n.read = false "
	      )
	void markAllAsRead(@Param("receiverId") Long receiverId);

	// 단건 읽음 처리
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query(
			"UPDATE Notification n "
		  + "   SET n.read = true "
		  + " WHERE n.receiver.id = :receiverId "
		  + "   AND n.notificationId = :notificationId "
		  )
	void markAsRead(@Param("notificationId") Long notificationId,
	                @Param("receiverId") Long receiverId);

	// 전체 게시글 알림 페이징 목록
	@Query(
		    "SELECT n, p.board.boardId " +
		    " FROM Notification n " +
		    " JOIN Post p ON n.postId = p.postId " +
		    " WHERE n.receiver.id = :receiverId " +
		    "   AND n.notificationType IN (:postTypes) " +
		    "   AND n.deleted = false " +
		    " ORDER BY n.createdAt DESC"
		)
		Page<Object[]> findAllPostNotifications(@Param("receiverId") Long receiverId,
		                                        @Param("postTypes") List<NotificationType> postTypes,
		                                        Pageable pageable);

	// 전체 댓글 알림 페이징 목록
	@Query(
		    "SELECT n, p.board.boardId " +
		    " FROM Notification n " +
		    " JOIN Post p ON n.postId = p.postId " +
		    " WHERE n.receiver.id = :receiverId " +
		    "   AND n.notificationType IN (:commentTypes) " +
		    "   AND n.deleted = false " +
		    " ORDER BY n.createdAt DESC"
		)
	Page<Object[]> findAllCommentNotifications(@Param("receiverId") Long receiverId,
			                                       @Param("commentTypes") List<NotificationType> commentTypes,
			                                       Pageable pageable);

}
