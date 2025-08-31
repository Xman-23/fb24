// 1. 댓글 가져오기 (통합 검색 부모, 자식 게시글에 공통으로 빼기)
function loadComments(sortBy = 'recent', page = 0) {
    $.ajax({
        url: `/comments/post/${postId}?sortBy=${sortBy}&page=${page}`,
        type: 'GET',
        success: function(response) {
        	console.log(response);
        	$("#post_comments").empty();
            $('#post_comments').text(`${response.activeTotalElements}`);
            renderComments(response.comments, $('#comments_list'));
        },
        error: function(err) {
            console.error(err);
            alert("댓글을 불러오는 중 에러가 발생했습니다.");
        }
    });
}

// 2. 댓글 트리 렌더링
function renderComments(comments, comments_list) {
	console.log("main_post renderComments() Start");
	comments_list.empty();
    comments.forEach(comment => {
        var commentElem = createCommentElem(comment);
        comments_list.append(commentElem);
    });
}

// 댓글 요소 생성 (재귀로 대댓글까지 처리)
function createCommentElem(comment) {
    var commentDiv = $(`
        <div class="comment" data_comment_id="${comment.commentId}">
            <div class="comment_header">
                <span class="comment_author">${comment.authorNickname}</span>
                <span class="comment_created">${comment.updatedAgo || comment.createdAt}</span>
                <span class="comment_actions">
                    <button class="comment_btn_reply">답글</button>
                    <button class="comment_btn_edit">수정</button>
                    <button class="comment_btn_report">신고</button>
                    <button class="comment_btn_like">👍 <span class="comment_like_count">${comment.likeCount}</span></button>
                    <button class="comment_btn_dislike">👎 <span class="comment_dislike_count">${comment.dislikeCount}</span></button>
                </span>
            </div>
            <div class="comment_content">${comment.content}</div>
            <div class="comments_child"></div>
        </div>
    `);

    // 대댓글 재귀
    if(comment.childComments && comment.childComments.length > 0) {
        var childContainer = commentDiv.find('.comments_child');
        comment.childComments.forEach(child => {
            childContainer.append(createCommentElem(child));
        });
    }

    // 좋아요/싫어요 클릭 이벤트
    commentDiv.find('.comment_btn_like').click(function() {
        handleReaction(comment.commentId, 'LIKE', $(this));
    });
    commentDiv.find('.comment_btn_dislike').click(function() {
        handleReaction(comment.commentId, 'DISLIKE', $(this));
    });

    return commentDiv;
}
// 댓글 리액션 처리
// 댓글 리액션 처리
function handleReaction(commentId, type, buttonElem) {
    ajaxWithToken({
        url: `/commentreactions/${commentId}/reaction`,
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ commentReactionType: type }),
        success: function(res) {
            // 좋아요/싫어요 숫자 덮어쓰기
            buttonElem.closest('.comment-header').find('.comment_like_count').text(res.likeCount);
            buttonElem.closest('.comment-header').find('.comment_dislike_count').text(res.dislikeCount);

            // 내가 누른 상태 강조
            if(res.userCommnetReactionType === 'LIKE') {
                buttonElem.closest('.comment-header').find('.comment_btn_like').css('font-weight','bold');
                buttonElem.closest('.comment-header').find('.comment_btn_dislike').css('font-weight','normal');
            } else if(res.userCommnetReactionType === 'DISLIKE') {
                buttonElem.closest('.comment-header').find('.comment_btn_dislike').css('font-weight','bold');
                buttonElem.closest('.comment-header').find('.comment_btn_like').css('font-weight','normal');
            } else {
                buttonElem.closest('.comment-header').find('.comment_btn_like, .comment_btn_dislike').css('font-weight','normal');
            }
        },
        error: function(xhr) {
            console.error(xhr);
            alert("리액션 처리 중 에러가 발생했습니다: " + xhr.responseText);
        }
    });
}