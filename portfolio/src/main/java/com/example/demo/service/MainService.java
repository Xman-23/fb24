package com.example.demo.service;

import org.springframework.data.domain.Pageable;

import com.example.demo.dto.MainPostPageResponseDTO;

public interface MainService {

	MainPostPageResponseDTO getMainPopularPosts(Pageable pageable);

}
