package com.example.demo.domain.member;


import com.example.demo.domain.comment.Comment;

import com.example.demo.domain.member.memberenums.MemberStatus;
import com.example.demo.domain.member.memberenums.MemberGradeLevel;
import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.domain.member.membernotificationsettings.MemberNotificationSetting;
import com.example.demo.domain.notification.Notification;
import com.example.demo.domain.post.Post;
import com.example.demo.domain.visitor.VisitorHistory;

import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Getter
@Setter
@Builder
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

	// 회원의 계정상태('ACTIVE(정상)', 'DORMANT(휴면 회원), SUSPENDED(정지 회원), WITHDRAWN (탈퇴 회원)'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private MemberStatus status = MemberStatus.ACTIVE;

    // 회원 등급(등급명), 추후에 nullable = false;로 바꿔야됨
    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = true)
    private MemberGradeLevel memberGradeLevel; 

    // 회원 등급 점수, 추후에 nullable = false;로 바꿔야됨
    @Column(name = "grade_score", nullable = true)
    private Integer gradeScore = 0;

	/* 알림 설정과 1:1 양방향 매핑
	   mappedBy = "member"는 MemberNotificationSetting 엔티티의 'member' 필드와 연결됨을 의미
	   cascade = CascadeType.ALL 로 Member 저장/삭제 시 알림 설정도 같이 처리
	   fetch = FetchType.LAZY 로 실제 사용 시점에 조회해 성능 최적화
	   optional = false 는 알림 설정이 반드시 존재해야 함을 나타냄
	*/
	@OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch =  FetchType.LAZY, optional = false )
	private MemberNotificationSetting notificationSetting;

	// 알림 양방향 매핑 ('NullException' 방지를 위한 ArrayList 객체 생성)
	@OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Notification> notifications = new ArrayList<>(); 

	// 게시글 양방향 매핑 ('NullException' 방지를 위한 ArrayList 객체 생성)
	@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Post> posts = new ArrayList<>();

	// 댓글 양방향 매핑 ('NullException' 방지를 위한 ArrayList 객체 생성)
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	// 로그인 기록 양방향 매핑 ('NullException' 방지를 위한 ArrayList 객체 생성)
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VisitorHistory> visitorHistories = new ArrayList<>();
	
	// 이용역관 동의 양방향 매핑 ('NullException' 방지를 위한 ArrayList 객체 생성)
	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MemberConsent> consents = new ArrayList<>();

//********************************************* 회원 등급 Start **********************************************************************
	private void updateGradeLevel() {

	    // 관리자일 경우 등급 자동 지정
	    if (this.role == Role.ROLE_ADMIN) {
	    	this.memberGradeLevel = MemberGradeLevel.관리자;
	        return;
	    }

	    // 일반 회원 점수 기반 등급
	    if (this.gradeScore >= 300000) {
	    	this.memberGradeLevel = MemberGradeLevel.왕;
	    }else if(this.gradeScore >= 250000) {
	    	this.memberGradeLevel = MemberGradeLevel.세자;
	    }else if(this.gradeScore >= 200000) {
	    	this.memberGradeLevel = MemberGradeLevel.왕자; 
	    }else if(this.gradeScore >= 175000) {
	    	this.memberGradeLevel = MemberGradeLevel.영의정;
	    }else if(this.gradeScore >= 150000) {
	    	this.memberGradeLevel = MemberGradeLevel.좌의정;
	    }else if(this.gradeScore >= 100000) {
	    	this.memberGradeLevel = MemberGradeLevel.우의정;
	    }else if(this.gradeScore >= 80000) {
	    	this.memberGradeLevel = MemberGradeLevel.좌참판;
	    }else if(this.gradeScore >= 55500) {
	    	this.memberGradeLevel = MemberGradeLevel.병조판서;
	    }else if(this.gradeScore >= 37000) {
	    	this.memberGradeLevel = MemberGradeLevel.병조정랑;
	    }else if(this.gradeScore >= 25500) {
	    	this.memberGradeLevel = MemberGradeLevel.암행어사;
	    }else if(this.gradeScore >= 10000) {
	    	this.memberGradeLevel = MemberGradeLevel.참판;
	    }else if(this.gradeScore >= 7500) {
	    	this.memberGradeLevel = MemberGradeLevel.부사;
	    }else if(this.gradeScore >= 5000) {
	    	this.memberGradeLevel = MemberGradeLevel.현감;
	    }else if(this.gradeScore >= 3000) {
	    	this.memberGradeLevel = MemberGradeLevel.참군;
	    }else if(this.gradeScore >= 2500) {
	    	this.memberGradeLevel = MemberGradeLevel.서리;
	    }else if(this.gradeScore >= 1500){
	    	this.memberGradeLevel = MemberGradeLevel.급제자;
	    }else if(this.gradeScore >= 500) {
	    	this.memberGradeLevel = MemberGradeLevel.유생;
	    }else {
	    	this.memberGradeLevel = MemberGradeLevel.백성;
	    }

	}
