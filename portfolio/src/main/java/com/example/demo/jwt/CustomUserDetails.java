package com.example.demo.jwt;

import java.util.Collection;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.domain.member.Member;

public class CustomUserDetails implements UserDetails {

	private final Member member;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetails.class);

	public CustomUserDetails(Member member) {
		this.member = member;
	}

	//  getAuthorities() - 멤버 Role(enum)을 SimpleGrantedAuthority로 감싸서 권한 리스트 반환
	//  스프링 시큐리티는 "ROLE_XXX" 형식 권한을 인식하므로 member.getRole().name()이 "ROLE_ADMIN" 같은 값이어야 함
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		String role = member.getRole().name(); 
		logger.info("CustomUserDetails getAuthorities() 호출 role   :" + role );
		// 멤버의 role 문자열로 변환 후 SimpleGrantedAuthority로 감싸서 Collections.singletonList로 반환
		return Collections.singletonList(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		return this.member.getPassword();
	}

	@Override
	public String getUsername() {
		return this.member.getUsername();
	}

	public Member getMember() {
		return this.member;
	}

	public String getEmail() {
		return this.member.getEmail();
	}

	public String getNickname() {
		return this.member.getNickname();
	}

}
