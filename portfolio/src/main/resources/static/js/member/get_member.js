// 이메일 붙이기
function getEmail() {
    return $("#email1").val().trim() + "@" + $("#email2").val().trim();
}

// 주민번호 붙이기
function getResidentNumber() {
    return ($("#residentNumber1").val() + $("#residentNumber2").val()).replaceAll("-", "").trim();
}

// 사용자이름 value 가져오기 
function getUserNameValue() {
	return $("#username").val().trim();
}

// 패스워드 trim()
function passwordTrim() {
	return $("#password").val().trim();
}

// 핸드폰 합치기
function getPhoneNumber() {
	return ($("#phoneNumber1").val() + $("#phoneNumber2").val() + $("#phoneNumber3").val()).replaceAll("-", "").trim();
}