//********************************************* 회원 등급 End **********************************************************************

//********************************************* 게시물 등급 점수 Start **********************************************************************
	// 게시물 추가할때의 점수
	public void insertPostScore() {
	    this.gradeScore = this.gradeScore + 1;
	    this.updateGradeLevel();
	}

	// 게시물 삭제할때의 점수
	public void deletePostScore() {
		this.gradeScore = Math.max(this.gradeScore -1, 0);
		this.updateGradeLevel();
	}

	// 게시물 신고당할시 점수
	public void benPostScore() {
		this.gradeScore = Math.max(this.gradeScore -20, 0);
		this.updateGradeLevel();
	}
//********************************************* 게시물 등급 점수 End **********************************************************************

//********************************************* 댓글 등급 점수 Start **********************************************************************
	// 댓글 추가할떄의 점수
	public void addCommentScore() {
	    this.gradeScore = this.gradeScore +1;
	    this.updateGradeLevel();
	}

	// 댓글 삭제할때의 점수
	public void deleteCommentScore() {
		this.gradeScore = Math.max(this.gradeScore -1, 0);
		this.updateGradeLevel();
	}

	// 댓글 신고당할시 점수
	public void benCommentScore() {
		this.gradeScore = Math.max(this.gradeScore -15,0);
		this.updateGradeLevel();
	}
//********************************************* 댓글 등급 점수 End **********************************************************************

//********************************************* 게시물 리액션 등급 점수 Start **********************************************************************

	// 게시글 좋아요 점수 
	public void addPostLikeScore() {
	    this.gradeScore = this.gradeScore +5;
	    this.updateGradeLevel();
	}

	// 게시글 좋아요 취소, 또는 싫어요로 바꿀시 점수 복구
	public void cancelPostLikeScore() {
		this.gradeScore = Math.max(this.gradeScore -5, 0);
		this.updateGradeLevel();
	}

	// 게시글 싫어요 점수
	public void addPostDislikeScore() {
		// 0점 이하로 안 떨어지게 Math.max로 방지
	    this.gradeScore = Math.max(0, this.gradeScore - 3);
	    this.updateGradeLevel();
	}

	// 게시글 싫어요 취소 또는 좋아요로 바꿀시 점수 복구
	public void cancelPostDislikeScore() {
		this.gradeScore = this.gradeScore +3;
		this.updateGradeLevel();
	}
//********************************************* 게시물 리액션 등급 점수 End **********************************************************************

//********************************************* 댓글 리액션 등급 점수 Star **********************************************************************
	// 댓글 좋아요 점수
	public void addCommentLikeScore() {
	    this.gradeScore = this.gradeScore +3;
	    this.updateGradeLevel();
	}

	// 댓글 좋아요 취소 또는 좋아요에서 싫어요로 바꿀시 점수 복수
	public void cancelCommentLikeScore() {
		this.gradeScore = Math.max(this.gradeScore-3, 0);
		this.updateGradeLevel();
	}

	// 댓글 싫어요 점수
	public void addCommentDislikeScore() {
		this.gradeScore = Math.max(0, this.gradeScore - 2);
		this.updateGradeLevel();
	}

	// 댓글 싫어요 취소 또는 싫어요에서 좋아요로 바꿀시 점수 복수
	public void cancelCommentDislikeScore() {
		this.gradeScore = this.gradeScore +2;
		this.updateGradeLevel();
	}
//********************************************* 댓글 리액션 등급 점수 End **********************************************************************

}
