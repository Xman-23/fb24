// 타이머 변수 
var debounceTimer;
var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

// 타이머 변수
var debounceTimer;

var currentKeyword = '';     
var currentSearchType = 'keyword'; 

var sortBy = 'latest';

//*****************************************Board ID Start*************************************************************
// 도메인에서 '/'기준으로 배열화[] 5 -1  
const pathParts = window.location.pathname.split("/");
("pathParts:", pathParts);
// 도메인 배열에서 2번째 인덱스 boardId 가져오기
const boardId = Number(pathParts[pathParts.length - 4]);
// 도메인에서 마지막 인덱스 postId 가져오기
const postId = Number(pathParts[pathParts.length - 1]);
// 공지게시판 BoardId
const noticeBoard = [1];
// 부모게시판 BoardId
const parentBoardIds = [9, 14, 15, 20];

var memberId = Number(localStorage.getItem('memberId'));
//*****************************************No Comment Start************************************************************* 
const no_main_popularList = "메인 인기 게시글이 없습니다.";
const no_popularList = "인기 게시글이 없습니다.";
const no_normalList = "게시글이 없습니다.";
const no_searchList = "검색 결과가 없습니다.";
const no_fin_noticeList ="고정된 공지 게시글이 없습니다.";
const no_noticeList ="공지 게시글이 없습니다.";
//*****************************************No Comment End*************************************************************
function show_image_download(images) {
    const container = $("#post_images_download");
    container.empty(); // 기존 내용 초기화

    if (!images || images.length === 0) {
        container.hide();
        return;
    }

    // "이미지:" 스팬 추가
    let html = `<span id="image_download_span">이미지: </span>`;

    // 이미지 링크 추가
    html += images.map(img => {
        return `<a href="${img.imageUrl}" download="${img.originalFileName}" style="margin:0 5px;">${img.originalFileName}</a>`;
    }).join(", "); // 콤마로 연결

    container.append(html);
    container.show();
}
//*****************************************Board Function End*************************************************************
//*****************************************Function End*************************************************************

