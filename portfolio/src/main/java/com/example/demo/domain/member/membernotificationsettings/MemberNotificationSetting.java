package com.example.demo.domain.member.membernotificationsettings;

import com.example.demo.domain.member.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "member_notification_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true) // 연관관계 제외
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // 연관관계 제외
public class MemberNotificationSetting {

	// 'id' 자동 증가 필드를 사용하지 않는 이유:
	// 회원 한 명당 알림 설정은 하나만 존재하며, 1:1 매핑(OneToOne)이기 때문에
	// 회원의 ID(member.id)를 그대로 기본키(PK)로 사용함.
	// 즉, 별도의 ID 없이 memberId = member.id 구조로 게시글/댓글 알림 여부를 설정할 수 있음.
	@Id
	@Column(name = "member_id") // 주요키(PK)이자, 외래키(PK)로 사용되므로, 'OneToOne' 관계에서 컬럴명을 "member_id"로 통일
	@EqualsAndHashCode.Include
	private Long memberId;

	// @MapsId: NotificationSetting은 회원(Member)과 1:1 관계이며,
	// memberId를 기본키(PK)로 사용하고, 동시에 Member 엔티티를 참조하는 외래키(FK)로 사용함.
	// 즉, memberId = member.id 이며, 별도의 자동 증가 ID는 필요 없음.
	// 예) member.id = 15인 회원이 알림 설정을 등록하면, NotificationSetting의 memberId도 15로 저장됨
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "member_id")
	private Member member;

	// 게시글 전용 알림 여부 설정 필드
	private boolean postNotificationEnabled  = true;
	
	// 댓글 전용 알림 여부 설정 필드
	private boolean commentNotificationEnabled = true;

}
