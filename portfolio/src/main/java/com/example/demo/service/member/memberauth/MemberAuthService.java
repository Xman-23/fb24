package com.example.demo.service.member.memberauth;

import java.time.LocalDateTime;






import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.MemberGradeLevel;
import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.dto.member.memberauth.AuthTokenResponseDTO;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) //조회 성능 향상 시키기 위해 'readOnly'
@RequiredArgsConstructor
public class MemberAuthService {

	private final JwtUtil jwtUtil;
	private final BCryptPasswordEncoder passwordEncoder;


	// 레파지토리
    private final MemberRepository memberRepository;

    //로그
    private static final Logger logger = LoggerFactory.getLogger(MemberAuthService.class);


    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    //*************************************************** Service START ***************************************************//

    // 로그인 Service
    // 'Service'클래스 자체가 'readOnly'이므로, 
    // 'DB' '생성, 수정, 삭제'가 이뤄지는곳에 '@Transactional' 어노테이션 필수!
    @Transactional 
    public AuthTokenResponseDTO login(String trimEmail, String trimPassword) {


    	logger.info("AuthService login() Start");

    	// DTO
        String dtoEmail = trimEmail;
        String dtoPassword = trimPassword;

        logger.info("AuthService login() Start loginEmail   :" + dtoEmail);

        Member member = memberRepository.findByEmail(dtoEmail)
                                        .orElseThrow(() -> 
                                        {
                                        	logger.warn("AuthService login() 로그인 - 등록된 이메일이 없습니다.   :" +  dtoEmail);
                                        	return new IllegalArgumentException("등록된 이메일이 없습니다.");
                                        });

        // DB
        String dbTrimPassword = safeTrim(member.getPassword());
        // role : Admin, User
        Role dbRole = member.getRole();

        // 첫번쨰 파라미터 암호화 되지 않은 DTO Password, 두번째 파라미터 암호화된 DB Password
        if (!passwordEncoder.matches(dtoPassword, dbTrimPassword)) {
            logger.error("로그인 실패 - 비밀번호가 맞지 않습니다.: {}", dbTrimPassword, dtoPassword);
            throw new IllegalArgumentException("로그인 정보가 유효하지 않습니다.");
        }

        // 'Subject'가 'email'이고, 역할(role) 정보를 포함한 액세스 토큰 생성
        String accessToken = jwtUtil.generateToken(dtoEmail, dbRole);
        String refreshToken  = jwtUtil.generateRefreshToken(dtoEmail);

        logger.info("AuthService login() Success End");
        return new AuthTokenResponseDTO(accessToken, refreshToken, dbRole);
    }

    // 액세스 토큰 재발급 Service
    public AuthTokenResponseDTO refreshAccessToken(String trimRefreshToken) {

    	logger.info("AuthService refreshAccessToken() Start");

        // '7일'리프레시 토큰을 이용하여, 'subject : email' 추출
        String email = jwtUtil.getEmailFromToken(trimRefreshToken);

        Member member = memberRepository.findByEmail(email)
                                        .orElseThrow(() -> 
                                        	{
                                        		logger.error("AuthService refreshAccessToken()  - 등록된 이메일이 없습니다 : {} " +  email);
                                        		return new IllegalArgumentException("등록된 이메일이 없습니다.");
                                        	});

        // DB Role : Admin, User
        Role dbRole = member.getRole();

        // 'email'을 활용하여 다시 '액세서 토큰' 생성
        String newAccessToken = jwtUtil.generateToken(email,dbRole);

        logger.info("AuthService refreshAccessToken() Success End");
        return new AuthTokenResponseDTO(newAccessToken, null, dbRole);
    }


  //*************************************************** Service END ***************************************************//

}
