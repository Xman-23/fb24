let totalCurrentSearchType = "total_keyword";

let totalDebounceTimer

// 검색 버튼 및 Enter 키 이벤트 처리
function total_keyword_search() {
    // (1) 검색 버튼 클릭 이벤트
    $(".total_search_btn").click(function() {
		total_search();
    });

    // (2) Enter 키 입력 이벤트
    $("#total_search_input").on("keydown", function(e) {
        if (e.key === "Enter") {
			total_search();
        }
    });
}

// input 외부 클릭 시 자동완성 닫기
function total_auto_complete_reset() {
	$(document).on("click", function(e) {
		//closest : 태그 자기자신, 부모태그들 찾기
	    if(!$(e.target).closest("#total_search_input, #total_autocomplete_list").length) {
	        $("#total_autocomplete_list").empty().hide();
	    }
	});
}

function total_auto_complete_search_reset() {
	// 검색 타입 변경 시 자동완성 초기화
	$("#totalsearch_type").on("change", function() {
	    totalCurrentSearchType = $(this).val();
	    $("#total_autocomplete_list").empty().hide();
	});

}

function total_search() {
	const keyword = $("#total_search_input").val().trim();
	const type = $("#totalsearch_type").val(); // total_keyword, total_author, total_autocomplete 등
	window.location.href = `/search?query=${encodeURIComponent(keyword)}&type=${encodeURIComponent(type)}`;
}

// 실시간 검색 API 
function total_real_time_search() {

	// 자동완성 리스트 태그 
	const list = $("#total_autocomplete_list");

    $("#total_search_input").off("input").on("input", function() {

		if (totalCurrentSearchType !== 'total_keyword') {
		    list.empty().hide();
		    return; // 작성자 검색 시 자동완성 비활성화
		}

        window.clearTimeout(totalDebounceTimer);
        // 제목+본문 input 태그 value 가져온 후 앞뒤 공백제거
        const keyword = $(this).val().trim();

        // 키워드가 존재 하지 않거나 2글자 미만일경우 자동완성 리스트 호출 X
        if(!keyword || keyword.length < 2) {
        	list.empty().hide();
            return;
        }

        // window.setTimeout() 메서드 호출 API 실행후 (0.5초) 후 재실행
        totalDebounceTimer = window.setTimeout(() => {
            $.ajax({
                url: `/posts/autocomplete?keyword=${encodeURIComponent(keyword)}`,
                method: "GET",
                success: function(data) {
                    list.empty();
                    if(data.length === 0) {
                        list.hide();
                        return;
                    }
					// 데이터가 있다면
                    data.forEach(title => {
                    	// <li>제목+본문 실시간 타이틀</li>태그 생성
                        const item = $(`<li>${title}</li>`);
                    	// <li></li> 태그 클릭시
                        item.on("click", function() {
                        	// 검색창 input태그에 value를 타이틀 부여
                            $("#total_search_input").val(title);
                        	// 검색 타입을 자동완성 타입으로 부여
                            $("#totalsearch_type").val("total_autocomplete");
                        	// 리스트 비워고 가려준 다음에
                            list.empty().hide();
                        	// <li>태그 클릭시 바로 검색 API 호출
                            total_search(); 
                        });
                    	// '#autocomplete-list'.append(<li>${title}</li>);
                        list.append(item);
                    });
					// "#autocomplete-list" 보여주기
                    list.show();
                },
                error: function(xhr) {
                    console.error(xhr);
                }
            });
        }, 500); // 300ms 디바운스
    });
}

// 실행
$(document).ready(function() {
	/* 검색 API 호출 (바텀) */
	total_keyword_search();
    total_auto_complete_reset();
	total_auto_complete_search_reset();
	total_real_time_search();
});