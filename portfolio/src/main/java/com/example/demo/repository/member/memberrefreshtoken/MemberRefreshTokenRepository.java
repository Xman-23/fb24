package com.example.demo.repository.member.memberrefreshtoken;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.member.memberrefreshtoken.MemberRefreshToken;

@Repository
public interface MemberRefreshTokenRepository extends JpaRepository<MemberRefreshToken, Long> {

    Optional<MemberRefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByEmail(String email);
}