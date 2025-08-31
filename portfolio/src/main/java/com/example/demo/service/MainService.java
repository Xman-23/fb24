package com.example.demo.service;


import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.demo.dto.MainPostPageResponseDTO;

public interface MainService {

	// 인기 게시글 조회
	MainPostPageResponseDTO getMainPopularPosts(Pageable pageable);

	// 인기 게시글 키워드 검색
	MainPostPageResponseDTO getMainPopularPostsSearch (String keyword, Pageable pageable);

	// 인기 게시글 작성자 
	MainPostPageResponseDTO getMainPopularPostsAuthor (String nickname, Pageable pageable);

	// 인기 게시글 실시간 검색
	List<String> getMainPopularPostsAutoComplete (String keyword);

	MainPostPageResponseDTO getMainPopularPostsAutoCompleteSearch (String title, Pageable pageable);

}
