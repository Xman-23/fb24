package com.example.demo.domain.member;

import java.time.LocalDateTime;

import com.example.demo.domain.member.memberenums.MemberConsentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "member_consent")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 어떤 회원의 동의인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 동의 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private MemberConsentType consentType;

    // 동의 여부 (true/false)
    @Column(name = "consent_status",nullable = false)
    private boolean consentStatus;

    // 동의한 날짜/시간
    @Column(name = "consent_date", nullable = false)
    private LocalDateTime consentDate;

    // 동의 당시의 IP
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
