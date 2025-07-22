package com.example.demo.repository.postimage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.postImage.PostImage;
import java.util.*;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

	// 특정 게시글에 첨부된 이미지 리스트 조회
	List<PostImage> findByPost(Post post);

}
