package com.example.demo.domain.member;

import com.example.demo.domain.member.memberenums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	member: Entity

	Table : member

	Column
	id					(PK)
	email				(이메일(id)
	password			(패스워드)
	username			(사용자명)
	phone_Number		(핸드폰번호)
	residentNumber		(주민번호)
	nickname			(닉네임)
	address				(주소)
	role				(역할 : 관리자 ,유저)
*/

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "member")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Member {

	@Id // 'PK' 어노테이션
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY) //'PK 자동 증가' 어노테이션
	private Long id;

	// 아이디로 사용할 이메일
	@Column(name = "email", nullable = false, unique = true, length =100)
	private String email;

	// 패스워드
	@Column(name = "password", nullable = false)
	private String password;

	// 사용자명
	@Column(name = "username", nullable = false, length = 50)
	private String username;
	
	// 핸드폰 번호
	@Column(name = "phone_number", nullable = false, length = 20)
	private String phoneNumber;

	// 암호화된 주민번호 (07.17 변경 ssn -> residentNumber)
	@Column(name = "residentnumber", nullable = false, length = 100)
	private String residentNumber;

	// 닉네임
	@Column(name = "nickname", nullable = false, unique = true, length = 20)
	private String nickname;

	// 주소
	@Column(name = "address", nullable = true, length = 255)
	private String address;

	// 역할(Admin, User)
	// length=20 넣으면 MySQL enum인데 Hibernate가 varchar로 인식해서
	// 계속 alter table 실행함. 그래서 length는 빼야함.
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Role role;

}
