package com.example.demo.service;

import org.springframework.data.domain.Pageable;

import com.example.demo.dto.MainPopularPostPageResponseDTO;

public interface MainService {

	MainPopularPostPageResponseDTO getMainPopularPosts(Pageable pageable);

}
