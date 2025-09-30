// 타이머 변수
var debounceTimer;
var adminRole = localStorage.getItem('role');
//*************************************************** 변수 START ***************************************************//

var currentKeyword = '';     
var currentSearchType = 'keyword'; 

var currentMode = 'post'; // 'post' | 'search'

//*************************************************** 변수 End ***************************************************//

//*****************************************Board ID Start*************************************************************
// 도메인에서 '/'기준으로 배열화 
const pathParts = window.location.pathname.split("/");
// 도메인에서 마지막 인덱스 boardId 가져오기
const boardId = Number(pathParts[pathParts.length - 1]);
// 공지게시판 BoardId
const noticeBoard = [1];
// 부모게시판 BoardId
const parentBoardIds = [9, 14, 15, 20];
//*****************************************Board ID End*************************************************************

//*****************************************No Comment Start************************************************************* 
const no_main_popularList = "메인 인기 게시글이 없습니다.";
const no_popularList = "인기 게시글이 없습니다.";
const no_normalList = "게시글이 없습니다.";
const no_searchList = "검색 결과가 없습니다.";
const no_fin_noticeList ="고정된 공지 게시글이 없습니다.";
const no_noticeList ="공지 게시글이 없습니다.";
//*****************************************No Comment End*************************************************************

//*************************************************** Funtion Start ***************************************************//
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

function check_notice_board() {
	if(noticeBoard.includes(boardId)) {
		getBoardInfo();
		getMain(0);
		adminButton();
		auto_search();
	}else{
		alert("잘못된 접근입니다. 공지 게시판이 아닙니다.");
		window.location.href ="/";
	}
}

function adminButton() {
    // 관리자면 버튼 보여주기
    const role = localStorage.getItem('role');
    if(role === 'ROLE_ADMIN') {
        $('#admin-footer-buttons').show();
        
	    // 버튼 클릭 이벤트
	    $('#createChildBoardBtn').off('click').on('click', function() {
	        window.location.href = `/board_child_create/${boardId}`;
	    });

	    $('#editParentBoardBtn').off('click').on('click', function() {
	        window.location.href = `/board_parent_update/${boardId}`;
	    });

	    // 게시판 삭제 API
	    $('#deleteParentBoardBtn').off('click').on('click', function() {
	        if(confirm("정말 이 부모게시판을 삭제하시겠습니까?")) {
	            ajaxWithToken({
	                url: `/boards/admin/${boardId}`,
	                type: 'DELETE',
	                success: function() {
	                    alert("삭제 완료");
	                    window.location.href = '/'; // 메인으로 이동
	                },
	                error: function(xhr) {
	                    alert(xhr.responseText || "삭제 실패");
	                }
	            });
	        }
	    });
    }
}
//*************************************************** Funtion End ***************************************************//

//*************************************************** 랜더링 Start ***************************************************//
// 상단 고정 공지글(3개) 렌더링
function renderTopPinnedNotices(posts) {

	if(check_posts(posts)) {
		$("#notice_fin_post_list").append(no_posts_tag(no_fin_noticeList));
		//프로세스 종료
		return;
	}

	posts.forEach(post => {
		if(post.notice === true) {
			const finNoticeHtml =`
					                <li class="post-card notice">
				                    	<a href="/board/${post.boardId}/notice/${post.postId}" class="post-link">
				                        	<div class="post-info">
				                            	<h3 class="post-title">📌 [공지] ${post.title}</h3>
				                            	<p class="post-meta">
				                                	작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 조회수: ${post.viewCount} <br>
				                                	게시판: ${post.boardName} | 작성일자: ${post.createdAt}
				                            	</p>
				                        	</div>
				                    	</a>
				                	</li>
            						 `;
			$("#notice_fin_post_list").append(finNoticeHtml);
		}
	})
}
 //일반 공지글 렌더링
 function renderNormalNotices(posts) {

	if(check_posts(posts)) {
		$("#notice_normal_post_list").append(no_posts_tag(no_noticeList));
		//프로세스 종료
		return;
	}

	posts.forEach(post => {
		if(post.notice === true) {
			const normalNoticeHtml =`
					                <li class="post-card normalnotice">
										<a href="/board/${post.boardId}/notice/${post.postId}" class="post-link">
				                        	<div class="post-info">
				                            	<h3 class="post-title">[공지] ${post.title}</h3>
				                            	<p class="post-meta">
				                                	작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 조회수: ${post.viewCount} <br>
				                                	게시판: ${post.boardName} | 작성일자: ${post.createdAt}
				                            	</p>
				                        	</div>
				                    	</a>
				                	</li>
            						 `;
			$("#notice_normal_post_list").append(normalNoticeHtml);
		}
	})
 }
