package com.back.devc.domain.post.post.controller;

import com.back.devc.domain.post.post.dto.AdminPostDetailResponse;
import com.back.devc.domain.post.post.dto.AdminPostListResponse;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.service.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @GetMapping("")
    public ResponseEntity<List<AdminPostListResponse>> list(
    ) {
        List<Post> result = adminPostService.findAll();

        List<AdminPostListResponse> postDtoList = result.stream()
                .map(AdminPostListResponse::new)
                .toList();

        return ResponseEntity.ok(postDtoList);
    }

    @GetMapping("/{postid}")
    public ResponseEntity<AdminPostDetailResponse> detail(
            @PathVariable Long postid
    ) {
        return ResponseEntity.ok(adminPostService.findDetailById(postid));
    }
}
