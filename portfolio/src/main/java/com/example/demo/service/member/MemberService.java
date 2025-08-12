package com.example.demo.service.member;


import java.util.*;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.MemberStatus;
import com.example.demo.domain.member.memberenums.Role;
import com.example.demo.domain.member.membernotificationsettings.MemberNotificationSetting;
import com.example.demo.dto.member.MemberInfoDTO;
import com.example.demo.dto.member.MemberUpdateRequestDTO;
import com.example.demo.dto.member.MemberVerifyFindEmailRequestDTO;
import com.example.demo.dto.member.MemberVerifyResetPasswordDTO;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;
import com.example.demo.repository.member.memberrefreshtoken.MemberRefreshTokenRepository;
import com.example.demo.security.AES256Util;
import com.example.demo.validation.string.SafeTrim;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OneToOne;

import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class MemberService {
	//내부(final), 외부(private) 데이터 변겅 불가
	private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final MemberRefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 로그
    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);

    // 생성자 주입
    //'@Autpwired'안에 'Bean'이 포함되어있어 객체 생명주기 관리 
    @Autowired
    public MemberService(MemberRepository memberRepository, 
    					 BCryptPasswordEncoder passwordEncoder, 
    					 JwtUtil jwtUtil,
    					 MemberRefreshTokenRepository refreshTokenRepository) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    //*************************************************** Service START ***************************************************//

    // 아이디 중복 검사 Service
    public boolean serviceCheckEmail(String trimEmail) {

        boolean exists = memberRepository.existsByEmail(trimEmail);
        logger.info("serviceCheckEmail existsByEmail 결과: {}", exists);

        return exists;
    }

    // 닉네임 중복 검사 Service
    public boolean serviceCheckNickname(String trimNickname) {

    	// 중복되는 닉네임이 있으면은 'false'
    	if(memberRepository.existsByNickname(trimNickname)) {
    		return false;
    	}

    	// 중복되는 것이 없으면 'true'
    	return true;
    }

    // 회원가입 Service
    @Transactional //(07.19 02:00 @Transactional 추가)
    public Member signup(Member member) {

    	// Controller의 toEntity()에서 이미 Role이 설정되어 있으므로 불필요
    	// Role role = member.getRole();

    	if(member.getRole() == null) {
    		member.setRole(Role.ROLE_USER);
    	}

    	String trimEmail = member.getEmail();
    	String trimPassword = member.getPassword();
    	String trimResidentNumber= member.getResidentNumber();

    	// 이메일 중복 유효검사 (Unique 제약)
    	if(memberRepository.existsByEmail(trimEmail)) {
    		throw new DuplicateKeyException("이미 사용중인 이메일입니다.");
    	}

    	member.setEmail(trimEmail);

    	// 비밀번호 암호화 (BCryptPasswordEncoder)
        String encodedPassword = passwordEncoder.encode(trimPassword);
        // 암호화된 비밀번호를 MemberEntity의 'password'에 setting
        member.setPassword(encodedPassword);

        // 주민번호 암호화 (AES256Util)
        try {
        	// 암호화된 주민번호를 MemberEntity의 'ssn'에 setting
        	String encryptedSsnNumber = AES256Util.encrypt(trimResidentNumber);
        	// memberRepository로 DB직접 접근 후 Insert(save)
        	member.setResidentNumber(encryptedSsnNumber);
		} catch (Exception e) {
			throw new RuntimeException("주민번호 암호화 실패 ", e);
		}

         
        MemberNotificationSetting setting = MemberNotificationSetting.builder()
        		                                                        .member(member) //관계 설정
        		                                                        .postNotificationEnabled(true)
        		                                                        .commentNotificationEnabled(true)
        		                                                        .build();

        // '@OneToOne'의 'optional = false' 설정에 의해서 먼저 'Member'엔티티에 Setting 후, 
        member.setNotificationSetting(setting);

        // 회원 상태 'ACTIVE'로 초기화 셋팅
        member.setStatus(MemberStatus.ACTIVE);

        // 레파지토리(JPA) 'save' -> DB 접근 
        return memberRepository.save(member);
    }

    // 이메일(ID) 찾기 Service
    public String findEmail(MemberVerifyFindEmailRequestDTO verifyFindEmailRequestDto) {

    	//DTO
    	String trimDtoUserName = verifyFindEmailRequestDto.getUsername();
    	String trimDtoResidentNumber  = verifyFindEmailRequestDto.getResidentNumber(); 

    	// 'List'로 받을 수 밖에 없는 이유, 'email'을 파라미터로 사용하는경우 'email'은 'Unique' 하기때문에, 단 한번만 가져오기때문에 'Optimal<Member>' 또는 'Member'로 단 한건만 가져온다.
    	// 하지만, 'username' 즉 사용자 이름인 경우에는 같은 이름을 가진 사용자가 어려명 일 수 있므로, 그 여러명의 'Member Entity'를 담기위해서 List<Member>로 받아야한다.
    	List<Member> memberList = memberRepository.findByUsername(trimDtoUserName);
    	
    	
    	for(Member member : memberList) {

    		try{

    			//DB
    			String memberUserName = member.getUsername().trim();
    			String memberResidentNumber = member.getResidentNumber().trim(); 

    			// 'memberRepository'가 DB에 접근하여 주민번호를 가져올 경우 주민번호는 암호화 처리가 되어있어,
    			// 암호화된 주민번호를 복호화 진행.
    			String decryptResidentNumber = AES256Util.decrypt(memberResidentNumber);
    			//만약 DB의 유저이름과 복호화된 주민번호, 그리고 'Requset'로 온 주민번호와 이름이 맞다면(equals)
    			if(memberUserName.equals(trimDtoUserName) && decryptResidentNumber.equals(trimDtoResidentNumber)) {
    				// 임시 JWT 토큰 발급 (dtoUserN
    				return jwtUtil.createTempToken(trimDtoUserName, trimDtoResidentNumber);
    			}
    		}catch(Exception e){
    			throw new RuntimeException("주민번호 복호화 실패 ", e);
    		}
    	}

    	// 'for-each문'을 통해 요청(Request)으로 온 주민번호와 DB의 회원 주민번호가 맞지 않는다면
    	// 잘못된 요청(Request) 파라미터가 온 것임으로 'IllegalArgumentException' 예외를 던짐
    	throw new IllegalArgumentException("일치하는 회원이 없습니다.");
    }

    // 비밀번호 변경 임시 토큰 발급 Service
    public String createResetPasswordToken(MemberVerifyResetPasswordDTO verifyResetPasswordDto) {

    	//DTO
    	String dtoEmail = verifyResetPasswordDto.getEmail().trim();
    	String dtoUserName = verifyResetPasswordDto.getUsername().trim();
    	String dtoResidentNumber = verifyResetPasswordDto.getResidentNumber().trim();
    	Member member = memberRepository.findByEmail(dtoEmail)
    			                        .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));
    	//DB
    	String memberEmail = member.getEmail().trim();
    	String memberUserName = member.getUsername().trim();
    	String memberResidentNumber = member.getResidentNumber().trim();

    	try {
			String decryptedResidentNumber = AES256Util.decrypt(memberResidentNumber);
			// 이메일, 사용자 이름, 주민번호가 맞다면은 
			if(memberEmail.equals(dtoEmail)) {
				if(memberUserName.equals(dtoUserName)) {
					if(decryptedResidentNumber.equals(dtoResidentNumber)) {
						// 비밀번호를 재설정 하기 위한 임시 토큰 발급
						return jwtUtil.createResetPasswordToken(dtoEmail, dtoUserName, dtoResidentNumber); 
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("주민번호 복호화 실패", e);
		}

    	 throw new IllegalArgumentException("일치하는 회원이 없습니다.");

    }

    // 패스워드 변경 Service
    @Transactional //(07.19 02:00 @Transactional 추가)
    public void resetPasswordByEmail(String emailClaims, String newPassword) {

    	Member member = memberRepository.findByEmail(emailClaims)
    			                        .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));
    	String trimNewPassword = newPassword.trim();
    	// DB
    	String memberCurrnetPassword = member.getPassword().trim();
    	// 첫번째 파라미터 : 암호화 되지 않은 새로운 패스워드, 두번째 파라미터 : DB에 저장된 현재 암호화된 패스워드
    	if(passwordEncoder.matches(trimNewPassword, memberCurrnetPassword)) {
    		throw new IllegalArgumentException("기존 비밀번호와 동일합니다.");
    	}
    	String encodedPassword = passwordEncoder.encode(trimNewPassword);
    	member.setPassword(encodedPassword);
    	memberRepository.save(member);

    }

    // 회원정보 조회 Service
    public MemberInfoDTO getMyInfo(String email){
    	//이메일이 존재할시 해당 이메일의 모든 Member Entity 반환 (비밀번호, 이름, 주민번호, 전화번호 등등..)
    	String trimEmail = email.trim();
    	Member member = memberRepository.findByEmail(trimEmail)
    	                                .orElseThrow(() -> {
    	                                	logger.error("MemberService getMyInfo() IllegalArgumentException : 회원이 존재하지 않습니다.");
    	                                	return new IllegalArgumentException("회원이 존재하지 않습니다.");
    	                                });
    	String decryptedResidentNumber = "";

    	//DB
    	String memberResidentNumber = member.getResidentNumber().trim();

    	try {
			decryptedResidentNumber = AES256Util.decrypt(memberResidentNumber);
		} catch (Exception e) {
			//해당 회원에게 보여줄 주민번호 복호화 실패 예외 처리.
			throw e;
		}
    	//'email'에 해당하는 'MemberEntity'를 -> MemberInfoDto 변환 후 DTO필드에 정보를 담는다.
    	MemberInfoDTO memberInfoDto = new MemberInfoDTO(member.getUsername(),
    													member.getEmail(),
    													member.getPhoneNumber(),
    													decryptedResidentNumber,
    													member.getNickname(),
    													member.getAddress());
    	return memberInfoDto;
    }

    // 회원 정보 변경 Service
    @Transactional //(07.19 02:00 @Transactional 추가)
    public void updateMember(String trimUserName, MemberUpdateRequestDTO dto) {

    	// // DB에서 닉네임으로 회원 조회(findByNickname), 조회된 Member 객체는 영속 상태가 됨
    	Member member = memberRepository.findByNickname(trimUserName)
    			                        .orElseThrow(() -> {
    			                        	
    			                        	return new EntityNotFoundException("회원을 찾을 수 없습니다.");
    			                        });

    	// 알림 설정 변경 여부 ('Member'에 명시된 'MemberNotificationSetting'도 자동적으로 영속성상태)
    	MemberNotificationSetting memberNotificationSetting = member.getNotificationSetting();

    	// DB업데이트 여부 변수
    	boolean dbSetting = false;

    	//DTO
    	String dtoNickName = dto.getNickname();
    	String dtoPhoneNumber = dto.getPhoneNumber();
    	String dtoAddress = dto.getAddress();
    	Boolean dtoPostNotificationEnable = dto.getPostNotificationEnabled();
    	Boolean dtoCommentNotificationEnabled = dto.getCommentNotificationEnabled();

    	//DB
    	String dbNickName = member.getNickname();
    	String dbPhoneNumber = member.getPhoneNumber();
    	String dbAddress = member.getAddress();
    	boolean dbPostNotificationEnabled = memberNotificationSetting.isPostNotificationEnabled();
    	boolean dbCommentNotificationEnabled = memberNotificationSetting.isCommentNotificationEnabled();

    	// 닉네임 수정(DTO와 DB가 다를시에만)
    	if(!dtoNickName.isEmpty() && !dtoNickName.equals(dbNickName)) {
    		member.setNickname(dtoNickName);
    		dbSetting = true;
    	}

    	// 핸드폰 번호 수정((DTO와 DB가 다를시에만))
    	if(!dtoPhoneNumber.isEmpty() && !dtoPhoneNumber.equals(dbPhoneNumber))  {
    		member.setPhoneNumber(dtoPhoneNumber);
    		dbSetting = true;
    	}

    	// 주소 수정((DTO와 DB가 다를시에만)
    	if(!dtoAddress.isEmpty() && !dtoAddress.equals(dbAddress)) {
    		member.setAddress(dtoAddress);
    		dbSetting = true;
    	}

    	// 'Request'로 받은 게시글 알림 설정 여부가 'null'이 아니라면,
    	if(dtoPostNotificationEnable != null) {
    		// 기존 'Member'에 셋팅된 게시글 설정여부와, 'Request'로 받은 게시글 설정여부가 다를경우에만 셋팅
    		if(!dtoPostNotificationEnable.equals(dbPostNotificationEnabled)) {
    			// 'MemberNotificationSetting'가 'Member'에 의해 영속성 상태이므로, 다른 값이 셋팅 될 경우 자동 'UPDATE'
    			memberNotificationSetting.setPostNotificationEnabled(dtoPostNotificationEnable);
    			dbSetting = true;
    		}
    	}

    	// 'Request'로 받은 게시글 알림 설정 여부가 'null'이 아니라면,
    	if(dtoCommentNotificationEnabled != null) {
    		// 기존 'Member'에 셋팅된 댓글 설졍여부와 , 'Request'로 받은 게시글 설정여부가 다를경우에만 셋팅
    		if(!dtoCommentNotificationEnabled.equals(dbCommentNotificationEnabled)) {
    			// 'MemberNotificationSetting'가 'Member'에 의해 영속성 상태이므로, 다른 값이 셋팅 될 경우 자동 'UPDATE'
    			memberNotificationSetting.setCommentNotificationEnabled(dtoCommentNotificationEnabled);
    			dbSetting = true;
    		}
    	}

    	if(dbSetting == false) {
			logger.error("MemberService updateMember() IllegalArgumentException : 수정이 단 한번도 이뤄지지 않음.");
			throw new IllegalArgumentException("잘못된 접근 입니다.");
    	}

    	try {
    		// 'Member'가 영속성 상태지만 명시적으로 호출 
			memberRepository.save(member);
		} catch (Exception e) {
			// DB 제약조건 위반
			logger.error("회원 정보 변경 DB제약조건 위반으로 인해 save 실패");
			throw new DataIntegrityViolationException("회원 정보 변경 DB제약조건 위반으로 인해 save 실패", e);
		}
    }

    // 회원 탈퇴 Service
    @Transactional //(07.19 02:00 @Transactional 추가)
    public void deleteMemberByEmail(String trimEmail) {

    	Member member = memberRepository.findByEmail(trimEmail)
    	                                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));
    	memberRepository.delete(member);
    }

    //*************************************************** Service END ***************************************************//
}
