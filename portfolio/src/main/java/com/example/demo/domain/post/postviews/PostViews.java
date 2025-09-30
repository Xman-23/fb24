package com.example.demo.domain.post.postviews;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.post.Post;

@Entity
@Table(
    name = "post_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "member_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostViews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, updatable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();
}