package com.example.demo.repository.post.postimage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postimage.PostImage;

import java.util.*;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

	// findBy[참조 필드명][참조 대상 필드명] -> findByPostPostId 
	// ManyToOne, 즉 외래키를 가진 엔티티 클래스 에서만 가능
	List<PostImage> findByPostPostId(Long postId);

	// 대표이미지 가져오기 ASC(오름차순) 첫번쨰
	@Query(value = 
			"SELECT pi.image_url "
		  + "  FROM post_image pi "
		  + " WHERE pi.post_id = :post_id "
		  + " ORDER BY pi.order_num ASC "
		  + " LIMIT 1",
		  nativeQuery = true)
	Optional<String> findTopImageUrlByPost(@Param("post_id") Long postId);

}
