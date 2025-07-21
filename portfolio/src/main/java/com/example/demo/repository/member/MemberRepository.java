package com.example.demo.repository.member;

import java.util.Optional;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.member.Member;

//JpaRepository 상속받아 간단한 CRUD, 페이징 처리 가능
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	// 이메일 중복검사를 위한 추상메소드
	boolean existsByEmail(String email);

	// 닉네임 중복 검사를 위한 추상메소드
	boolean existsByNickname(String nickname);

	// 사용자 이름 중복검사를 위한 추상 메소드	
	boolean existsByUsername(String username);

	// 로그인을 위한 이메일 조회 추상메소드
	Optional<Member> findByEmail(String email);

	// 이메일(ID) 찾기 위한 추상메소드
	List<Member> findByUsername(String username);

	// 회원정보 변경을 위한 추상메소드
	Optional<Member> findByNickname(String nickname);

}
