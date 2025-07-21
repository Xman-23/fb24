package com.example.demo.domain.refreshtoken;

/*
	Entity : refresh_tokens

	Table : refresh_tokens

	Column
	id					(PK)
	token				(리프레시토큰)
	email				(이메일(ID))
	expiryDate			(토큰 만료기간)
*/

import java.time.LocalDateTime;

import jakarta.persistence.Column;

/*
	Entity : member

	Table : member

	Column
	id				(PK)
	token			(토큰)
	email			(이메일(id))
	expiryDate		(토큰 만료 시간)
*/

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity //테이블,속성 생성과 매핑을  위한 어노테이션
@Table(name = "refresh_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token", nullable = false, unique = true)
	private String token;

	// 같은 이메일로 토큰이 '계속' 재발급 되므로, 'unique' 설정 'X'
	@Column(name = "email", nullable =false)
	private String email;

	@Column(nullable = false)
	private LocalDateTime expiryDate;

}
