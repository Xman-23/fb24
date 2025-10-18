var token = localStorage.getItem('accessToken'); // 현재 액세스 토큰 가져오기

let allPreviews = [];  // 전체 preview DOM 저장
let files = [];
const maxCount = 7;   // 최대 업로드 개수 10개로 제한

let existingImages = [];    // 기존 게시글 이미지 정보 저장

//*****************************************Board ID Start*************************************************************
const pathParts = window.location.pathname.split("/");
const postId = Number(pathParts[pathParts.length - 1]);
//*****************************************Board ID End*************************************************************

function updateFileNameList() {
    $("#file_names_container").empty();

    // 기존 이미지 표시
    existingImages.forEach(img => {
        const fileNameItem = $("<div>")
            .addClass("file-name-item")
            .text(img.originalFileName)
            .data("existing", true);
        $("#file_names_container").append(fileNameItem);
    });

    // 새로 선택한 파일 표시
    files.forEach(f => {
        const fileNameItem = $("<div>")
            .addClass("file-name-item")
            .text(f.name)
            .data("file", f);
        $("#file_names_container").append(fileNameItem);
    });

}

//**************************************************************게시글 조회 Start******************************************************************************** */
// 게시판 정보 + 자식 게시판 조회
function get_board_api(childBoardId) {
	// 게시판ID 유효성 체크
    if (!childBoardId || isNaN(childBoardId)) {
    	alert("올바른 boardId가 필요합니다."); 
    	return; 
    }
	// 현재 게시판 정보 조회 API
    $.getJSON(`/boards/${childBoardId}`, function(board) {
		const boardId = board.parentBoard;
        // 자식 게시판 조회 API
        $.getJSON(`/boards/${boardId}/hierarchy`, function(data) {
			$("#board_select").empty();
			$.each(data.childBoards, function (i, child) {
			    var selected = (child.boardId === childBoardId) ? "selected" : "";
			    $("#board_select").append(
			        `<option value="${child.boardId}" ${selected}>${child.name}</option>`
			    );
			});
			getPostDetail();
        }).fail(function(xhr) {
        	console.error("자식 게시판 조회 실패:", xhr.responseText);
        });
    }).fail(function(xhr) {
    	console.error("게시판 단건 조회 실패:", xhr.responseText);
    });
}

// 게시글 호출 API
function getPostDetail() {
    $.ajax({
        url: `/posts/${postId}`,
        method: 'GET',
        success: function(post) {
            if(post) {
                // 제목, 내용 입력창에 값 세팅
                $("#title").val(post.title);
                $("#content").val(post.content);

				// 게시판 선택 값 세팅 (boardId 가 내려온다고 가정)
				/*if (post.boardId) {
				    $("#board_select").val(post.boardId);
				}*/

                // 이미지 목록도 불러오기
                getPostImages(postId);
            }
        },
        error: function(err) {
            alert("게시글 조회 실패: " + err.responseText);
        }
    });
}

// 기존 게시글 이미지 불러오기
function getPostImages(postId) {
    $.ajax({
        url: `/posts/${postId}/images`,
        method: 'GET',
        success: function(images) {
            // order_num 기준 정렬
            existingImages = images.sort((a, b) => a.orderNum - b.orderNum);

            // 기존 이미지 표시
            $("#image_preview_container").empty();
            $("#file_names_container").empty();

            existingImages.forEach(img => {
                const wrapper = $(`
                    <div class="preview-wrapper existing" data-image-id="${img.imageId}">
                        <img src="${img.imageUrl}" class="preview-image">
                        <button type="button" class="remove-image-btn">&times;</button>
                    </div>
                `);
                $("#image_preview_container").append(wrapper);

                const fileNameItem = $("<div>")
                    .addClass("file-name-item")
                    .text(img.originalFileName)
                    .data("fileName", img.originalFileName);
                $("#file_names_container").append(fileNameItem);
            });

            // 모두 삭제 버튼 표시
            if(existingImages.length > 0) {
                $("#remove_all_images_btn").css("display", "inline-block");
            }
        },
        error: function(err) {
            alert("이미지 조회 실패: " + err.responseText);
        }
    });
}

//**************************************************************게시글 조회 End******************************************************************************** */

//**************************************************************이미지 생성 Start********************************************************************************* */
function image_visible() {
    $("#images").on("change", function (event) {
        const newFiles = Array.from(event.target.files);

        newFiles.forEach((file) => {
            // 기존 이미지 + 새로 선택한 이미지 합산해서 maxCount 체크
            const totalImagesCount = files.length + existingImages.length;
            if (totalImagesCount >= maxCount) {
                alert(`최대 ${maxCount}개까지 업로드 가능합니다. 현재 ${existingImages.length}개가 이미 존재합니다.`);
                return;
            }

            // 중복 체크: 새 파일 vs 새 파일, 새 파일 vs 기존 이미지
            const isDuplicate = files.some(f => f.name === file.name) || 
                                existingImages.some(img => img.originalFileName === file.name);

            if (isDuplicate) {
                alert(`이미 업로드되었거나 기존 게시글에 존재하는 파일 이름입니다: ${file.name}`);
                return;
            }

            // 파일 배열에 추가
            files.push(file);

            const reader = new FileReader();
            reader.onload = function (e) {
                // 미리보기 wrapper 생성
                const wrapper = $("<div>").addClass("preview-wrapper").data("file", file);
                const img = $("<img>").attr("src", e.target.result).addClass("preview-image");
                const removeBtn = $("<button>").attr("type","button").addClass("remove-image-btn").html("&times;");

                wrapper.append(img).append(removeBtn);
                allPreviews.push(wrapper);
                $("#image_preview_container").append(wrapper);

				// **파일 이름 갱신**
				updateFileNameList();

                // 이미지가 있으면 모두 삭제 버튼 표시
                if (files.length + existingImages.length > 0) {
                    $("#remove_all_images_btn").css("display", "inline-block");
                }

				// 새 이미지가 추가되면 sortable 새로 고침
				$("#image_preview_container").sortable("refresh");
            };
            reader.readAsDataURL(file);
        });

        // input[type=file] 실제 파일 업데이트
        const dt = new DataTransfer();
        files.forEach(f => dt.items.add(f));
        $("#images")[0].files = dt.files;
    });
}

