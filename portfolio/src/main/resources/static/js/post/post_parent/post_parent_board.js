
var currentMode = 'post'; // 'post' | 'search'
var currentKeyword = '';     
var currentSearchType = 'keyword';

//*****************************************Function Start*************************************************************
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
//*****************************************Function End*************************************************************

//*************************************************** Function Start ***************************************************//
function check_parent_board_number(parentBoardIds) {
    if (parentBoardIds.includes(boardId) && !noticeBoard.includes(boardId)) {
        get_board_api(boardId);
        getMain(0);
        keywordSearch();
        auto_search();
        adminButton();
    } else {
        alert("잘못된 접근입니다. 일반 게시판이 아닙니다.");
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
	                success: function(res) {
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
// 공지 게시글 렌더링
function renderNoticePosts(posts) {
	// 공지 게시글 유효성 체크
	if(!posts && !posts.length > 0) {
		$("#post_notice_list").append(no_posts_tag(no_fin_noticeList));
	}

	posts.forEach(function(post) {
		if(post.notice === true) {
            const noticeHtml = `
                <li class="post-card notice">
                    <a href="/board/${boardId}/notice/${post.postId}" class="post-link">
                        <div class="post-info">
                            <h3 class="post-title">📌 [공지] ${post.title}</h3>
                            <p class="post-meta">
                                작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 조회수: ${post.viewCount} <br>
                                게시판: ${post.boardName} | 작성일자: ${post.createdAt}
                            </p>
                        </div>
                    </a>
                </li>`;
            $("#post_notice_list").append(noticeHtml);
		}
	})
}

// 인기 게시글 렌더링
function renderPosts(posts) {

    // 인기 게시글 유효성 체크
	if(!posts || !posts.length > 0) {
		$("#post_popluar_list").append(no_posts_tag(no_popularList));
	}

    posts.forEach(function(post) {
        if (post.notice === false) {
            let thumbnailHtml = '';
            if (post.thumbnailImageUrl && post.thumbnailImageUrl.trim() !== '') {
                thumbnailHtml = `<img src="${post.thumbnailImageUrl}" alt="썸네일" class="post-thumbnail">`;
            }
            const postHtml = `
                <li class="post-card">
                    <a href="/board/${boardId}/popular/${post.postId}" class="post-link">
                        ${thumbnailHtml}
                        <div class="post-info">
                            <h3 class="post-title">[HOT🔥] ${post.title}</h3>
                            <p class="post-meta">
                                작성자: ${post.userNickname} | 좋아요: ${post.reactionCount} | 댓글: ${post.commentCount} <br>
                                게시판: ${post.boardName} | 조회수: ${post.viewCount} | 작성일자: ${post.createdAt}
                            </p>
                        </div>
                    </a>
                </li>`;
            $("#post_popluar_list").append(postHtml);
        }
    });
}
//*************************************************** 랜더링 End ***************************************************//

//*************************************************** API Start ***************************************************//
// 게시판 정보 + 자식 게시판 조회
function get_board_api(boardId) {
	// 게시판ID 유효성 체크
    if (!boardId || isNaN(boardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
	// 현재 게시판 정보 조회 API
    $.getJSON(`/boards/${boardId}`, function(board) {
    	document.title = "Log | " + board.name;
        $('#parent-name').text(board.name);
        $('#board-description').text("(" + board.description + ")");

        // 자식 게시판 조회 API
        $.getJSON(`/boards/${boardId}/hierarchy`, function(data) {
        	// 자식 게시판이 존재한다면,
            if (data.childBoards && data.childBoards.length > 0) {
            	// 자식게시판을 나열하기 위한 <div>태그 생성
                const childList = $('<div class="child-list"></div>');
                data.childBoards.forEach(function(child, index) {
                	// 자식 게시판 <a href>(링크) 처리
                    const childLink = $(`<a href="/board_normal/${child.boardId}">${child.name}</a>`);
                    childList.append(childLink);
                    // 자식게시판 '|' 처리'(length이므로, index기준에 맞출려면 'length-1'처리)
                    if (index < data.childBoards.length - 1) {
                    	childList.append(' | ');
                    }
                });
                // 자식게시판 나열하기
                $("#board_parent_h2").after(childList);
            }
        }).fail(function(xhr) {
        	console.error("자식 게시판 조회 실패:", xhr.responseText);
        });
    }).fail(function(xhr) {
    	console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

// 부모 게시판 인기글,게시글 불러오기
function getMain(page = 0) {

    $("#post_notice_list").empty();
    $("#post_popluar_list").empty();

    currentMode = 'post';
    $.ajax({
        url: `/posts/boards/${boardId}/posts?page=${page}`,
        method: "GET",
        success: function(response) {
        	console.log(response);
        	// '0페이지' 일때만 공지 페이지 보여주기
        	if(page === 0) {
        		const noticePosts = [...response.topNotices || [] ]
        		renderNoticePosts(noticePosts);
        	}
            const posts = [...(response.popularPosts || [])];
            
            renderPosts(posts);
            post_renderPagination(response);
        },
        error: function(xhr) {

        	$("#post_notice_list").append(no_posts_tag(no_fin_noticeList));

			$("#post_popluar_list").append(no_posts_tag(no_popularList));

			post_renderPagination(errorPage);
		    alert(xhr.responseText || "");
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
    	url = `/posts/boards/${boardId}/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}`
    } else if (currentSearchType=== "author") {
    	url = `/posts/boards/${boardId}/search/author/${encodeURIComponent(currentKeyword)}?page=${page}`;
    } else if(currentSearchType === "autocomplete") {
    	url = `/posts/boards/${boardId}/autocomplete/search?title=${encodeURIComponent(currentKeyword)}&page=${page}`
    }

    $("#post_notice_list").empty();
    $("#post_popluar_list").empty();

	$.ajax({
		url : url,
		method : "GET",
		success: function(data) {
			
            // 제목 변경: 검색 결과일 경우
            $("#parent-name").text(`검색 결과: "${currentKeyword}"`);
    		$("#board-description").text("");

			if(data.posts && data.posts.length > 0) {
				renderPosts(data.posts || []);
				post_renderPagination(data);
			}else {
				$("#post_popluar_list").append(no_posts_tag(no_searchList));
				post_renderPagination(data);
			}
		},
		error: function(xhr) {
			$("#post_popluar_list").append(no_posts_tag(no_searchList));
			post_renderPagination(errorPage);
		    alert(xhr.responseText || "");
		}
	})
}

function auto_search() {
	// 검색 이벤트 초기화
    $("#searchInput").on("input", function() {
        if (currentSearchType !== 'keyword') return; // 작성자 검색 시 자동완성 비활성화

        clearTimeout(debounceTimer);
        const keyword = $(this).val().trim();
        const list = $("#autocomplete-list");

        if (!keyword||keyword.length < 2) {
            list.empty().hide();
            return;
        }
		
        debounceTimer = setTimeout(() => {
            $.ajax({
                url: `/posts/boards/${boardId}/autocomplete?keyword=${encodeURIComponent(keyword)}`,
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

//*************************************************** API End ***************************************************//

$(document).ready(function() {
	check_parent_board_number(parentBoardIds);
});