//*****************************************API Start******************************************************************
//*****************************************Post API Start******************************************************************
// 조회수 증가 API (메인, 부모 , 자식 게시글 공용으로 사용)
function view_count_increment(postId) {
	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}/view`,
		method: "PATCH",
		success: function() {
			getPostDetail(postId);
		},
		error: function(err) {
			("조회수 증가 실패: " + err.responseText);
		}
	})
}

// 게시글 호출 API
function getPostDetail() {
	$("#post_title").empty();
	$("#post_board").empty();
	$("#post_author").empty();
	$("#post_created").empty();
	$("#post_views").empty();
	$("#post_likes").empty();
	$("#post-dislikes").empty();
	$("#post_content").empty();
	$("#post_images").empty();
	$("#post_images_download").empty();

	// ajax옵션 객체 셋팅
	$.ajax({
		url: `/posts/${postId}`,
		method: 'GET',
		success: function(post) {
			if(post) {
				renderPost(post);
			}else {
				return;
			}
		},
		error: function(err) {
			("게시글 조회 실패: " + err.responseText);
		}
	})
}

// 게시글 수정 버튼 클릭
$(document).on("click", "#btn_edit_post", function() {

    if (!token) {
        if (confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
            localStorage.setItem("redirectAfterLogin", window.location.href);
            window.location.href = "/signin";
        }
        return;
    }

    // 작성자만 접근 가능 → renderPost()에서 버튼 자체는 작성자만 보이도록 처리했으므로,
    // 여기서는 바로 수정 페이지로 이동만 하면 됨
    window.location.href = `/board/${boardId}/normal/post/${postId}`;
});

// 게시글 삭제 버튼 클릭
$(document).on("click", "#btn_delete_post", function() {

	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			localStorage.setItem("redirectAfterLogin", window.location.href);
			window.location.href ="/signin";
		}
		return;	
	}

    if(!confirm("정말로 게시글을 삭제하시겠습니까?")) return;

    ajaxWithToken({
        url: `/posts/${postId}`,
        type: "DELETE",
        success: function() {
            alert("게시글이 삭제되었습니다.");
            window.location.href =`/board_normal/${boardId}`; // 목록 페이지로 이동
        },
        error: function(xhr) {
            alert(xhr.responseText || "게시글 삭제 중 오류가 발생했습니다.");
        }
    });
});

// 상세 게시글 좋아요/싫어요 클릭 API (메인, 부모 , 자식 게시글 공용으로 사용)
function reaction_api(postId,token) {

	$("#post_btn_like, #post_btn_dislike").off("click").on("click", function() {

		if(!token) {
			if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
				localStorage.setItem("redirectAfterLogin", window.location.href);
				window.location.href ="/signin";
			}
			return;	
		}

		// 좋아요, 싫어요 버튼 클릭시 해당 객체 태그 ($(this)) 가져와서, 해당 태그의 'id' 삼항연산자로 비교
	    var reactionType = $(this).attr("id") === "post_btn_like" ? "LIKE" : "DISLIKE";

	    ajaxWithToken({
	        url: `/postreactions/${postId}/reaction`,
	        type: "POST",
	        contentType: "application/json",
	        data: JSON.stringify({ reactionType: reactionType }),
	        success: function(response) {
	            // 서버 응답 DTO(PostReactionResponseDTO) 값으로 UI 업데이트
	            $("#post_likes").text(response.likeCount);
	            $("#post_dislikes").text(response.dislikeCount);

	            // 내가 현재 누른 상태 강조 (LIKE, DISLIKE, null)
	            if (response.userPostReactionType === "LIKE") {
	                $("#post_btn_like").css("font-weight", "bold");
	                $("#post_btn_dislike").css("font-weight", "normal");
	            } else if (response.userPostReactionType === "DISLIKE") {
	                $("#post_btn_dislike").css("font-weight", "bold");
	                $("#post_btn_like").css("font-weight", "normal");
	            } else {
	                // 취소된 경우
	                $("#post_btn_like, #post_btn_dislike").css("font-weight", "normal");
	            }
	        },
	        error: function(xhr) {
	            alert(xhr.responseText);
	        }
	    });
	});
}

// -------------------- 게시글 신고 버튼 클릭 이벤트 --------------------
// 동적 DOM 포함 (게시글 신고 버튼 클래스: .post_btn_report)
$(document).on("click", "#post_btn_report", function() {

	if(!token) {
		if(confirm("로그인이 필요한 기능입니다. 로그인하시겠습니까?")) {
			localStorage.setItem("redirectAfterLogin", window.location.href);
			window.location.href ="/signin";
		}
		return;	
	}

    // URL에서 postId 추출
    const pathParts = window.location.pathname.split("/");
    const postId = Number(pathParts[pathParts.length - 1]);

    openPostReportPopup(postId);
});

// -------------------- 게시글 신고 팝업 열기 --------------------
function openPostReportPopup(postId) {
    // textarea 초기화 & 포커스
    $("#post_report_reason").val("").focus();

    // postId 저장 (신고 제출 시 사용)
    $("#btn_submit_post_report").data("postId", postId);

    // 팝업 표시
    $("#post_report_modal, #post_popup_overlay").fadeIn(200);
}

// -------------------- 게시글 신고 제출 --------------------
$(document).on("click", "#btn_submit_post_report", function() {
    const postId = $(this).data("postId");
    const reason = $("#post_report_reason").val().trim();

    if(reason.length < 10) {
        alert("신고 사유는 최소 10글자 이상이어야 합니다.");
        $("#post_report_reason").focus();
        return;
    }

    ajaxWithToken({
        url: `/posts/${postId}/report`,
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify({ reason }),
        success: function(response) {
            alert(response || "신고가 접수되었습니다.");
            $("#post_report_modal, #post_popup_overlay").fadeOut(200);
        },
        error: function(xhr) {
            alert(xhr.responseText || "신고 처리 중 오류가 발생했습니다.");
        }
    });
});

// -------------------- 게시글 신고 팝업 취소 --------------------
$(document).on("click", "#btn_cancel_post_report, #post_popup_overlay", function() {
    $("#post_report_modal, #post_popup_overlay").fadeOut(200);
});
//*****************************************Post API End******************************************************************
//*************************************************** Post 랜더링 Start ***************************************************//

function renderPost(post) {
	$("#post_title").text("[HOT]"+post.title);
	$("#post_board").text(post.boardName);
	$("#post_views").text(post.viewCount);
	$("#post_author").text(post.userNickname);
	$("#post_created").text(post.createdAt);
	$("#post_likes").text(post.likeCount);
	$("#post_dislikes").text(post.dislikeCount);
	$("#post_content").html(post.content);

	if(token) {
		// 버튼 표시 제어
		if (memberId === post.authorId) {
		    // 작성자가 본인일 경우
		    $("#post_manage_buttons").show();       // 수정/삭제 버튼 보이기
		    $("#post_btn_report").hide();           // 신고 버튼 숨기기
		} else {
		    // 작성자가 본인이 아닐 경우
		    $("#post_manage_buttons").hide();       // 수정/삭제 버튼 숨기기
		    $("#post_btn_report").show();           // 신고 버튼 보이기
		}
	}else {
		// 로그인이 안될시
		$("#post_manage_buttons").hide();       // 수정/삭제 버튼 숨기기
		$("#post_btn_report").hide();           // 신고 버튼 숨기기
	}

    if (post.imageUrls && post.imageUrls.length > 0) {
        post.imageUrls.forEach(url => {
        	$("#post_images").append(`<img src="${url}" alt="게시글 이미지">`);
        });
        shwo_image_download(post.imageUrls);
    }
}

function renderPost(post) {
	$("#post_title").text("[HOT] " + post.title);
	$("#post_board").text(post.boardName);
	$("#post_views").text(post.viewCount);
	$("#post_author").text(post.userNickname);
	$("#post_created").text(post.createdAt);
	$("#post_likes").text(post.likeCount);
	$("#post_dislikes").text(post.dislikeCount);
	$("#post_content").html(post.content);

	if(token) {
		// 버튼 표시 제어
		if (memberId === post.authorId) {
		    // 작성자가 본인일 경우
		    $("#post_manage_buttons").show();       // 수정/삭제 버튼 보이기
		    $("#post_btn_report").hide();           // 신고 버튼 숨기기
		} else {
		    // 작성자가 본인이 아닐 경우
		    $("#post_manage_buttons").hide();       // 수정/삭제 버튼 숨기기
		    $("#post_btn_report").show();           // 신고 버튼 보이기
		}
	}else {
		// 로그인이 안될시
		$("#post_manage_buttons").hide();       // 수정/삭제 버튼 숨기기
		$("#post_btn_report").hide();           // 신고 버튼 숨기기
	}

	getPostImages(postId);
}

// 기존 게시글 이미지 불러오기
function getPostImages(postId) {
    $.ajax({
        url: `/posts/${postId}/images`,
        method: 'GET',
        success: function(images) {

			if (images && images.length > 0) {
			    // orderNum 기준 오름차순 정렬
			    images.sort((a, b) => a.orderNum - b.orderNum);

			    images.forEach(img => {
			        $("#post_images").append(`<img src="${img.imageUrl}" alt="${img.originalFileName}">`);
			    });

			    // 다운로드 링크에도 원본 파일명 적용
			    show_image_download(images.map(img => ({
			        url: img.imageUrl,
			        originalFileName: img.originalFileName
			    })));
			}
			show_image_download(images);
        },
        error: function(err) {
            alert("이미지 조회 실패: " + err.responseText);
        }
    });
}

//*************************************************** Post 랜더링 End ***************************************************//

//*****************************************API End******************************************************************
$(document).ready(function() {
	/* Post API Start */
	view_count_increment(postId); //조회수 증가(그안에 게시글 불러오기)
	reaction_api(postId,token); // 게시글 리액션
	/* Post API End */
});