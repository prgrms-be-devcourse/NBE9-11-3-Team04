package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentAttachmentSuccessCode implements SuccessCodeSpec {
    COMMENT_ATTACHMENT_201_UPLOAD(HttpStatus.CREATED, "COMMENT_ATTACHMENT_201_UPLOAD", "댓글 첨부파일 업로드 성공"),
    COMMENT_ATTACHMENT_200_LIST(HttpStatus.OK, "COMMENT_ATTACHMENT_200_LIST", "댓글 첨부파일 조회 성공"),
    COMMENT_ATTACHMENT_200_DELETE(HttpStatus.OK, "COMMENT_ATTACHMENT_200_DELETE", "댓글 첨부파일 삭제 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}