//**************************************************************이미지 생성 End********************************************************************************* */
//**************************************************************게시글 수정 Start******************************************************************************** */
$(document).on("submit", "#post_update_form", function(e) {
    e.preventDefault();

    const formData = new FormData(this); // form 태그 기반으로 자동 수집

    ajaxWithToken({
        url: `/posts/${postId}`,   // postId는 URL에서 추출해야 함
        type: "PATCH",             // @PatchMapping 매핑
        data: formData,
        processData: false,        // FormData 그대로 전송
        contentType: false,
        success: function(response) {
			var responseBoardId = response.boardId;
            alert("게시글이 수정되었습니다.");
            window.location.href = `/board/${responseBoardId}/normal/${postId}`;
        },
        error: function(xhr) {
            alert("게시글 수정 실패: " + xhr.responseText);
        }
    });
});

// ========================= 드래그&드롭 이미지 순서 변경 =========================
function image_orderNumber_change() {
	$("#image_preview_container").sortable({
	    items: ".preview-wrapper",
	    cursor: "move",
	    opacity: 0.7,
	    update: function(event, ui) {
	        // 순서 변경 후 서버에 전송
	        const orderList = [];
	        $("#image_preview_container .preview-wrapper").each(function(index) {
	            const wrapper = $(this);
	            
	            if(wrapper.hasClass("existing")) {
	                const imageId = wrapper.data("image-id");
	                orderList.push({
	                    imageId: imageId,
	                    orderNum: index
	                });
	            }
	        });

	        if(orderList.length > 0) {
	            $.ajax({
	                url: `/posts/${postId}/images/order`,
	                type: "PATCH",
	                contentType: "application/json",
	                headers: { 'Authorization': `Bearer ${token}` },
	                data: JSON.stringify(orderList),
	                success: function(response) {
	                    console.log("이미지 순서가 저장되었습니다.");
	                },
	                error: function(xhr) {
	                    alert("이미지 순서 저장 실패: " + xhr.responseText);
	                }
	            });
	        }
	    }
	});
}

//**************************************************************게시글 수정 End******************************************************************************** */

//**************************************************************게시글 삭제 Start******************************************************************************** */

// 모든 이미지 삭제 버튼 클릭 처리 (DB + 화면 동시 반영)
$(document).on("click", "#remove_all_images_btn", function() {
    if (!confirm("정말로 모든 이미지를 삭제하시겠습니까?")) return;

    $.ajax({
        url: `/posts/${postId}/images`,  // 전체 삭제 API
        type: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` },
        success: function() {
            // 기존 코드 재사용
            files = [];
            allPreviews = [];
            existingImages = [];

            $("#image_preview_container").empty();
            $("#file_names_container").empty();
            $("#images")[0].value = "";

            $(this).hide(); // 버튼 숨기기

            alert("모든 이미지가 삭제되었습니다.");
        },
        error: function(xhr) {
            alert("이미지 삭제 실패: " + xhr.responseText);
        }
    });
});

// 이미지 삭제 버튼 클릭 처리 (새 파일 / 기존 이미지 구분)
$(document).on("click", ".remove-image-btn", function() {
    const wrapper = $(this).closest(".preview-wrapper");

	if(wrapper.hasClass("existing")) {
	    const imageId = wrapper.data("image-id");

	    if(confirm("정말로 이미지를 삭제하시겠습니까?")) {
	        ajaxWithToken({
	            url: `/posts/${postId}/images/${imageId}`,
	            type: 'DELETE',
	            success: function() {
	                // 배열에서 제거
	                existingImages = existingImages.filter(img => img.imageId !== imageId);

	                // 미리보기 wrapper 제거
	                wrapper.remove();

	                // **파일 이름 갱신**
	                updateFileNameList();

	                if ($("#image_preview_container .preview-wrapper").length === 0) {
	                    $("#remove_all_images_btn").hide();
	                }
	                alert("이미지가 삭제되었습니다.");
	            }
	        });
	    }
	} else {
        // 새로 선택한 파일 삭제
        const file = wrapper.data("file");

        files = files.filter(f => f !== file);
        allPreviews = allPreviews.filter(w => w.data("file") !== file);

        wrapper.remove();
		// **파일 이름 갱신**
		updateFileNameList();
        const dt = new DataTransfer();
        files.forEach(f => dt.items.add(f));
        $("#images")[0].files = dt.files;

        if(files.length === 0 && existingImages.length === 0) {
            $("#remove_all_images_btn").hide();
        }
    }
});
//**************************************************************게시글 삭제 End******************************************************************************** */
$(document).ready(function() {
	getPostDetail();
	image_visible();
	image_orderNumber_change();
});