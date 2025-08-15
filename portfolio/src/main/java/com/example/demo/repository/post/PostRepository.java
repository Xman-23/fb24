package com.example.demo.repository.post;


import com.example.demo.domain.post.Post;

import com.example.demo.domain.post.postenums.PostStatus;
import com.example.demo.domain.board.Board;
import com.example.demo.domain.member.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	// 전체 공지글 (공지 게시판 전용)
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId = 1 "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = true "
		  + " ORDER BY p.createdAt DESC "
		  )
	Page<Post> findNoticePosts(Pageable pageable);

	// 제목 또는 내용에 키워드를 포함한 게시글 검색 (ACTIVE 상태만 검색, 대소문자 무시, 페이징 처리) 사용
	@Query( "SELECT p " 
		  + "  FROM Post p " 
		  + " WHERE p.status = 'ACTIVE' " 
		  + "   AND (" 
		  + "         LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
		  + "    OR "
		  + "         LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))" 
		  + "       )"
		  )
	Page<Post> searchByKeyword(@Param("keyword")String keyword, Pageable pageable);

	// 자식게시판 인기순 정렬
	@Query( "SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board = :board "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = false "
		  + " ORDER BY (SELECT COUNT(r)"
		  + "             FROM PostReaction r"
		  + "            WHERE r.post = p"
		  + "              AND r.reactionType = 'LIKE'"
		  + "          ) DESC, "
		  + "          p.createdAt DESC"
		  )
	Page<Post> findPopularPostsByBoard(@Param("board") Board board, Pageable pageable);

	// 자식게시판 최신순 정렬
	Page<Post> findByBoardAndStatusAndIsNoticeFalseOrderByCreatedAtDesc(Board board, 
																		PostStatus status, 
																		Pageable pageable);

	// 'ACTIVE 게시글'에서 '작성자ID(memberId)'를 검색 하여 페이징 처리
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.author = :member "
		  + "   AND p.status = :status "
		  )
	Page<Post> findByAuthorAndStatus (@Param("member") Member member, 
			                           @Param("status") PostStatus status, 
			                           Pageable pageable);
	
	// 3개의 핀으로 설정된 공지 게시글 모든 게시판에 보여주기
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board = :board "
		  + "   AND p.isPinned = true "
		  + "   AND p.isNotice = true "
		  + "   AND p.status = 'ACTIVE' "
		  + " ORDER BY p.createdAt DESC "
		  )
	List<Post> findTop3PinnedByBoard (@Param("board") Board board, Pageable pageable);

	/* 조회수 증가
	   @Modifying(clearAutomatically = true)
	   JPQL UPDATE 후 1차 캐시를 자동으로 비워줘서,
	   다음 조회 시 DB에서 최신 데이터를 가져오도록 함
	*/
	@Modifying(clearAutomatically = true)
	@Transactional
	@Query( "UPDATE Post p "
		  + "   SET p.viewCount = p.viewCount + 1 "
		  + " WHERE p.postId = :postId "
		  )
	int incrementViewCount(@Param("postId") Long postId);

	// 자식 게시판 상단 인기글 뽑기 3개 뽑기 (총 갯수를 뽑는게 아니므로 = 'List')
	//'nativeSQL'사용하므로, @PageableDefault 에서 페이징 조건(size page)만 명시, 정렬(sort, direction)은 명시해도 적용이 되지않음
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "			  SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "							 WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "						  END) AS net_like_count,"
		  + "                    SUM(CASE WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 END) AS like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "            ) AS pr_summary "
		  + "    ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id = :boardId "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= :recentThreshold "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND COALESCE(pr_summary.net_like_count, 0) >= :netLikeThreshold "
		  + " ORDER BY COALESCE(pr_summary.like_count, 0) DESC, "
		  + "          p.created_at DESC "
		  + " LIMIT :limit ", 
		  nativeQuery = true)
	List<Post> findTopPostsByBoardWithNetLikes(@Param("boardId") Long boardId,
											   @Param("likeThreshold") int likeThreshold,
											   @Param("netLikeThreshold") int netLikeThreshold,
											   @Param("recentThreshold") LocalDateTime recentThreshold,
											   @Param("limit") int limit);

	// 상단 공지 게시글 3개뽑기
	@Query(
			"SELECT p "
		  + "  FROM Post p "
		  + " WHERE p.board.boardId = 1 "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.isNotice = true "
		  + "   AND p.isPinned = true "
		  + " ORDER BY p.createdAt DESC "
		  )
	List<Post> findTop3FixedNotices(Pageable pageable);

	/* 좋아요 60일 기준으로 가중치 계산 
	 한 페이지당 받을 데이터(content)와 데이터의 총 갯수 (totalElements) 구하기
	 파라미터로 'Pageable'을 받을시에 개발자가 직접 정해준 'size'에 의해 'totlaPage' 구해짐
	 */
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "							  WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "							  ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                             ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND COALESCE(pr_summary.net_like_count, 0) >= :netLikeThreshold "
		  + " ORDER BY (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC, "
		  + "          p.created_at DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ("
		  + "              SELECT pr.post_id, "
		  + "                     SUM(CASE "
		  + "                             WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                             ELSE 0 "
		  + "                          END) AS like_count, "
		  + "                     SUM(CASE "
		  + "                              WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                              WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                              ELSE 0 "
		  + "                          END) AS net_like_count "
		  + "                FROM post_reaction pr "
		  + "               GROUP BY pr.post_id "
		  + "            ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND COALESCE(pr_summary.net_like_count, 0) >= :netLikeThreshold ",
		  nativeQuery = true)
	Page<Post> findPopularPostsByWeightedScore(@Param("childBoardIds") List<Long> childBoardIds,
			                                   @Param("likeThreshold") int likeThreshold,
			                                   @Param("netLikeThreshold") int netLikeThreshold,
			                                   @Param("dayLimit") int dayLimit,
			                                   Pageable pageable);

	/** 게시글 배치 조회
	    조건 : 수정일자 기준으로 5년 지나고, 조회수가 100이하이며, 
	          공지글이 아니며, ACTIVE상태인 게시글, 'pin'으로 고정된 글 X
	 @return : 삭제돨 게시글 수(레코드) 반환
	*/
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN comment c ON p.post_id = c.post_id "
		  + " WHERE p.updated_at <= :cutDate "
		  + "   AND p.view_count <= :maxViewCount "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.is_notice = false "
		  + "   AND p.is_pinned = false "
		  + " GROUP BY p.post_id "
		  + "HAVING COUNT(c.comment_id) = 0", nativeQuery = true)
	List<Post> findByDeadPost (@Param("cutDate")LocalDateTime cutDate,
			                   @Param("maxViewCount") int maxViewCount);

	/** 게시글 배치 삭제 
	    조건 : 수정일자 기준으로 5년 지나고, 조회수가 100이하이며, 
	          공지글이 아니며, ACTIVE상태인 게시글, pin으로 고정된 글 X
	 @return : 삭제된 게시글 수(레코드) 반환
	*/
	@Modifying
	@Query(value = 
			"DELETE FROM post "
		  + " WHERE post_id IN ( "
		  + "                   SELECT p.post_id "
		  + "                     FROM post p "
		  + "                     LEFT JOIN comment c ON p.post_id = c.post_id "
		  + "                    WHERE p.updated_at <= :cutDate "
		  + "                      AND p.view_count <= :maxViewCount "
		  + "                      AND p.status = 'ACTIVE' "
		  + "                      AND p.is_notice = false "
		  + "                      AND p.is_pinned = false "
		  + "                    GROUP BY p.post_id "
		  + "                   HAVING COUNT(c.comment_id) = 0 "
		  + "                  )", nativeQuery =  true)
	int deleteDeadPost (@Param("cutDate") LocalDateTime cutDate,
			            @Param("maxViewCount") int maxViewCount);

	/** 공지글 배치 조회
		조건 : 수정일자 기준으로 5년 지나고, 공지글(notice = true), ACTIVE인 게시글
	 @return : 삭제된 공직즐 List 반환
	*/
	@Query(value = 
			"SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN comment c ON p.post_id = c.post_id "
		  + " WHERE p.updated_at <= :cutDate "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.is_notice = true "
		  + " GROUP BY p.post_id "
		  + "HAVING COUNT(c.comment_id) = 0", nativeQuery = true)
	List<Post> findByDeadNoticePost (@Param("cutDate")LocalDateTime cutDate);

	/** 공지글 배치 삭제 
	    조건 : 수정일자 기준으로 5년 지나고, 공지글(notice = true), ACTIVE인 게시글 
	    @return : 삭제된 게시글 수(레코드) 반환
	*/
	@Modifying
	@Query(value = 
			"DELETE FROM post "
		  + " WHERE post_id IN ( "
		  + "                   SELECT p.post_id "
		  + "                     FROM post p "
		  + "                     LEFT JOIN comment c ON p.post_id = c.post_id "
		  + "                    WHERE p.updated_at <= :cutDate "
		  + "                      AND p.status = 'ACTIVE' "
		  + "                      AND p.is_notice = true "
		  + "                    GROUP BY p.post_id "
		  + "                   HAVING COUNT(c.comment_id) = 0 "
		  + "                  )", nativeQuery =  true)
	int deleteDeadNoticePost (@Param("cutDate") LocalDateTime cutDate);

	/** 
	 * 단건 게시글 집계조회 (댓글, 좋아요, 싫어요)
	 * post_id   comment_id   reaction_id   reaction_type
	 *		1         101          1001          LIKE       // 댓글 101에 대한 좋아요 반응
	 *		1         102          1001          LIKE       // 댓글 102에 대한 좋아요 반응 (같은 reaction_id인 걸로 봐서 같은 사용자 혹은 같은 반응일 수 있음)
	 *		1         103          1002          DISLIKE    // 댓글 103에 대한 싫어요 반응
	 *		1         104          1003          LIKE       // 댓글 104에 대한 좋아요 반응
	 *		1         105          NULL          NULL       // 댓글 105에 대해 반응이 없는 경우 (reaction이 NULL)
	 */

	public interface PostAggregate {
		Long getPostId(); 		// AS postId
		Long getCommentCount(); // AS commentCount
		Long getLikeCount(); 	// AS likeCount;
		Long getDislikeCount(); // AS dislikeCount;
	}

	@Query(value = 
			"SELECT p.post_id AS postId, "
		  + "      COALESCE(COUNT(DISTINCT c.comment_id), 0) AS commentCount, "
		  + "      COALESCE(SUM(CASE "
		  + "                       WHEN r.reaction_type  = 'LIKE' THEN 1 ELSE 0 "
		  + "                    END), 0) AS likeCount, "
		  + "      COALESCE(SUM(CASE "
		  + "                       WHEN r.reaction_type = 'DISLIKE' THEN 1 ELSE 0 "
		  + "                    END), 0) AS dislikeCount "
		  + "  FROM post p "
		  + "  LEFT JOIN comment c ON p.post_id = c.post_id "
		  + "  LEFT JOIN post_reaction r ON p.post_id = r.post_id "
		  + " WHERE p.post_id = :postId "
		  + " GROUP BY p.post_id ", nativeQuery = true)
	Optional<PostAggregate> findPostAggregateByPostId(@Param("postId") Long postId);

}
