// 방문 기록을 위한 'Cookie'API
function getCookie() {
	$.ajax({
		url : "/visitors/track",
		method: "GET",
		success: function() {

			// 오늘/이번 달 방문자 수 가져오기
			getVisitor();

		},
		error: function(xhr) {
			alert(xhr.responseText || "");
			// 실패해도 방문자수 가져오기
			getVisitor();
		}
	})
}

// 방문자 API(바텀)
function getVisitor() {
	// 2025-08-17T14:58:18.220Z -> slice(0,10)
	// -> 2025-08-17
	var today = new Date().toLocaleDateString("sv-SE",{ timeZone : "Asia/Seoul"});

	// tag
	var todayContainer = $('#today-visitors');
	var monthContainer = $('#month-visitors');

	// 오늘 방문자 ex) /visitors/daily?date = today(2025-08-17)
	$.getJSON("/visitors/daily", {date: today})
	 .done(function(todayCount) {
		 // ajax 성공시
		 todayContainer.empty();
		 todayContainer.text(formatNumber(todayCount));
	 })
	 .fail(function(xhr) {
		alert(xhr.responseText); // 서버 예외 메세지 표시
	 })

	 // 이번 달 방문자 ex) /visitors/monthly?yearMonth = today(2025-08)
	$.getJSON("/visitors/monthly", {yearMonth: today})
	 .done(function(monthCount){
		 // ajax 성공시
		 monthContainer.empty();
		 monthContainer.text(formatNumber(monthCount));
	 })
	 .fail(function(xhr) {
		alert(xhr.responseText); // 서버 예외 메세지 표시
	 })
}

// 숫자 ',' 처리
function formatNumber(number) {

	if (number == null || isNaN(number)) {
		return "";
	}

	return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// ===================== 페이지 로드 시 자동 시작 =====================
$(document).ready(function() {
	getCookie();
	getVisitor();
});