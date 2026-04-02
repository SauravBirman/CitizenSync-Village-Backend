package com.citizensync.backend.controller;

import com.citizensync.backend.dto.IssueCommentRequest;
import com.citizensync.backend.dto.IssueCreateRequest;
import com.citizensync.backend.dto.IssueVoteRequest;
import com.citizensync.backend.entity.Issue;
import com.citizensync.backend.entity.IssueComment;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.service.CurrentUserService;
import com.citizensync.backend.service.IssueService;
import com.citizensync.backend.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final CurrentUserService currentUserService;
    private final MediaStorageService mediaStorageService;

    @PostMapping
    public Issue submitIssue(@RequestBody IssueCreateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return issueService.submitIssue(request, currentUser);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Issue submitIssueWithMedia(
            @RequestPart("issue") IssueCreateRequest request,
            @RequestPart(value = "media", required = false) MultipartFile media
    ) {
        User currentUser = currentUserService.getCurrentUser();
        return issueService.submitIssue(request, currentUser, media);
    }

    @GetMapping
    public List<Issue> getIssues(@RequestParam(required = false) String village,
                                 @RequestParam(required = false) String status) {
        return issueService.getIssues(village, status);
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<ByteArrayResource> getIssueMedia(@PathVariable("id") Long issueId) {
        Issue issue = issueService.getIssueById(issueId);
        if (issue.getMediaStoragePath() == null || issue.getMediaStoragePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = mediaStorageService.readDecryptedMedia(issue.getMediaStoragePath(), issue.isMediaEncrypted());
        MediaType mediaType;
        try {
            mediaType = issue.getMediaContentType() != null
                    ? MediaType.parseMediaType(issue.getMediaContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + (issue.getMediaFileName() == null ? "media" : issue.getMediaFileName()) + "\"")
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable("id") Long issueId) {
        issueService.deleteIssue(issueId);
    }

    @PostMapping("/{id}/votes")
    public Issue voteIssue(@PathVariable("id") Long issueId, @RequestBody IssueVoteRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return issueService.voteIssue(issueId, request.getVoteType(), currentUser);
    }

    @GetMapping("/{id}/votes/me")
    public Map<String, Boolean> hasVoted(@PathVariable("id") Long issueId) {
        User currentUser = currentUserService.getCurrentUser();
        return Map.of("voted", issueService.hasVoted(issueId, currentUser));
    }

    @PostMapping("/{id}/satisfaction-votes")
    public Issue voteSatisfaction(@PathVariable("id") Long issueId, @RequestBody IssueVoteRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return issueService.voteSatisfaction(issueId, request.getVoteType(), currentUser);
    }

    @GetMapping("/{id}/satisfaction-votes/me")
    public Map<String, Boolean> hasVotedSatisfaction(@PathVariable("id") Long issueId) {
        User currentUser = currentUserService.getCurrentUser();
        return Map.of("voted", issueService.hasVotedSatisfaction(issueId, currentUser));
    }

    @PatchMapping("/{id}/resolve")
    public Issue resolveIssue(@PathVariable("id") Long issueId) {
        return issueService.resolveIssue(issueId);
    }

    @PostMapping("/{id}/comments")
    public IssueComment addComment(@PathVariable("id") Long issueId, @RequestBody IssueCommentRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return issueService.addComment(issueId, request, currentUser);
    }

    @GetMapping("/{id}/comments")
    public List<IssueComment> getComments(@PathVariable("id") Long issueId) {
        return issueService.getComments(issueId);
    }

    @GetMapping("/analytics")
    public Map<String, Object> getIssueAnalytics(@RequestParam String village) {
        return issueService.getIssueAnalytics(village);
    }
}