//*************************************************** 랜더링 End ***************************************************//

//*************************************************** API Start ***************************************************//
// 게시판 정보 조회
function getBoardInfo() {
    if (!boardId || isNaN(boardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
    $.getJSON(`/boards/${boardId}`, function(board) {
    	document.title = "Log_" + board.name;
        $("#notice-name").text(board.name);
        $("#notice-description").text(`(${board.description})`);
    }).fail(function(xhr) {
        console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

function getMain(page = 0) {

	$("#notice_fin_post_list").empty();
	$("#notice_normal_post_list").empty();

	currentMode = 'post';

    $.ajax({
        url: `/posts/notices?page=${page}`,
        method: "GET",
        success: function(response) {

			if(Number(page) === 0) {
				renderTopPinnedNotices(response.topPinnedNotices);
			}
			
            //일반 공지글 렌더링
            renderNormalNotices(response.notices);

            // 페이징 정보 업데이트
            post_renderPagination(response);
        },
        error: function(xhr) {
        	$("#notice_fin_post_list").append(no_posts_tag(no_fin_noticeList));
        	$("#notice_normal_post_list").append(no_posts_tag(no_noticeList));
			post_renderPagination(errorPage);
            alert("공지글 조회 실패:", xhr.responseText);
        }
    });
}

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

    if(currentSearchType === "keyword") {
    	url = `/posts/boards/notice/${boardId}/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`
    } else if (currentSearchType=== "author") {
    	url = `/posts/boards/notice/${boardId}/search/author/${encodeURIComponent(currentKeyword)}?page=${page}`;
    } else if(currentSearchType === "autocomplete") {
    	url = `/posts/boards/notice/${boardId}/autocomplete/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`
    }

    $("#notice_fin_post_list").empty();
    $("#notice_normal_post_list").empty();

	$.ajax({
		url : url,
		method : "GET",
		success: function(data) {
            // 제목 변경: 검색 결과일 경우
            $("#notice-name").text(`검색 결과: "${currentKeyword}"`);
    		$("#notice-description").text("");

			if(data.posts && data.posts.length > 0) {
				renderNormalNotices(data.posts || []);
				post_renderPagination(data);
			}else {
				$("#notice_normal_post_list").append(no_posts_tag(no_searchList));
				post_renderPagination(data);
			}
			
		},
		error: function(xhr) {
			$("#notice-name").text(`검색 결과: "${currentKeyword}"`);
			$("#notice-description").text("");
			$("#notice_normal_post_list").append(no_posts_tag(no_searchList));
			post_renderPagination(errorPage);
		    alert(xhr.responseText || "");
		}
	})
}
function auto_search() {
	$("#searchInput").on("input", function() {
	    if (currentSearchType !== 'keyword') return; // 작성자 검색 시 자동완성 비활성화

	    clearTimeout(debounceTimer);
	    const keyword = $(this).val().trim();
	    const list = $("#autocomplete-list");

	    if (keyword.length < 2) {
	        list.empty().hide();
	        return;
	    }

	    debounceTimer = setTimeout(() => {
	        $.ajax({
	            url: `/posts/boards/notice/${boardId}/autocomplete?keyword=${encodeURIComponent(keyword)}`,
	            method: "GET",
	            success: function(data) {
	                list.empty();
	                if (!data || data.length === 0) {
	                    list.hide();
	                    return;
	                }

	                data.forEach(title => {
	                    const item = $(`<li class="autocomplete-item">${title}</li>`);
	                    item.on("click", function() {
	                        $("#searchInput").val(title);
	                        $("#searchType").val("autocomplete");
	                        list.empty().hide();
	                        executeSearch(0); // 클릭 시 바로 검색 실행
	                    });
	                    list.append(item);
	                });
	                list.show();
	            },
	            error: function(xhr) {
	                console.error("자동완성 조회 실패:", xhr.responseText);
	            }
	        });
	    }, 500); // 1초 디바운스
	});
}

function create_notice_post() {

	if(adminRole === "ROLE_ADMIN") {
		$("#create_post_button").show();

		$("#create_notice_post_button").off("click").on("click", function() {
			window.location.href=`/${boardId}/notice/post`;
		})
	}
}

//*************************************************** API End ***************************************************//

$(document).ready(function() {
	check_notice_board();
	create_notice_post();
});

