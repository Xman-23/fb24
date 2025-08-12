package com.example.demo.dto.comment.commentreport;

import com.example.demo.dto.comment.CommentResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportResponseDTO {

	private String message;
	private CommentResponseDTO comment;

}
