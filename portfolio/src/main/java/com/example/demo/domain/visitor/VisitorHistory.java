package com.example.demo.domain.visitor;

import java.time.LocalDateTime;

import com.example.demo.domain.member.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visitor_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 로그인한 회원이면 연결, 비회원이면 null
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	// 사이트 접속 시 발급되는 비회원 UUID (비회원, 로그인 접속자 중복제거)
	@Column(name = "visitor_id", nullable = false)
	private String visitorId;

	// 접소 IP
	@Column(name = "ip_address", nullable = false, length = 45)
	private String ipAddress;

	// 접속 시간
	@Column(name = "login_time", nullable = true)
	private LocalDateTime loginTime;

	@Column(name = "user_agent", nullable = false)
	private String userAgent;

	@Column(name = "visit_time", nullable = false)
	private LocalDateTime visitTime;

}
