package com.example.demo.controller.member;

import java.util.*;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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

import com.example.demo.repository.member.MemberRepository;
import com.example.demo.security.AES256Util;
import com.example.demo.service.member.MemberService;
import com.example.demo.validation.email.EmailValidation;
import com.example.demo.validation.nickname.NickNameValidation;
import com.example.demo.validation.password.PasswordValidation;
import com.example.demo.validation.phone.PhoneValidation;
import com.example.demo.validation.residentnumber.ResidentNumberValidation;
import com.example.demo.domain.member.Member;
import com.example.demo.dto.auth.AuthLoginDTO;
import com.example.demo.dto.member.MemberInfoDTO;
import com.example.demo.dto.member.MemberResetPasswordDTO;
import com.example.demo.dto.member.MemberSignupDTO;
import com.example.demo.dto.member.MemberUpdateRequestDTO;
import com.example.demo.dto.member.MemberVerifyFindEmailRequestDTO;
import com.example.demo.dto.member.MemberVerifyResetPasswordDTO;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

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

		if(!NickNameValidation.isValidNickname(trimNickname)) {
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
			                                                        BindingResult bindingResult) {

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

		// 비밀번호 유효성 검사
		if(!PasswordValidation.isValidPassword(trimPassword)) {
			return ResponseEntity.badRequest().body("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
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
		if(!NickNameValidation.isValidNickname(trimNickName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("닉네임이 유효하지 않습니다.");
		}
		
		// 중복된 닉네임일 경우 409반환
		if(!memberService.serviceCheckNickname(trimNickName)) {
			//Client Error 409 CONFLICT(충돌,중복)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
		}

		memberSignupDto.setNickname(trimNickName);

		
		try {
			//DTO(toEntity()) -> Entity 변경후 -> Service 호출 
			memberService.signup(memberSignupDto.toEntity());
		} catch (DuplicateKeyException e) {
			//이메일(Unique)제약이 걸려있으므로, 이메일 중복일시 발생하는 예외
			logger.error("MemberService DuplicateKeyException 발생   :     "+ e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다.");
		} catch (DataIntegrityViolationException e) {
			//DB 저장 시 제약조건 위반 발생 시 (예: 중복, 필수값 누락 등)
			 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력 데이터가 유효하지 않습니다." );
		} catch (Exception e) {
			//서버 에러 500번대 : INTERNAL_SERVER_ERROR
			logger.error("MemberService RuntimeException 발생   :     " + e.getMessage() , e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러가 발생했습니다.");
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

		if(bindingResult.hasErrors()) {
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
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			// Service 에서 던진 'RuntimeException' catch
			// 'Runtime'은 서버 실행 문제이므로 '서버 오류 처리 (INTERNAL_SERVER_ERROR(500번대))
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		// 예외가 발생하지 않으면 임시 토큰을 Map(Key, Value)으로 응답(Response)
		return ResponseEntity.ok(Collections.singletonMap("token", token));
	}

	// 2. 이메일(ID) 찾기 발급된 임시토큰으로 아이디 찾기 API엔드포인트
	@GetMapping("/show-email")
	public ResponseEntity<?> showEmail(@RequestHeader("Authorization") String bearerToken) {

		// Authorization Header(헤더)의 Bearer 토큰명 ->(replace) 토큰명 변환
		String token = bearerToken.replace("Bearer ", "");
		if(!jwtUtil.validateToken(token)) {
			//인증 관련 실패이므로, UNAUTHORIZED(401) 메세지 리턴
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
		}

		// 임시토큰에 저장된 정보를 추출하기 위한 Claims Class
		Claims claims = jwtUtil.getClaimsFromToken(token);

		// 이메일 임시토큰에 주체 비교
	    if (!"TEMP".equals(claims.getSubject())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일 임시 토큰이 아닙니다.");
	    }

	    // 임시토큰에 저장된 '사용자 이름' 추출
	    String usernameClaims = claims.get("username", String.class);
	    // 임시토큰에 저장된 '주민버노' 추출
	    String residentNumberClaims = claims.get("residentNumber", String.class);

	    // 다시 한번 DB 조회
	    List<Member> allMembers = memberRepository.findAll();
	    for (Member member : allMembers) {
	    	
	    	/*
	    	// 'true' : 복호화 주민번호 'false' : 암호화 주민번호 
	    	if(!isHexBinary(member.getResidentNumber())) {
	    		// 복호화 된 주민번호를 갖은 회원일시 다음 회원 으로 'continue'
	    		continue;
	    	}
	    	*/
	        try {
	        	// 주민번호 복호화
	            String decryptedRRN = AES256Util.decrypt(member.getResidentNumber());
	            if (member.getUsername().equals(usernameClaims ) && decryptedRRN.equals(residentNumberClaims)) {
	            	//200(OK)
	                return ResponseEntity.ok(Collections.singletonMap("email", member.getEmail()));
	            }
	        } catch (RuntimeException e) {
	        	//500(Server Error)
	        	logger.error("MemberController RuntimeException 발생   :   " + e.getMessage() , e);
	        	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
	        }
	    }
	    //400(Client Error)
	    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("회원 정보가 일치하지 않습니다.");
	}

	@PostMapping("/reset-password-token")
	public ResponseEntity<?> findPassword(@RequestBody @Valid MemberVerifyResetPasswordDTO verifyResetPasswordDto,
															  BindingResult bindingResult) {

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
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}
		return ResponseEntity.ok(Collections.singletonMap("token", token));
	}

	@PatchMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestHeader("Authorization") String bearerToken,
										   @RequestBody @Valid MemberResetPasswordDTO resetPasswordDto,
										                       BindingResult bindingResult) {

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
		logger.info("MemberController resetPassword claims :" + claims);

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

		try {
			memberService.resetPasswordByEmail(emailClaims, newPassword);
		} catch (IllegalArgumentException e) {
			logger.error("resetPasswordByEmail IllegalArgumentException 발생" + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("기존 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
	}

	/*
	// 토큰 테스트 API엔드포인트
	// URL,HEADER(Token)을(를) 받기위해 '@GetMapping'
	@GetMapping("/test")
	public ResponseEntity<String> testJwtToken(Authentication authentication) {
		// JwtAuthenticationFilter에서 인증 성공 시, email을 principal로 설정함
		// authentication.getPrincipal(); -> email을 가져올 수 있음
		String email = (String) authentication.getPrincipal();
		//testuser@example.com
		return ResponseEntity.ok("인증 성공! 현재 로그인된 사용자 이메일: " + email);
	}
	*/

	// 회원정보 API엔드포인트
	// URL,HEADER(Token)을(를) 조화하기위해 '@GetMapping'
	@GetMapping("/me")
	public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

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
		} catch (DuplicateKeyException e) {
	        logger.error("getMyInfo DuplicateKeyException 발생   :    " + e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다.");
	    } catch (DataIntegrityViolationException e) {
	        logger.error("getMyInfo DataIntegrityViolationException 발생   :   " + e.getMessage() , e);
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력 데이터가 유효하지 않습니다.");
	    } catch (Exception e) {
	        logger.error("getMyInfo RuntimeException 발생   :    " + e.getMessage(), e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
	    }
		/*
		DTO로 가공해서 응답
		MemberInfoDto memberInfoDto = new MemberInfoDto(member.getUsername(), member.getEmail(), member.getPhoneNumber());
		*/
		// Controller -> View
		return ResponseEntity.ok(memberInfoDto);
	}

	// 회원정보 수정 API엔드포인트 (데이터 일부 수정 : 'Patch')
	@PatchMapping("/me") 
	public ResponseEntity<?> updateMyInfo(@RequestBody MemberUpdateRequestDTO memberUpdateRequest,
										  @AuthenticationPrincipal CustomUserDetails customUserDetails) {

		// 'DTO'는 @RequestBody로 매핑되므로, 요청 body가 없으면 컨트롤러 진입 전 400(Bad Request) 에러 발생
		// 따라서 DTO 자체에 대한 null 체크는 불필요하며, 각 필드에 대해서만 유효성 검사를 수행하면 됨

		// 토큰 에서 닉네임 가져오기
		String trimCurrentNickname = customUserDetails.getNickname().trim();

		//DTO 설정
		String trimDtoNewNickName = safeTrim(memberUpdateRequest.getNickname());
		String trimDtoNewPhoneNumber = safeTrim(memberUpdateRequest.getPhoneNumber());
		String trimDtoNewAddress = safeTrim(memberUpdateRequest.getAddress());

		if(!trimDtoNewNickName.isEmpty()) {
			// 비정상 닉네임일 경우 BadRequest(400) 반환
			if(!NickNameValidation.isValidNickname(trimDtoNewNickName)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("닉네임이 유효하지 않습니다.");
			}
			// 중복된 닉네임일 경우 409반환
			if(!memberService.serviceCheckNickname(trimDtoNewNickName)) {
				//Client Error 409 CONFLICT(충돌,중복)
				return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
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
			logger.error("updateMember DataIntegrityViolationException 발생   :" + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력 데이터가 유효하지 않습니다.");
		} catch (Exception e) {
			logger.error("updateMember Exception 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		return ResponseEntity.ok("회원정보가 수정되었습니다.");
	}
	
	// 회원 탈퇴 API엔드포인트
	@DeleteMapping("/me")
	public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		String trimEmail = safeTrim(customUserDetails.getEmail());
		try {
			memberService.deleteMemberByEmail(trimEmail);
		} catch (IllegalArgumentException e) {
			logger.error("회원 탈퇴 실패: " + e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러");
		}

		return ResponseEntity.ok("회원 탈퇴가 정상 처리되었습니다.");
	}

	//*************************************************** API END ***************************************************//
}
