package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;

@Repository
public interface MainPostRepository extends JpaRepository<Post, Long> {

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
		  + "   AND COALESCE(pr_summary.net_like_count, 0) >= :netLikeThreshold "
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
		  + "   AND COALESCE(pr_summary.net_like_count, 0) >= :netLikeThreshold ",nativeQuery =  true) 
	Page<Post> findMainPopularPosts(@Param("childBoardIds") List<Long> childBoardIds,
									@Param("likeThreshold") int likeThreshold,
	                                @Param("netLikeThreshold") int netLikeThreshold,
	                                @Param("dayLimit") int dayLimit,
	                                Pageable pageable);
}
