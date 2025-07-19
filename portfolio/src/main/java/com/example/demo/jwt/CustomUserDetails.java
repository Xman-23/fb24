package com.example.demo.jwt;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.domain.Member;

public class CustomUserDetails implements UserDetails {

	private final Member member;

	public CustomUserDetails(Member member) {
		this.member = member;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return Collections.emptyList();
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
