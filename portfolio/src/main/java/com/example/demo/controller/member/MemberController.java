package com.example.demo.controller.member;

import java.util.*;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.security.AES256Util;
import com.example.demo.service.member.MemberService;
import com.example.demo.validation.member.email.EmailValidation;
import com.example.demo.validation.member.password.PasswordValidation;
import com.example.demo.validation.member.phone.PhoneValidation;
import com.example.demo.validation.member.residentnumber.ResidentNumberValidation;
import com.example.demo.validation.string.WordValidation;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.memberenums.MemberConsentType;
import com.example.demo.dto.member.MemberInfoDTO;
import com.example.demo.dto.member.MemberResetPasswordDTO;
import com.example.demo.dto.member.MemberSignupDTO;
import com.example.demo.dto.member.MemberUpdateRequestDTO;
import com.example.demo.dto.member.MemberVerifyFindEmailRequestDTO;
import com.example.demo.dto.member.MemberVerifyFindEmailResponseDTO;
import com.example.demo.dto.member.MemberVerifyResetPasswordDTO;
import com.example.demo.dto.member.memberauth.AuthLoginDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.repository.member.MemberRepository;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.apache.catalina.connector.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController //'json'으로 요청,응답 처리
@RequestMapping("/members") //API(부모)주소
public class MemberController {

	// private 외부 , final 내부 에서 '데이터 불변' 유지
	private final JwtUtil jwtUtil;
	private final MemberService memberService;
	private final MemberRepository memberRepository;

	// 로그
	private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

	//'Service 의존성' 주입 (new MemberService()로 객체 생성 안해도됨)
	//'@Autpwired'안에 'Bean'이 포함되어있어 객체 생명주기 관리 
	@Autowired 
	public MemberController(MemberService memberService 
							, JwtUtil jwtUtil
							, MemberRepository memberRepository) {
		this.memberService = memberService;
		this.jwtUtil = jwtUtil;
		this.memberRepository = memberRepository;
	}

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

    //*************************************************** API START ***************************************************//

