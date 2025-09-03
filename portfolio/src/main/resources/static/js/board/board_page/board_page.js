
var currentMode = 'post'; // 'post' | 'search'

var errorPage = {
					pageNumber : 0,
					totalPages : 0
				};

function post_renderPagination(data) {
	console.log("data :", data);
    var currentPage = data.pageNumber; //현재 페이지
    var totalPages = data.totalPages; // 총데이터/Size = 총 페이지
    var lastPage = totalPages - 1; // 마지막페이지(인덱스 기준)

    const pageButtonsContainer = $("#page-buttons");
    pageButtonsContainer.empty();

	if (totalPages === 0) {
	    pageButtonsContainer.empty();
	    // 버튼 1개만 보여주고 비활성화
	    var btn = $(`<button>1</button>`);
	    btn.prop("disabled", true);
	    pageButtonsContainer.append(btn);

		$("#pagination-container button").prop("disabled", true);

	    return;
	}

    // 현재 모드에 따라 페이지 이동 함수 선택
    function loadPage(targetPage) {
        if (currentMode === 'post') {
        	getMain(targetPage);
        } else {
        	executeSearch(targetPage);
        }
        
    }

    // 1) 페이지 번호 버튼 'i(index)'를 기준으로 페이징
    for (let i = 0; i < totalPages; i++) {
    	// 버튼 번호 만들기
        let btn = $(`<button>${i + 1}</button>`);
    	// 현재 페이지(inedex) 와 for문의 'i'와 같으면 버튼 비활성화
        if (i === currentPage) {
        	btn.prop("disabled", true);
        }
        btn.click(() => {
        	// 메인 인기글 페이징
            if(currentMode === 'post') {
                getMain(i);
            } else {
            	// 검색 페이징
                executeSearch(i);
            }
        });
        pageButtonsContainer.append(btn);
    }

	// 이전 페이지 버튼
    $("#prev-page").prop("disabled", !data.hasPrevious) // 이전 페이지가 없으면 버튼 비활성화
                   .off("click") // 기존 클릭 이벤트 제거 (버튼 중복 이벤트 방지)
                   .click(() => { // 버튼 클릭시 발생하는 이벤트
                	   // 이전버튼 누를시 이전버튼 페이지에 존재하는 게시물(데이터) 그려주기
                	   // currentPage가 2페이지(index=1)인경우 이전버튼을 누르면 1페이지(index=0)가 되야하므로
                	   // 'currentPage-1'
                	   if(data.hasPrevious) {
                		    // Math.max로 음수 방지
				        	loadPage(Math.max(currentPage - 1, 0))
				        }
    });

    // 다음 버튼
    $("#next-page").prop("disabled", !data.hasNext) // 다음 페이지가 없으면 버튼 비활성화
                   .off("click") // 기존 클릭 이벤트 제거 (버튼 중복 이벤트 방지)
                   .click(() => { // 버튼 클릭시 발생하는 이벤트
                	// 다음버튼 누를시 다음버튼 페이지에 존재하는 게시물(데이터) 그려주기
                	// currentPage가 2페이지(index=1)인경우 다음버튼을 누르면 3페이지(index=2)가 되므로,
                	// 'currentpage+1'
           	        if(data.hasNext) {
						// Math.main으로 마지막 페이지를 넘기는것을 방지
						loadPage(Math.min(currentPage + 1, lastPage));
        	        }
    });

    // 3) 처음 버튼
    $("#first-page").prop("disabled", !data.hasFirst) // 처음 페이지인경우, 버튼 비활성화
                    .off("click")
                    .click(() => {
                    	// 1페이지(index=0)의 게시물(데이터)를 그려줘야하기때문에
                    	// 1페이지를 의미하는 'index=0'을 넘겨줘야한다
            	        if(data.hasFirst) {
            	        	loadPage(0);
            	        }
    });
	// 4) 마지막 버튼
    $("#last-page").prop("disabled", !data.hasLast) // 마지막 페이지인경우 버튼 비활성화
                   .off("click")
                   .click(() => {
                	// 마지막페이지(lastPage)의 게시물(데이터)를 그려줘야하기때문에
                	// 마지막 페이지를 넘겨줘야한다.
           	        if(data.hasLast) {
           	        	loadPage(lastPage)
        	        }
    });

    // 5) 점프 뒤로 (10페이지 단위)
    $("#jump-backward").prop("disabled", data.jumpBackwardPage == null || data.jumpBackwardPage === currentPage)
                       .off("click")
                       .click(() => {
	               	        if(data.jumpBackwardPage != null && data.jumpBackwardPage !== currentPage) {
	               	        	loadPage(data.jumpBackwardPage)
	            	        }
    });
    // 6) 점프 앞으로 (10페이지 단위)
    $("#jump-forward").prop("disabled", data.jumpForwardPage == null || data.jumpForwardPage === currentPage)
                      .off("click")
                      .click(() => {
	               	        if(data.jumpForwardPage != null && data.jumpForwardPage !== currentPage) {
	               	        	loadPage(data.jumpForwardPage)
	           		        }
    });
}
