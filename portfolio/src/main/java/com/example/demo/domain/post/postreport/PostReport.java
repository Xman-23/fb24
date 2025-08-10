package com.example.demo.domain.post.postreport;

import java.time.LocalDateTime;

import com.example.demo.domain.post.Post;
import com.example.demo.domain.post.postreport.postreportenums.PostReportStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReport {

    @Id
    @Column(name = "report_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    // 신고한 회원 ID (참조용, 외래키는 필요에 따라 Member 엔티티 연결 가능)
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    // 신고 대상 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 신고 사유 (optional)
    @Column(name = "reason", length = 500)
    private String reason;

    // 신고 상태 - 처리 상태 (PENDING, REVIEWED, REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostReportStatus status;

    // 신고 일시
    @Column(name = "created_at", nullable  = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PostReportStatus.PENDING;
        }
    }

}
