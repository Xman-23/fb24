// 타이머 변수
var debounceTimer;

var currentKeyword = '';     
var currentSearchType = 'keyword'; 

var sortBy = 'latest';

var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

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

//*************************************************** 페이지 초기화 START ***************************************************//

//*************************************************** 페이지 초기화 End ***************************************************//

//*************************************************** Function Start ***************************************************//

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

function check_child_board() {
	if(!parentBoardIds.includes(boardId) && !noticeBoard.includes(boardId)) {
		getBoardInfo();
		getMain(0);
		sort_select_change();
		auto_search();
		adminButton();
	}else {
        alert("잘못된 접근입니다. 일반 게시판이 아닙니다.");
    }
}

function sort_select_change() {
	// select 박스 변경 이벤트
	$("#sortSelect").on("change", function() {
	    sortBy = $(this).val();
	    getMain(0);
	});
}

function adminButton() {
    // 관리자면 버튼 보여주기
    const role = localStorage.getItem('role');
    if(role === 'ROLE_ADMIN') {
        $('#admin-footer-buttons').show();

		/*
	    // 버튼 클릭 이벤트
	    $('#createChildBoardBtn').on('click', function() {
	        window.location.href = `/board_child_create/${boardId}`;
	    });
		*/

	    $('#editParentBoardBtn').off('click').on('click', function() {
	        window.location.href = `/board_child_update/${boardId}`;
	    });

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

//*************************************************** Function End ***************************************************//

//*************************************************** 랜더링 Start ***************************************************//

// 인기글 렌더링
function renderPopularPosts(posts) {

    if (check_posts(posts)) {
        $("#child_popular_post_list").append(no_posts_tag(no_popularList));
        return;
    }

    posts.forEach(post => {
        const thumbnailHtml = post.thumbnailImageUrl ? `<img src="${post.thumbnailImageUrl}" class="post-thumbnail">` : '';
        $("#child_popular_post_list").append(`
            <li class="post-card popular">
                <a href="/board/${boardId}/normal/popular/${post.postId}" class="post-link">
                    ${thumbnailHtml}
                    <div class="post-info">
                        <h3 class="post-title">[HOT] ${post.title}</h3>
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

// 일반글 렌더링
function renderNormalPosts(posts) {
    if (check_posts(posts)) {
        $("#child_normal_post_list").append(no_posts_tag(no_normalList));
        return;
    }

    posts.forEach(post => {
        $("#child_normal_post_list").append(`
            <li class="post-card">
                <a href="/board/${boardId}/normal/${post.postId}" class="post-link">
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

//*************************************************** 랜더링 Start ***************************************************//

//*************************************************** API Start ***************************************************//

// 게시판 정보 조회
function getBoardInfo() {
    if (!boardId || isNaN(boardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
    $.getJSON(`/boards/${boardId}`, function(board) {
    	document.title = "Log_" + board.name;
        $("#child-name").text(board.name);
        $("#child-description").text(`(${board.description})`);
    }).fail(function(xhr) {
        console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

// 게시글 조회 (인기글 + 일반글)
function getMain(page = 0) {

	$("#child_popular_post_list").empty();
	$("#child_normal_post_list").empty();
	

	currentMode = 'post';

    $.getJSON(`/posts/boards/${boardId}/posts/sorted?sortBy=${sortBy}&page=${page}`, function(response) {
    	if(Number(page) === 0) {
		    renderPopularPosts(response.popularPosts); // 인기글 렌더링
    	}
        renderNormalPosts(response.normalPosts);   // 일반글 렌더링
        post_renderPagination(response);           // 페이징 렌더링
    }).fail(function(xhr) {
    	$("#child_popular_post_list").append(no_posts_tag(no_popularList));
    	$("#child_normal_post_list").append(no_posts_tag(no_normalList));
        alert("게시글 조회 실패:", xhr.responseText);
    });
}

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
    	url = `/posts/boards/child/${boardId}/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`;
    } else if (currentSearchType=== "author") {
    	url = `/posts/boards/child/${boardId}/search/author/${encodeURIComponent(currentKeyword)}?page=${page}`;
    }  else if(currentSearchType === "autocomplete") {
    	url = `/posts/boards/child/${boardId}/autocomplete/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`;
    }

	$("#child_popular_post_list").empty();
	$("#child_normal_post_list").empty();

	$.ajax({
		url : url,
		method : "GET",
		success: function(data) {

            // 제목 변경: 검색 결과일 경우
            $("#child-name").text(`검색 결과: "${currentKeyword}"`);
    		$("#child-description").text("");

			if(data.posts && data.posts.length > 0) {
				renderNormalPosts(data.posts || []);
			}else {
				$("#child_normal_post_list").append(no_posts_tag(no_searchList));
			}
			post_renderPagination(data);
		},
		error: function(xhr) {
			$("#child-name").text(`검색 결과: "${currentKeyword}"`);
			$("#child-description").text("");
			$("#child_normal_post_list").append(no_posts_tag(no_searchList));
			post_renderPagination(errorPage);
		    alert(xhr.responseText || "");
		}
	})
}
function auto_search() {
    // 자동완성 (키워드 검색 전용)
    $("#searchInput").on("input", function() {
        if(currentSearchType !== 'keyword') return; // 작성자 검색 시 자동완성 비활성화

        clearTimeout(debounceTimer);
        const keyword = $(this).val().trim();
        const list = $("#autocomplete-list");

        if(keyword.length < 2) {
            list.empty().hide();
            return;
        }

        debounceTimer = setTimeout(() => {
            $.ajax({
                url: `/posts/boards/child/${boardId}/autocomplete?keyword=${encodeURIComponent(keyword)}`,
                method: "GET",
                success: function(data) {
                    list.empty();
                    if(!data || data.length === 0) {
                        list.hide();
                        return;
                    }
                    data.forEach(title => {
                        const item = $(`<li class="autocomplete-item">${title}</li>`);
                        item.on("click", function() {
                            $("#searchInput").val(title);
                            $("#searchType").val("autocomplete");
                            list.empty().hide();
                            executeSearch(0);
                        });
                        list.append(item);
                    });
                    list.show();
                },
                error: function(xhr) {
                    console.error("자동완성 조회 실패:", xhr.responseText);
                }
            });
        }, 500); // 0.5초 디바운스
    });
}

function create_post() {
	$("#create_post_button").off("click").on("click", function() {
		if(!token) {
			if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
				localStorage.setItem("redirectAfterLogin", window.location.href);
				window.location.href = "/signin"; // 로그인 페이지 이동
			}
			return;	
		}

		window.location.href=`/board/${boardId}/normal/post`;
	})
}

// 실행
$(document).ready(function() {
    check_child_board();
	create_post(); // 게시글 생성하러가기
});