    // 아이디 조회를 위한 API엔드포인트(데이터 조회 : Get)
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam("email") String email) {

    	logger.info("MemberController checkEmail() Start");

    	String trimEmail = safeTrim(email);
    	logger.info("MemberController checkEmail() trimEmail   :" + trimEmail);
    	// 이메일 유효성 검사
    	if(!EmailValidation.isValidEmail(trimEmail)) {
    		logger.info("MemberController checkEmail() '첫번째 IF문' trimEmail   :" + trimEmail);
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일이 유효하지 않습니다.");
    	}

    	// 이메일 중복 검사
    	if(memberService.serviceCheckEmail(trimEmail)) {
    		logger.info("MemberController checkEmail() '두번쨰 IF문' trimEmail   :" + trimEmail);
    		return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다.");
    	}

    	logger.info("MemberController checkEmail() Success End");
    	return ResponseEntity.ok("사용이 가능한 이메일입니다.");
    }

	// 닉네임 조회를 위한 API엔드포인트(데이터 조회 : Get)
	@GetMapping("/check-nickname")
	public ResponseEntity<?> checkNickname(@RequestParam("nickname") String nickname) {

		logger.info("MemberController checkNickname() Start");
		String trimNickname = nickname.trim();

		if(!WordValidation.isValidNickname(trimNickname)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 닉네임입니다.");
		}

		// 중복검사 실시
		if(!memberService.serviceCheckNickname(trimNickname)) {
			//Client Error 409 CONFLICT(충돌,중복)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
		}

		logger.info("MemberController checkNickname() Success End");
		return ResponseEntity.ok("사용 가능한 닉네임입니다.");
	}

	// 회원가입 처리 API엔드포인트 (데이터 생성 : Post)
	// Json -> @RequestBody -> @Valid
	@PostMapping("/signup")
	public ResponseEntity<String> registerMember(@RequestBody@Valid MemberSignupDTO memberSignupDto,
			                                     BindingResult bindingResult,
			                                     HttpServletRequest request) {

		logger.info("MemberController registerMember() Start");

		// '@Vaild'에 의해 필드 유효성, 제약조건을 검사후 다음 코드 진행.
		// 필드가 유효하지 않을경우 'Bad Request(400)' 반환 

		// '@Vaild'에 의해 유효성 검사 결과가 BindingResult에 담김
		// 예외가 있을 경우 hasError() 메서드 'True'
		if(bindingResult.hasErrors()) {
			logger.error("PostController registerMember() Error : 'MemberSignupDTO'가 유효하지 않습니다.");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// DTO
		String trimEmail = safeTrim(memberSignupDto.getEmail());
		String trimPassword = safeTrim(memberSignupDto.getPassword());
		String trimResidentNumber = safeTrim(memberSignupDto.getResidentNumber());
		String trimPhoneNumber = safeTrim(memberSignupDto.getPhoneNumber());
		String trimNickName = safeTrim(memberSignupDto.getNickname()).trim();


		// 이메일 유효성 검사
		if(!EmailValidation.isValidEmail(trimEmail)) {
			return ResponseEntity.badRequest().body("이메일이 유효하지 않습니다.");
		}

		// 이메일 중복 검사
		if(memberService.serviceCheckEmail(trimEmail)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다.");
		}

		// 이메일과 비밀번호 동일 여부 검사
		if (trimEmail.equals(trimPassword)) {
		    return ResponseEntity.badRequest().body("이메일과 비밀번호는 동일할 수 없습니다.");
		}
		
		// 비밀번호 유효성 검사
		if(!PasswordValidation.isValidPassword(trimPassword)) {
			return ResponseEntity.badRequest().body("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
		}

		List<Member> memberList = memberRepository.findByUsername(safeTrim(memberSignupDto.getUsername()));

		for(Member member : memberList) {
			
			String memberResidentNumber = member.getResidentNumber().trim(); 

			String decryptResidentNumber = "";
			// 'memberRepository'가 DB에 접근하여 주민번호를 가져올 경우 주민번호는 암호화 처리가 되어있어,
			// 암호화된 주민번호를 복호화 진행.
			try {
				decryptResidentNumber = AES256Util.decrypt(memberResidentNumber);
			} catch (RuntimeException e) {
				logger.error("MemberService RuntimeException 발생   : {}" , e.getMessage() , e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
			}
			// 'DB'에 기존에 있는 주민번호와 요청 주민번호와 같다면은 badRequest(400)반환
			if(decryptResidentNumber.equals(trimResidentNumber)) {
				return ResponseEntity.badRequest().body("가입된 회원이 존재합니다.");
			}
		}

		// 주민번호 유효성 검사
		if(!ResidentNumberValidation.isValidResidentNumberWithChecksum(trimResidentNumber)) {
			return ResponseEntity.badRequest().body("주민번호가 유효하지 않습니다.");
		}	

		// 핸드폰 유효성 검사
		if(!PhoneValidation.isValidPhoneNumber(trimPhoneNumber)) {
			return ResponseEntity.badRequest().body("핸드폰번호가 유효하지 않습니다.");
		}

		// 휴대폰 번호 '-' 제거 후
		String removeTrimPhoneNumber = trimPhoneNumber.replaceAll("-", "").trim();
		memberSignupDto.setPhoneNumber(removeTrimPhoneNumber);

		// 비정상 닉네임일 경우 BadRequest(400) 반환
		if(!WordValidation.isValidNickname(trimNickName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("닉네임이 유효하지 않습니다.");
		}
		
		// 중복된 닉네임일 경우 409반환
		if(!memberService.serviceCheckNickname(trimNickName)) {
			//Client Error 409 CONFLICT(충돌,중복)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
		}

		memberSignupDto.setNickname(trimNickName);

        // 동의 체크 항목
        List<MemberConsentType> consentTypes = memberSignupDto.getConsents();

        // 요청 IP
        String ipAddress = request.getRemoteAddr();

		
		try {
			//DTO(toEntity()) -> Entity 변경후 -> Service 호출 
			memberService.signup(memberSignupDto.toEntity(),consentTypes,ipAddress);
		} catch (DuplicateKeyException e) {
			//이메일(Unique)제약이 걸려있으므로, 이메일 중복일시 발생하는 예외
			logger.error("MemberService DuplicateKeyException 발생   : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		}  catch (Exception e) {
			//서버 에러 500번대 : INTERNAL_SERVER_ERROR
			logger.error("MemberService RuntimeException 발생   : {}" , e.getMessage() , e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}

		logger.info("MemberController registerMember() Success End");
		//'HTTP.status'가 200번대
		return ResponseEntity.ok("회원가입 성공!");
	}

	// 1. 이메일(ID) 찾기 임시 토큰 발급 API 엔드 포인트
	// 사용자(username)와 주민번호(residentNumber)를 받아 일치시 임시 토큰 발급
	// @RequestBody(JsonBody)로 받으므로, PostMapping
	@PostMapping("/find-email")
	public ResponseEntity<?> findEmail(@RequestBody @Valid MemberVerifyFindEmailRequestDTO verifyFindEmailRequestDto,
									  BindingResult bindingResult) {

		logger.info("MemberController findEmail() Start");

		if(bindingResult.hasErrors()) {
			logger.error("MemberController findEmail() BAD_REQUEST : 입력값이 유효하지 않습니다");
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// DTO(After Trim)
		String trimUsername = safeTrim(verifyFindEmailRequestDto.getUsername());
		String trimResident = safeTrim(verifyFindEmailRequestDto.getResidentNumber());

		// DTO(After Set)
		verifyFindEmailRequestDto.setUsername(trimUsername);
		verifyFindEmailRequestDto.setResidentNumber(trimResident);

		String token = "";

		try {
				token = memberService.findEmail(verifyFindEmailRequestDto.toDto());
		} catch (IllegalArgumentException e) {
			// Service 에서 던진 'IllegalArgumentException' catch  
			logger.info("MemberController findEmail() IllegalArgumentException : {}", e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (RuntimeException e) {
			// Service 에서 던진 'RuntimeException' catch
			// 'Runtime'은 서버 실행 문제이므로 '서버 오류 처리 (INTERNAL_SERVER_ERROR(500번대))
			logger.info("MemberController findEmail() RuntimeException : {}", e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}

		logger.info("MemberController findEmail() End");
		// 예외가 발생하지 않으면 임시 토큰을 Map(Key, Value)으로 응답(Response)
		return ResponseEntity.ok(Collections.singletonMap("token", token));
	}

	// 2. 이메일(ID) 찾기 발급된 임시토큰으로 아이디 찾기 API엔드포인트
	@GetMapping("/show-email")
	public ResponseEntity<?> showEmail(@RequestHeader("Authorization") String bearerToken) {

		logger.info("MemberController showEmail() Start");

		// Authorization Header(헤더)의 Bearer 토큰명 ->(replace) 토큰명 변환
		String token = bearerToken.replace("Bearer ", "");
		if(!jwtUtil.validateToken(token)) {
			//인증 관련 실패이므로, UNAUTHORIZED(401) 메세지 리턴
			logger.error("MemberController showEmail() UNAUTHORIZED : 유효하지 않은 토큰입니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
		}

		// 임시토큰에 저장된 정보를 추출하기 위한 Claims Class
		Claims claims = jwtUtil.getClaimsFromToken(token);

		// 이메일 임시토큰에 주체 비교
	    if (!"TEMP".equals(claims.getSubject())) {
	    	logger.error("MemberController showEmail() BAD_REQUEST : 이메일 임시 토큰이 아닙니다.");
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일 임시 토큰이 아닙니다.");
	    }

	    // 임시토큰에 저장된 '사용자 이름' 추출
	    String usernameClaims = claims.get("username", String.class);
	    // 임시토큰에 저장된 '주민버노' 추출
	    String residentNumberClaims = claims.get("residentNumber", String.class);

        List<Member> members = memberRepository.findByUsername(usernameClaims);

        Member targetMember = null;

        for (Member member : members) {
        	try {
                if (AES256Util.decrypt(member.getResidentNumber()).equals(residentNumberClaims)) {
                    targetMember = member;
                    break;
                }
        	}catch (RuntimeException e) {
        		logger.error("MemberController showEmail() RuntimeException : {} ",e.getMessage(),e);
        		// 예외 발생시 다음 회원으로 넘어가 주민번호 비교
        		continue;
			}
        }

        if (targetMember == null) {
        	logger.error("MemberController showEmail() NotFound : 일치하는 회원이 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(Collections.singletonMap("error", "일치하는 회원이 없습니다."));
        }

        MemberVerifyFindEmailResponseDTO response= MemberVerifyFindEmailResponseDTO.builder()
                                                                                   .email(targetMember.getEmail())
                                                                                   .build();
        return ResponseEntity.ok(response);
	}

	@PostMapping("/reset-password-token")
	public ResponseEntity<?> findPassword(@RequestBody @Valid MemberVerifyResetPasswordDTO verifyResetPasswordDto,
															  BindingResult bindingResult) {

		logger.info("MemberController findPassword() Start");

		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		String token = "";

		try {
			
			token = memberService.createResetPasswordToken(verifyResetPasswordDto.toDto());

		}catch (IllegalArgumentException e) {
			// Service 에서 던진 'IllegalArgumentException' catch  
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (RuntimeException e) {
			// Service 에서 던진 'RuntimeException' catch
			// 'Runtime'은 서버 실행 문제이므로 '서버 오류 처리 (INTERNAL_SERVER_ERROR(500번대))
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("MemberController findPassword() End");
		return ResponseEntity.ok(Collections.singletonMap("token", token));
	}

	@PatchMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestHeader("Authorization") String bearerToken,
										   @RequestBody @Valid MemberResetPasswordDTO resetPasswordDto,
										   BindingResult bindingResult) {

		logger.info("MemberController resetPassword() Start");

		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		String token = bearerToken.replace("Bearer ", "");
		String newPassword = resetPasswordDto.getNewPassword();
		String confirmNewPassword = resetPasswordDto.getConfirmNewPassword();

		
		if (!PasswordValidation.isValidPassword(newPassword)) {
		    return ResponseEntity.badRequest().body("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
		}

		if(!jwtUtil.validateToken(token)) {
			//인증 관련 실패이므로, UNAUTHORIZED(401) 메세지 리턴
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
		}

		// 비밀번호 재설정 임시토큰에 저장된 정보를 추출하기 위한 Claims Class
		Claims claims = jwtUtil.getClaimsFromToken(token);

		// 비밀번호 임시토큰 주체 비교
		if(!"RESET".equals(claims.getSubject())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호 재설정 임시 토큰이 아닙니다.");
		}

		// 비밀번호 일치 여부 확인
		if(!newPassword.equals(confirmNewPassword)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호가 일치 하지 않습니다.");
		}

		// 임시 토큰에 들어있는 '이메일' 추출
		String emailClaims = claims.get("email", String.class);
		
		// 이메일과 비밀번호 동일 여부 검사
		if (emailClaims.equals(newPassword)) {
		    return ResponseEntity.badRequest().body("이메일과 비밀번호는 동일할 수 없습니다.");
		}

		try {
			memberService.resetPasswordByEmail(emailClaims, newPassword);
		} catch (IllegalArgumentException e) {
			logger.error("resetPasswordByEmail IllegalArgumentException : {} " + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		logger.info("MemberController resetPassword() End");
		return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
	}

	// 회원정보 API엔드포인트
	// URL,HEADER(Token)을(를) 조화하기위해 '@GetMapping'
	@GetMapping("/me")
	public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("MemberController getMyInfo() Start");
		/*
		 JwtAuthenticationFilter에서 인증 성공 시, email을 principal로 설정함
		 authentication.getPrincipal(); -> email을 가져올 수 있음

		 // 이전 코드 (email을 principal로 세팅)
		 UsernamePasswordAuthenticationToken authentication =
		     new UsernamePasswordAuthenticationToken(email, null, null);
		 String email = (String) authentication.getPrincipal(); 

		 // 변경된 코드 (userDetails를 principal로 세팅)
		 UsernamePasswordAuthenticationToken authentication =
		     new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		 String trimCurrentNickname = customUserDetails.getEmail().trim();
		*/

		String trimEmail = safeTrim(customUserDetails.getEmail());

		// DTO -> Controller
		MemberInfoDTO memberInfoDto = null;
		try {
			//email -> MemberService 던짐 -> MemberInfoDto 반환
			memberInfoDto = memberService.getMyInfo(trimEmail);
		} catch (IllegalArgumentException e) {
			logger.error("MemberController getMyInfo() IllegalArgumentException : {} ",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (RuntimeException e) {
			logger.error("MemberController getMyInfo() IllegalArgumentException : {} ",e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}

		logger.info("MemberController getMyInfo() Start");
		// Controller -> View
		return ResponseEntity.ok(memberInfoDto);
	}

	// 회원정보 수정 API엔드포인트 (데이터 일부 수정 : 'Patch')
	@PatchMapping("/me") 
	public ResponseEntity<?> updateMyInfo(@RequestBody@Valid MemberUpdateRequestDTO memberUpdateRequest,
										  BindingResult bindingResult,
										  @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("MemberController updateMyInfo() Start");

		// 'DTO'는 @RequestBody로 매핑되므로, 요청 body가 없으면 컨트롤러 진입 전 400(Bad Request) 에러 발생
		// 따라서 DTO 자체에 대한 null 체크는 불필요하며, 각 필드에 대해서만 유효성 검사를 수행하면 됨

		if(bindingResult.hasErrors()) {
			logger.error("MemberController updateMyInfo() Error : 'MemberUpdateRequestDTO'가 유효하지 않습니다.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 유효하지 않습니다.");
		}

		// 토큰 에서 닉네임 가져오기
		String trimCurrentNickname = customUserDetails.getNickname().trim();

		//DTO 설정
		String trimDtoNewNickName = safeTrim(memberUpdateRequest.getNickname());
		String trimDtoNewPhoneNumber = safeTrim(memberUpdateRequest.getPhoneNumber());
		String trimDtoNewAddress = safeTrim(memberUpdateRequest.getAddress());

		if(!trimDtoNewNickName.isEmpty()) {
			// 비정상 닉네임일 경우 BadRequest(400) 반환
			if(!WordValidation.isValidNickname(trimDtoNewNickName)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("닉네임이 유효하지 않습니다.");
			}
		}
		if(!trimDtoNewPhoneNumber.isEmpty()) {
			// 핸드폰 유효성 검사
			if(!PhoneValidation.isValidPhoneNumber(trimDtoNewPhoneNumber)) {
				return ResponseEntity.badRequest().body("핸드폰번호가 유효하지 않습니다.");
			}
		}

		// 휴대폰 번호 '-' 제거 후
		String removeTrimPhoneNumber = trimDtoNewPhoneNumber.replaceAll("-", "").trim();

		memberUpdateRequest.setNickname(trimDtoNewNickName);
		memberUpdateRequest.setPhoneNumber(removeTrimPhoneNumber);
		memberUpdateRequest.setAddress(trimDtoNewAddress);

		try {
			memberService.updateMember(trimCurrentNickname, memberUpdateRequest);
		} catch (DataIntegrityViolationException e) {
			logger.error("MemberController updateMember() DataIntegrityViolationException : {}" + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (EntityNotFoundException e) {
			logger.error("MemberController updateMember() EntityNotFoundException : {} ", e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("MemberController updateMember() IllegalArgumentException : {} ", e.getMessage(),e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		logger.info("MemberController updateMyInfo() End");
		return ResponseEntity.ok("회원정보가 수정되었습니다.");
	}
	
	// 회원 탈퇴 API엔드포인트
	@PatchMapping("/me/withdraw")
	public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		logger.info("MemberController deleteMyAccount() Start");

		String trimEmail = safeTrim(customUserDetails.getEmail());
		try {
			memberService.deleteMemberByEmail(trimEmail);
		} catch (IllegalArgumentException e) {
			logger.error("회원 탈퇴 실패: " + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		logger.info("MemberController deleteMyAccount() End");
		return ResponseEntity.ok("회원 탈퇴가 정상 처리되었습니다.");
	}

	//*************************************************** API END ***************************************************//
}
