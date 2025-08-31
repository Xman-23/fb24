// 검색 버튼 및 Enter 키 이벤트 처리
function keywordSearch() {
    // (1) 검색 버튼 클릭 이벤트
    $(".search-btn").click(function() {
        executeSearch(0); // 검색 버튼을 누르면 1페이지(0번)부터 검색 실행
    });

    // (2) Enter 키 입력 이벤트
    $("#searchInput").on("keydown", function(e) {
        if (e.key === "Enter") {
            executeSearch(0); // 엔터 누르면 검색 실행
        }
    });
}

// input 외부 클릭 시 자동완성 닫기
function autoCompleteReset() {
	$(document).on("click", function(e) {
		//closest : 태그 자기자신, 부모태그들 찾기
	    if(!$(e.target).closest("#searchInput, #autocomplete-list").length) {
	        $("#autocomplete-list").empty().hide();
	    }
	});
}

function autoCompleteSearchReset() {
	// 검색 타입 변경 시 자동완성 초기화
	$("#searchType").on("change", function() {
	    currentSearchType = $(this).val();
	    $("#autocomplete-list").empty().hide();
	});

}

// 실행
$(document).ready(function() {
	/* 검색 API 호출 (바텀) */
	keywordSearch();
    autoCompleteSearchReset();
	autoCompleteReset();
});