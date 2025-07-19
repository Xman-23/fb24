package com.example.demo.controller;


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

import com.example.demo.memberdto.MemberVerifyFindEmailRequestDto;
import com.example.demo.memberdto.MemberVerifyResetPasswordDto;
import com.example.demo.repository.MemberRepository;
import com.example.demo.security.AES256Util;
import com.example.demo.authdto.AuthLoginDto;
import com.example.demo.domain.Member;
import com.example.demo.jwt.CustomUserDetails;
import com.example.demo.jwt.JwtUtil;
import com.example.demo.memberdto.MemberInfoDto;
import com.example.demo.memberdto.MemberSignupDto;
import com.example.demo.memberdto.MemberUpdateRequest;
import com.example.demo.memberdto.MemberResetPasswordDto;
import com.example.demo.service.MemberService;

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

	// 닉네임 비속어 
	private static final String[] BANNED_NICKNAMES = {
		    // 한글 비속어
		    "시발", "씨발", "씨팔", "십새", "개새", "개같", "븅신", "병신",
		    "미친", "미쳣", "엿같", "좆", "존나", "지랄", "자지", "보지",
		    "후장", "엉덩이", "창녀", "걸레", "떡치", "싸발", "애미", "애비",
		    "놈년", "놈새", "느금", "틀딱", "김치녀", "된장녀", "한남", "메갈",
		    "일베", "급식충", "관종", "노답", "폐급",

		    // 우회 표현
		    "ㅅㅂ", "ㅄ", "ㅈㄴ", "ㅁㅊ", "ㅁㅊ놈", "ㅈ같", "ㅊㄴ", "ㅂㅅ",
		    "븅", "븅ㅅ", "병1신", "시1발", "씨1발", "ㅈ1랄", "미1친",

		    // 영어 비속어
		    "fuck", "shit", "bitch", "bastard", "ass", "asshole", "dick",
		    "pussy", "slut", "jerk", "whore", "nigger", "faggot", "gay",
		    "retard", "crap", "hell", "damn"
		};

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
	
    // 이메일 유효성 검사 메소드
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // 영문/숫자 포함, @ 포함, 도메인 형식
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(regex);
    }

    // 비밀번호 유효성 검사 메소드 (최소 8자, 영문+숫자+특수문자)
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        return password.matches(regex);
    }

    // 주민등록번호 유효성 검사 (하이픈 포함 or 미포함 가능)
    public static boolean isValidResidentNumber(String residentNumber) {
        if (residentNumber == null || residentNumber.trim().isEmpty()) {
            return false;
        }
        String regex = "^\\d{6}-?\\d{7}$";
        return residentNumber.matches(regex);
    }

    /*
    // DB 암호화 주민번호 유효성 검사 (복호화 주민번호 걸러내기 위한 메서드)
    public static boolean isHexBinary(String s) {
        return s.matches("[0-9A-Fa-f]+");
    }
	*/

    // 자음, 모음 유효성 검사
    public static boolean isOnlyConsonantOrVowel(String nickname) {

    	String regex = "^[ㄱ-ㅎㅏ-ㅣ]+$";

    	return nickname.matches(regex);
    }

    // 비속어 유효성 검사
    public static boolean containsForbiddenWord(String nickname) {

    	String trimToLowerCaseNickname = nickname.toLowerCase().trim();

    	for(int i=0; i<BANNED_NICKNAMES.length; i++) {
    		String toLowerCaseBannedNickname = BANNED_NICKNAMES[i].toLowerCase();
    		if(trimToLowerCaseNickname.contains(toLowerCaseBannedNickname)) {
    			return false; // 닉네임에 비속어가 있다면은 'false'
    		}
    	}
    	return true; // 닉네임에 비속어가 없다면은 'true'
    }

    // 닉네임 유효성 검사
    public static boolean isValidNickname(String nickname) {

    	if (nickname ==  null) {
    		return false;
    	}

    	// 완성형 한글, 영문, 숫자만 허용, 특수문자 불가
    	// 막는 것: 특수문자, 이모지, 띄어쓰기, 자음·모음 ‘외’의 이상한 문자들
    	String regex = "^[가-힣a-zA-Z0-9]{2,20}$";

    	String trimNickname = nickname.trim();

    	// 'true' : 유효하지 않은 닉네임, 'false' : 유효한 닉네임 
    	if(!trimNickname.matches(regex)) {
    		return false;
    	}

    	// '자음', '모음'만 있는 닉네임이라면은 'false';
    	if(isOnlyConsonantOrVowel(trimNickname)) {
    		return false;
    	}

    	// '비속어'가 있는 닉네임이라면은 'false';
    	if(!containsForbiddenWord(trimNickname)) {
    		return false;
    	}

    	// 정상적인 닉네임 이라면 'true'
    	return true;
    }

    // 휴대폰 번호 유효성 검사 메소드
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 숫자만 남기기 (하이픈, 공백 제거)
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // 정규표현식: 010으로 시작 + 10자리 또는 11자리
        String regex = "^01[016789]\\d{7,8}$";

        return cleaned.matches(regex);
    }

    // Request 에서 'null'을 가지고 있을 경우 안전하게 trim 처리하는 메서드
    private String safeTrim(String s) {
    	return (s == null) ? "" : s.trim();
    }

	// 회원가입 처리 API엔드포인트 (데이터 생성 : Post)
	// Json -> @RequestBody -> @Valid
	@PostMapping("/signup")
	public ResponseEntity<String> registerMember(@RequestBody@Valid MemberSignupDto memberSignupDto,
			                                                        BindingResult bindingResult) {
		// '@Vaild'에 의해 필드 유효성, 제약조건을 검사후 다음 코드 진행.
		// 필드가 유효하지 않을경우 'Bad Request(400)' 반환 

		// '@Vaild'에 의해 유효성 검사 결과가 BindingResult에 담김
		// 예외가 있을 경우 hasError() 메서드 'True'
		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		// 이메일 유효성 검사
		if(!isValidEmail(memberSignupDto.getEmail())) {
			return ResponseEntity.badRequest().body("이메일이 유효하지 않습니다.");
		}	

		
		// 비밀번호 유효성 검사
		if(!isValidPassword(memberSignupDto.getPassword())) {
			return ResponseEntity.badRequest().body("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
		}		

		// 주민번호 유효성 검사
		if(!isValidResidentNumber(memberSignupDto.getResidentNumber())) {
			return ResponseEntity.badRequest().body("주민번호가 유효하지 않습니다.");
		}	

		// 핸드폰 유효성 검사
		if(!isValidPhoneNumber(memberSignupDto.getPhoneNumber())) {
			return ResponseEntity.badRequest().body("핸드폰번호가 유효하지 않습니다.");
		}	

		// 휴대폰 번호 '-' 제거 후
		String removeTrimPhoneNumber = memberSignupDto.getPhoneNumber().replaceAll("-", "").trim();
		memberSignupDto.setPhoneNumber(removeTrimPhoneNumber);

		// 닉네임 유효성 검사 
		String trimNickName = memberSignupDto.getNickname().trim();

		// 비정상 닉네임일 경우 BadRequest(400) 반환
		if(!isValidNickname(trimNickName)) {
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
			//'HTTP.status'가 200번대
			return ResponseEntity.ok("회원가입 성공!");
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
	}

	// 닉네임 조회를 위한 API엔드포인트(데이터 조회 : Get)
	@GetMapping("/check-nickname")
	public ResponseEntity<?> checkNickname(@RequestParam("nickname") String nickname) {

		String trimNickname = nickname.trim();

		if(!isValidNickname(trimNickname)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 닉네임입니다.");
		}

		if(!memberService.serviceCheckNickname(trimNickname)) {
			//Client Error 409 CONFLICT(충돌,중복)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 닉네임입니다.");
		}

		return ResponseEntity.ok("사용 가능한 닉네임입니다.");
	}


	// 1.이메일(ID) 찾기 임시 토큰 발급 API 엔드 포인트
	// 사용자(username)와 주민번호(residentNumber)를 받아 일치시 임시 토큰 발급
	// @RequestBody(JsonBody)로 받으므로, PostMapping
	@PostMapping("/find-email")
	public ResponseEntity<?> findEmail(@RequestBody @Valid MemberVerifyFindEmailRequestDto verifyFindEmailRequestDto,
									   					   BindingResult bindingResult) {

		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

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
	public ResponseEntity<?> findPassword(@RequestBody @Valid MemberVerifyResetPasswordDto verifyResetPasswordDto,
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
										   @RequestBody @Valid MemberResetPasswordDto resetPasswordDto,
										                       BindingResult bindingResult) {

		logger.info("MemberController resetPassword 1");
		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body("입력값이 유효하지 않습니다.");
		}

		logger.info("MemberController resetPassword 2");
		String token = bearerToken.replace("Bearer ", "");
		String newPassword = resetPasswordDto.getNewPassword();
		String confirmNewPassword = resetPasswordDto.getConfirmNewPassword();

		logger.info("MemberController resetPassword 3");
		if (!isValidPassword(newPassword)) {
		    return ResponseEntity.badRequest().body("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
		}
		logger.info("MemberController resetPassword 4");
		if(!jwtUtil.validateToken(token)) {
			//인증 관련 실패이므로, UNAUTHORIZED(401) 메세지 리턴
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
		}
		logger.info("MemberController resetPassword 5");
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
		MemberInfoDto memberInfoDto = null;
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
	public ResponseEntity<?> updateMyInfo(@RequestBody MemberUpdateRequest memberUpdateRequest,
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
			if(!isValidNickname(trimDtoNewNickName)) {
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
			if(!isValidPhoneNumber(trimDtoNewPhoneNumber)) {
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
}
