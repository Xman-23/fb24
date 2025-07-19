package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller //'컨트롤러'임을 명시
public class HomeController {
	@GetMapping("/")// "/"로 접속할시
	@ResponseBody //문자열로 'BODY' 부분 응답
	public static String home() {
		return "Hello, Portfolio!"; 
	}
}
