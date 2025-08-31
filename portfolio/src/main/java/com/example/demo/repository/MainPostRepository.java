package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postenums.PostStatus;

@Repository
public interface MainPostRepository extends JpaRepository<Post, Long> {

	// 메인 인기 게시판 게시글 (좋아요순 먼저 내림차순 한후에, 생성일자 내림차)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + " ORDER BY (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC, "
		  + "          p.created_at DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1"
		  + "                            ELSE 0"
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold ",nativeQuery =  true) 
	Page<Post> findMainPopularPosts(@Param("childBoardIds") List<Long> childBoardIds,
									@Param("likeThreshold") int likeThreshold,
	                                @Param("netLikeThreshold") int netLikeThreshold,
	                                @Param("dayLimit") int dayLimit,
	                                Pageable pageable);
	
	// 메인 인기 게시판 키워드 검색 (최신순 내림차순 정렬후, 좋아요순 정렬)
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND (" 
		  + "         LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
		  + "    OR "
		  + "         LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))" 
		  + "       )"
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1"
		  + "                            ELSE 0"
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND (" 
		  + "         LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " 
		  + "    OR "
		  + "         LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))" 
		  + "       )",
		  nativeQuery =  true) 
	Page<Post> findMainPopularPostsKeyword(@Param("childBoardIds") List<Long> childBoardIds,
									       @Param("likeThreshold") int likeThreshold,
	                                       @Param("netLikeThreshold") int netLikeThreshold,
	                                       @Param("dayLimit") int dayLimit,
	                                       @Param("keyword") String keyword,
	                                       Pageable pageable);

	// 메인 인기 게시판 작성자 검색
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.author_id = :authorId "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1"
		  + "                            ELSE 0"
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.author_id = :authorId ",
		  nativeQuery =  true) 
	Page<Post> findMainPopularPostsAuthor(@Param("childBoardIds") List<Long> childBoardIds,
									      @Param("likeThreshold") int likeThreshold,
	                                      @Param("netLikeThreshold") int netLikeThreshold,
	                                      @Param("dayLimit") int dayLimit,
	                                      @Param("authorId") Long authorId,
	                                      Pageable pageable);

	// 메인 인기 게시판 실시간 검색
	@Query(value = 
		    "SELECT p.title "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1"
		  + "                            ELSE 0"
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ",
		  nativeQuery =  true) 
	Page<String> findMainPopularPostsAutoComplete(@Param("childBoardIds") List<Long> childBoardIds,
									              @Param("likeThreshold") int likeThreshold,
	                                              @Param("netLikeThreshold") int netLikeThreshold,
	                                              @Param("dayLimit") int dayLimit,
	                                              @Param("keyword") String keyword,
	                                              Pageable pageable);
	
	// 메인 인기 게시판 실시간 검색
	@Query(value = 
		    "SELECT p.* "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1 "
		  + "                            ELSE 0 "
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.title = :title "
		  + " ORDER BY p.created_at DESC, "
		  + "          (COALESCE(pr_summary.like_count, 0) * 1.0 + GREATEST(0, :dayLimit - DATEDIFF(NOW(), p.created_at))) DESC ",
		  countQuery = 
		    "SELECT COUNT(*) "
		  + "  FROM post p "
		  + "  LEFT JOIN ( "
		  + "             SELECT pr.post_id, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 ELSE 0 "
		  + "                         END) AS like_count, "
		  + "                    SUM(CASE "
		  + "                            WHEN pr.reaction_type = 'LIKE' THEN 1 "
		  + "                            WHEN pr.reaction_type = 'DISLIKE' THEN -1"
		  + "                            ELSE 0"
		  + "                         END) AS net_like_count "
		  + "               FROM post_reaction pr "
		  + "              GROUP BY pr.post_id "
		  + "             ) AS pr_summary ON p.post_id = pr_summary.post_id "
		  + " WHERE p.board_id IN (:childBoardIds) "
		  + "   AND p.is_notice = false "
		  + "   AND p.status = 'ACTIVE' "
		  + "   AND p.created_at >= DATE_SUB(NOW(), INTERVAL :dayLimit DAY) "
		  + "   AND COALESCE(pr_summary.like_count, 0) >= :likeThreshold "
		  + "   AND pr_summary.net_like_count >= :netLikeThreshold "
		  + "   AND p.title = :title ",
		  nativeQuery =  true) 
	Page<Post> findMainPopularPostsAutoCompleteSearch(@Param("childBoardIds") List<Long> childBoardIds,
									                  @Param("likeThreshold") int likeThreshold,
	                                                  @Param("netLikeThreshold") int netLikeThreshold,
	                                                  @Param("dayLimit") int dayLimit,
	                                                  @Param("title") String keyword,
	                                                  Pageable pageable);
}
