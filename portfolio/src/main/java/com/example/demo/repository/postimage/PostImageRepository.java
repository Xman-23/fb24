package com.example.demo.repository.postimage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.postImage.PostImage;
import java.util.*;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

	// findBy[참조 필드명][참조 대상 필드명] -> findByPostPostId 
	// ManyToOne, 즉 외래키를 가진 엔티티 클래스 에서만 가능
	List<PostImage> findByPostPostId(Long postId);

}
