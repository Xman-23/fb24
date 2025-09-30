package com.example.demo.repository.post.postviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.post.postviews.PostViews;

public interface PostViewsRepository extends JpaRepository<PostViews, Long> {

	@Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM post_views pv " +
            "WHERE pv.post_id = :postId AND pv.member_id = :memberId",
    nativeQuery = true)
	boolean existsByPostAndMember(@Param("postId") Long postId, @Param("memberId") Long memberId);
}
