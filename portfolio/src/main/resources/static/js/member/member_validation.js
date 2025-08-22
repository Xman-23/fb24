// 사용자 이름 검사
function checkUserName() {
	var valid = false;
	var userNameLength = $("#username").val().trim().length;
	if(userNameLength > 0) {
		valid = true;
	}

	return valid;
}

// ============================================
// 이메일 유효성 검사
// feedback 색상 대신 data-valid 속성으로 검증
// 색상만 같으면 다른라우저에서 true (rgb(0,128,0) == rgba(0,128,1))
// true이면 유효, false이면 유효하지 않음
// ==========================================
function checkEmailValid() {
	var valid = false;
	if($("#email-feedback").attr("data-valid") === "true"){
		valid = true;
	}
    return valid
}

// ============================================
// 닉넹미 유효성 검사
// feedback 색상 대신 data-valid 속성으로 검증 
// 색상만 같으면 다른라우저에서 true (rgb(0,128,0) == rgba(0,128,1))
// true이면 유효, false이면 유효하지 않음
// ==========================================
function checkNickNameValid() {
	var valid = false;
	if($("#nickname-feedback").attr("data-valid") === "true") {
		valid = true;
	}
	return valid;
}

// 첫번쨰 패스워드 input 태그 유효성 검사
function checkPasswordValidity() {
    var password = $("#password").val();

    // 패스워드 유효성 검사
    if (!password) { 
    	return false;
    }
    // 비밀번호 유효성 검사 메소드 (최소 8자, 영문+숫자+특수문자)
    var regex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

    // 패스워드 정규식 match(test)
    if (regex.test(password)) {
    	$("#password-valid-feedback").text("사용 가능한 비밀번호입니다.").css("color", "green");
        return true;
    } else {
        $("#password-valid-feedback").text("비밀번호는 최소 8자, 영문+숫자+특수문자 포함이어야 합니다.").css("color", "red");
        return false;
    }
}

// 두번재 비밀번호 input태그 확인 & 유효성 검사
function checkPasswordMatch() {
	// 패스워드 value
    var password = $("#password").val();
	// 패스워드 확인 value
    var confirm = $("#confirmPassword").val();

	// 패스워드 확인 value 유효성 검사
    if (!confirm) {
        $("#password-match-feedback").text("");
        return false;
    }

	// 패스워드 === 패스워드 확인 
    if (password === confirm) {
        $("#password-match-feedback").text("비밀번호가 일치합니다.").css("color", "green");
        return true;
    } else {
        $("#password-match-feedback").text("비밀번호가 일치하지 않습니다.").css("color", "red");
        return false;
    }
}

// 주민번호 유효성 검사
function checkResidentNumber() {
	var valid = false;
    var rn1 = $("#residentNumber1").val().trim();
    var rn2 = $("#residentNumber2").val().trim();

    // 숫자만 있는지 확인
    var rn1Valid = /^\d{6}$/.test(rn1); // 앞 6자리 숫자
    var rn2Valid = /^\d{7}$/.test(rn2); // 뒤 7자리 숫자
	
	if(rn1Valid && rn2Valid) {
		valid = true;
	}

    return valid;
}
// 주민번호 digit(숫자) 체크
function checkResidentNumberDigit() {
	$('#residentNumber1, #residentNumber2').on('input', function() {
	    this.value = this.value.replace(/\D/g,''); // 숫자만 허용
	});
}

function checkPhoneNumberDigit() {
	// 핸드폰 번호 숫자만 입력
	$('#phoneNumber1, #phoneNumber2, #phoneNumber3').on('input', function() {
		// "/\D/g" : 숫자가 아닌 문자 찾아내기
	    this.value = this.value.replace(/\D/g, '');
	});
}

// 핸드폰 번호 유효성 검사
function checkPhoneNumber() {
	var valid = false;
    var p1 = $("#phoneNumber1").val().trim();
    var p2 = $("#phoneNumber2").val().trim();
    var p3 = $("#phoneNumber3").val().trim();

	var p1Valid = /^\d{3}$/.test(p1);
	var p2Valid = /^\d{4}$/.test(p2);
	var p3Valid = /^\d{4}$/.test(p3);

	if(p1Valid && p2Valid && p3Valid) {
		valid = true;
	}
    // 입력된 핸드폰번호 value가 숫자 인지 확인
    return valid;
}
