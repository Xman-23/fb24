var debounceTimer;
var currentKeyword = '';   // 검색어 저장
var currentSearchType = 'keyword'; // 검색 타입

var currentMode = 'post'; // 'post' | 'search'

var errorPage = {
					pageNumber : 0,
					totalPages : 0
				};

const no_main_popularList = "메인 인기 게시글이 없습니다.";
const no_popularList = "인기 게시글이 없습니다.";
const no_normalList = "게시글이 없습니다.";
const no_searchList = "검색 결과가 없습니다.";
const no_fin_noticeList ="고정된 공지 게시글이 없습니다.";
const no_noticeList ="공지 게시글이 없습니다.";

// 게시글이 없는 경우 만들어줄 '<li></li>'태그
function no_posts_tag(string) {

	var no_posts_html = `
							<li class= "no-posts">
								${string}
							</li>
						`
	return no_posts_html;

}

// 게시글 유효성 체크
function check_posts(posts) {
	return !posts || posts.length === 0;
}


function renderPosts(posts) {

    if(check_posts(posts)) {
		$("#popular-posts-container").append(no_posts_tag(no_main_popularList));
 		return;
    }

    posts.forEach(function(post) {
        var thumbnailHtml = '';
        if (post.thumbnailImageUrl && post.thumbnailImageUrl.trim() !== '') {
            thumbnailHtml = `<img src="${post.thumbnailImageUrl}" alt="썸네일" class="post-thumbnail">`;
        }

        var postCard =`
        				<li class = "post-card">
        					<a href="/main/${post.postId}" class ="post-link">
        						${thumbnailHtml}
        						<div class="post-info">
        							<h3 class ="post-title">[🔥HOT🔥] ${post.title}</h3>
        							<p class = "post-meta">
        								작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 댓글: ${post.commentCount} <br>
        								게시판: ${post.boardName} | 조회수: ${post.viewCount} | 작성일자: ${post.createdAt}
        							</p>
        						</div>
        					</a>
        				</li>
			          `;

        $("#popular-posts-container").append(postCard);
    });
}

//*****************************************API Start*************************************************************

// 메인 인기글 API(페이지 넘길때 마다 호출)
function getMain(page = 0) {

	/* 페이징 변화시 상세 게시글 안보여주기
	if(page !==0) {
		page_check  = true;
	}

	if(page_check) {
		main_post.empty();
	}
	*/ 

	$("#popular-posts-container").empty();

	currentMode = 'post';

	$.ajax({
		url : `/main/popular-posts?page=${page}`, // 메인 컨트롤러 API
		method : 'GET',				// 메인 컨트롤러 HTTP 프로토콜
		success : function (data) {

			var mainDataLength = data.posts.length;

			if(mainDataLength === 0) {
				renderPosts(data.posts)
			} else {
				renderPosts(data.posts || []);
			}
			post_renderPagination(data);
		},
		error: function(xhr) {
		        $("#popular-posts-container").append(no_posts_tag(no_main_popularList));
		        post_renderPagination(errorPage);
		    alert(xhr.responseText || "");
		}
	});
}

// 나중에 API 갈아야함 인기게시글용 (작성자,닉네임, 실시간 검색으로)
// 검색 API
function executeSearch(page = 0) {

	currentKeyword  = $("#searchInput").val().trim(); //제목+본문 or 작성자
	currentSearchType = $("#searchType").val();


	// 검색어를 명시하지 않을시
	if(!currentKeyword) {
		alert("검색어를 입력하세요.");
		return;
	}

    if (currentKeyword.length < 2) {
        alert("검색어는 최소 2글자 이상 입력해주세요.");
        return;
    }

    currentMode = 'search';

	var url = "";

    if (currentSearchType === "keyword") {
        // 키워드 검색 (쿼리스트링(/search?))
    	url = `/main/popular_posts/keyword/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`;
    } else if (currentSearchType === "author") {
        // 작성자 검색 (PathVariable)
    	url = `/main/popular_posts/author/search/${encodeURIComponent(currentKeyword)}?page=${page}`;
    } else if(currentSearchType === "autocomplete") {
    	// 자동완성 검색 
    	url = `/main/popular_posts/autocomplete/title/search?title=${encodeURIComponent(currentKeyword)}&page=${page}`;
    }

	$("#popular-posts-container").empty();

	$.ajax({
		url : url,
		method : "GET",
		success: function(data) {

            // 제목 변경: 검색 결과일 경우
            $("#popular-posts h2").text(`검색 결과: "${currentKeyword}"`);

			if(data.posts && data.posts.length > 0) {
				renderPosts(data.posts || []);
				post_renderPagination(data);
			}else {
				$("#popular-posts-container").append(no_posts_tag(no_searchList));
				post_renderPagination(data);
			}
			
		},
		error: function(err) {
			$("#popular-posts h2").text(`검색 결과: "${currentKeyword}"`);
			$("#popular-posts-container").append(no_posts_tag(no_searchList));
			post_renderPagination(errorPage);
		    alert(err.responseText || "");
		}
	})
}

// 실시간 검색 API 
function RealTimeSearch() {

	// 자동완성 리스트 태그 
	const list = $("#autocomplete-list");

    $("#searchInput").off("input").on("input", function() {


		if(currentSearchType !== 'keyword'){
			return; // 작성자 검색 시 자동완성 비활성화
		}

        clearTimeout(debounceTimer);
        // 제목+본문 input 태그 value 가져온 후 앞뒤 공백제거
        const keyword = $(this).val().trim();

        // 키워드가 존재 하지 않거나 2글자 미만일경우 자동완성 리스트 호출 X
        if(!keyword || keyword.length < 2) {
        	list.empty().hide();
            return;
        }

        // window.setTimeout() 메서드 호출 API 실행후 (0.5초) 후 재실행
        debounceTimer = window.setTimeout(() => {
            $.ajax({
                url: `/main/popular_posts/autocomplete/search?keyword=${encodeURIComponent(keyword)}`,
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
                            $("#searchInput").val(title);
                        	// 검색 타입을 자동완성 타입으로 부여
                            $("#searchType").val("autocomplete");
                        	// 리스트 비워고 가려준 다음에
                            list.empty().hide();
                        	// <li>태그 클릭시 바로 검색 API 호출
                            executeSearch(0); 
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
//*****************************************API End*************************************************************

function post_renderPagination(data) {
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
        if (currentMode === 'post') {
        	getMain(targetPage);
        } else {
        	executeSearch(targetPage);
        }
        
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

// 실행
$(document).ready(function() {
	/* Board API Start */
	/* main API 호출 */
	getMain(0);
	/* 실시간 검색 API호출 */
	RealTimeSearch();
	/* Board API End */
});