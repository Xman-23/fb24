package com.example.demo.controller;

import org.slf4j.Logger;


import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MainPostPageResponseDTO;
import com.example.demo.service.MainService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {

	private final MainService mainService;

	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	@GetMapping("/popular-posts")
    public ResponseEntity<?> getMainPopularPosts(@PageableDefault(size = 10) Pageable pageable) {

    	logger.info("MainController getMainPopularPosts() Start");
    	
    	MainPostPageResponseDTO response = mainService.getMainPopularPosts(pageable);

        logger.info("MainController getMainPopularPosts() End");        
        return ResponseEntity.ok(response);
    }
}
