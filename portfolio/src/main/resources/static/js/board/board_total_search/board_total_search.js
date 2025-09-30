let totalCurrnetMode = "search";
const no_searchList = "검색 결과가 없습니다.";


var errorPage = {
					pageNumber : 0,
					totalPages : 0
				};

// 게시글 유효성 체크
function check_posts(posts) {
	return !posts || posts.length === 0;
}

// 게시글이 없는 경우 만들어줄 '<li></li>'태그
function no_posts_tag(string) {

	var no_posts_html = `
							<li class= "no-posts">
								${string}
							</li>
						`
	return no_posts_html;

}

function total_search_init() {
	const params = new URLSearchParams(window.location.search);
	const query = params.get("query") || "";
	const type = params.get("type") || "total_keyword";
	
	// input과 selectbox 초기화
	$("#total_search_input").val(query);
	$("#totalsearch_type").val(type);

	totalLoadSearchResults(0);
}

function totalLoadSearchResults(page = 0) {

	totalCurrentKeyword  = $("#total_search_input").val().trim(); //제목+본문 or 작성자
	totalCurrentSearchType = $("#totalsearch_type").val();

	// 검색어를 명시하지 않을시
	if(!totalCurrentKeyword) {
		$("#total_name").text(`검색 결과: "${totalCurrentKeyword}"`);
		$("#child_normal_post_list").append(no_posts_tag(no_searchList));
		alert("검색어를 입력하세요.");
		return;
	}

	if (totalCurrentKeyword.length < 2) {
		$("#total_name").text(`검색 결과: "${totalCurrentKeyword}"`);
		$("#child_normal_post_list").append(no_posts_tag(no_searchList));
	    alert("검색어는 최소 2글자 이상 입력해주세요.");
	    return;
	}

	var url = "";

	if (totalCurrentSearchType === "total_keyword") {
	    // 키워드 검색 (쿼리스트링(/search?))
		url = `/posts/search?keyword=${encodeURIComponent(totalCurrentKeyword)}&page=${page}`;
	} else if (totalCurrentSearchType === "total_author") {
	    // 작성자 검색 (PathVariable)
		url = `/posts/author/${encodeURIComponent(totalCurrentKeyword)}?page=${page}`;
	} else if(totalCurrentSearchType === "total_autocomplete") {
		// 자동완성 검색 
		url = `/posts/autocomplete/search?keyword=${encodeURIComponent(totalCurrentKeyword)}`
	}

	$("#child_normal_post_list").empty();

	$.ajax({
		url : url,
		method : "GET",
		success: function(data) {
			
			console.log("totalLoadSearchResults: ", data);
	        // 제목 변경: 검색 결과일 경우
	        $("#total_name").text(`검색 결과: "${totalCurrentKeyword}"`);

			if(data.posts && data.posts.length > 0) {
				renderNormalPosts(data.posts || []);
				total_search_post_renderPagination(data);
			}else {
				$("#child_normal_post_list").append(no_posts_tag(no_searchList));
				total_search_post_renderPagination(data);
			}
		},
		error: function(xhr) {
			$("#total_name").text(`검색 결과: "${totalCurrentKeyword}"`);
			$("#child_normal_post_list").append(no_posts_tag(no_searchList));
			total_search_post_renderPagination(errorPage);
		    alert(xhr.responseText);
		}
	})
}


// 일반글 렌더링
function renderNormalPosts(posts) {

    posts.forEach(post => {
	    const thumbnailHtml = post.thumbnailImageUrl ? `<img src="${post.thumbnailImageUrl}" class="post-thumbnail">` : '';
        $("#child_normal_post_list").append(`
            <li class="post-card">
                <a href="/board/${post.boardId}/normal/${post.postId}" class="post-link">
					${thumbnailHtml}
                    <div class="post-info">
                        <h3 class="post-title">${post.title}</h3>
                        <p class="post-meta">
                            작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 댓글: ${post.commentCount} <br>
                            게시판: ${post.boardName} | 조회수: ${post.viewCount} | 작성일자: ${post.createdAt}
                        </p>
                    </div>
                </a>
            </li>
        `);
    });
}

function total_search_post_renderPagination(data) {
    var currentPage = data.pageNumber; //현재 페이지
    var totalPages = data.totalPages; // 총데이터/Size = 총 페이지
    var lastPage = totalPages - 1; // 마지막페이지(인덱스 기준)

    const pageButtonsContainer = $("#page-buttons");
    pageButtonsContainer.empty();
	
	const maxPageButtons = 10;
	const group = Math.floor(currentPage / maxPageButtons);
	const startPage = group * maxPageButtons;
	const endPage = Math.min(startPage + maxPageButtons - 1, lastPage);

    // 현재 모드에 따라 페이지 이동 함수 선택
    function loadPage(targetPage) {
        totalLoadSearchResults(targetPage);
    }

    // 1) 페이지 번호 버튼 'i(index)'를 기준으로 페이징
    for (let i = startPage; i <= endPage; i++) {
    	// 버튼 번호 만들기
        let btn = $(`<button>${i + 1}</button>`);
    	// 현재 페이지(inedex) 와 for문의 'i'와 같으면 버튼 비활성화
        if (i === currentPage) {
        	btn.prop("disabled", true);
        }
		btn.click(() => loadPage(i));
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
$(document).ready(function() {

	total_search_init();

});