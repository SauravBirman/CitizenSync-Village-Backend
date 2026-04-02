package com.citizensync.backend.service;

import com.citizensync.backend.dto.IssueCommentRequest;
import com.citizensync.backend.dto.IssueCreateRequest;
import com.citizensync.backend.entity.Issue;
import com.citizensync.backend.entity.IssueComment;
import com.citizensync.backend.entity.IssueVote;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.IssueCommentRepository;
import com.citizensync.backend.repository.IssueRepository;
import com.citizensync.backend.repository.IssueVoteRepository;
import com.citizensync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueVoteRepository issueVoteRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MediaStorageService mediaStorageService;

    @Transactional
    public Issue submitIssue(IssueCreateRequest request, User currentUser) {
        return submitIssue(request, currentUser, null);
    }

    @Transactional
    public Issue submitIssue(IssueCreateRequest request, User currentUser, MultipartFile mediaFile) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new RuntimeException("Description is required");
        }

        Issue issue = new Issue();
        issue.setDescription(request.getDescription());
        issue.setVillage(resolveVillage(request.getVillage(), currentUser));
        issue.setVillageId(currentUser.getVillageId());
        issue.setPanchayatId(currentUser.getPanchayatId());
        issue.setPanchayat(currentUser.getPanchayat());
        issue.setSubdistrictId(currentUser.getSubdistrictId());
        issue.setSubdistrict(currentUser.getSubdistrict());
        issue.setDistrictId(currentUser.getDistrictId());
        issue.setDistrict(currentUser.getDistrict());
        issue.setStateId(currentUser.getStateId());
        issue.setState(currentUser.getState());
        issue.setStatus((request.getStatus() == null || request.getStatus().isBlank()) ? "Open" : request.getStatus());
        issue.setPriority((request.getPriority() == null || request.getPriority().isBlank())
                ? prioritizeIssue(request.getDescription())
                : request.getPriority());
        issue.setAddress(request.getAddress());
        issue.setLat(request.getLat());
        issue.setLng(request.getLng());
        issue.setReportedByEmail(currentUser.getEmail());
        issue.setReportedByName(currentUser.getName());
        issue.setReportedByAadhaar(currentUser.getAadhar());

        Issue saved = issueRepository.save(issue);

        if (mediaFile != null && !mediaFile.isEmpty()) {
            MediaStorageService.StoredMedia storedMedia = mediaStorageService.storeEncryptedMedia(mediaFile, saved.getId());
            saved.setMediaFileName(storedMedia.fileName());
            saved.setMediaContentType(storedMedia.contentType());
            saved.setMediaSizeBytes(storedMedia.sizeBytes());
            saved.setMediaStoragePath(storedMedia.storagePath());
            saved.setMediaEncrypted(storedMedia.encrypted());
            saved = issueRepository.save(saved);
        }

        notificationService.createNotificationForEmail(
                currentUser.getEmail(),
                saved.getId(),
                "Your issue \"" + saved.getDescription() + "\" has been reported."
        );

        return saved;
    }

    @Transactional
    public List<Issue> getIssues(String village, String status) {
        List<Issue> issues;

        if (village != null && !village.isBlank() && status != null && !status.isBlank()) {
            issues = issueRepository.findByVillageAndStatusOrderByRegisteredAtDesc(village, status);
        } else if (village != null && !village.isBlank()) {
            issues = issueRepository.findByVillageOrderByRegisteredAtDesc(village);
        } else if (status != null && !status.isBlank()) {
            issues = issueRepository.findByStatusOrderByRegisteredAtDesc(status);
        } else {
            issues = issueRepository.findAll();
            issues.sort(Comparator.comparing(Issue::getRegisteredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        processIssueLifecycle(issues);
        return issues;
    }

    @Transactional
    public void deleteIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        issueRepository.delete(issue);
    }

    public Issue getIssueById(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
    }

    @Transactional
    public Issue voteIssue(Long issueId, String voteType, User currentUser) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        if (!"urgent".equalsIgnoreCase(voteType) && !"notUrgent".equalsIgnoreCase(voteType)) {
            throw new RuntimeException("voteType must be 'urgent' or 'notUrgent'");
        }

        issueVoteRepository.findByIssueAndVoterEmailAndVoteCategory(issue, currentUser.getEmail(), "URGENCY")
                .ifPresent(v -> {
                    throw new RuntimeException("User has already voted");
                });

        IssueVote vote = new IssueVote();
        vote.setIssue(issue);
        vote.setVoterEmail(currentUser.getEmail());
        vote.setVoteCategory("URGENCY");
        vote.setVoteType(voteType);
        issueVoteRepository.save(vote);

        issue.setUrgentVotes((int) issueVoteRepository.countByIssueAndVoteCategoryAndVoteType(issue, "URGENCY", "urgent"));
        issue.setNotUrgentVotes((int) issueVoteRepository.countByIssueAndVoteCategoryAndVoteType(issue, "URGENCY", "notUrgent"));

        return issueRepository.save(issue);
    }

    public boolean hasVoted(Long issueId, User currentUser) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        return issueVoteRepository.findByIssueAndVoterEmailAndVoteCategory(issue, currentUser.getEmail(), "URGENCY")
                .isPresent();
    }

    @Transactional
    public Issue voteSatisfaction(Long issueId, String voteType, User currentUser) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        if (!"satisfied".equalsIgnoreCase(voteType) && !"unsatisfied".equalsIgnoreCase(voteType)) {
            throw new RuntimeException("voteType must be 'satisfied' or 'unsatisfied'");
        }

        issueVoteRepository.findByIssueAndVoterEmailAndVoteCategory(issue, currentUser.getEmail(), "SATISFACTION")
                .ifPresent(v -> {
                    throw new RuntimeException("User has already voted on satisfaction");
                });

        IssueVote vote = new IssueVote();
        vote.setIssue(issue);
        vote.setVoterEmail(currentUser.getEmail());
        vote.setVoteCategory("SATISFACTION");
        vote.setVoteType(voteType);
        issueVoteRepository.save(vote);

        int satisfiedVotes = (int) issueVoteRepository.countByIssueAndVoteCategoryAndVoteType(issue, "SATISFACTION", "satisfied");
        int unsatisfiedVotes = (int) issueVoteRepository.countByIssueAndVoteCategoryAndVoteType(issue, "SATISFACTION", "unsatisfied");

        issue.setSatisfiedVotes(satisfiedVotes);
        issue.setUnsatisfiedVotes(unsatisfiedVotes);

        if (unsatisfiedVotes > satisfiedVotes) {
            userRepository.findByRole("Sarpanch").stream()
                    .filter(u -> issue.getVillage() != null && issue.getVillage().equalsIgnoreCase(u.getSelectedVillage()))
                    .findFirst()
                    .ifPresent(sarpanch -> notificationService.createNotificationForEmail(
                            sarpanch.getEmail(),
                            issue.getId(),
                            "Issue \"" + issue.getDescription() + "\" in " + issue.getVillage()
                                    + " has more unsatisfied votes (" + unsatisfiedVotes + ") than satisfied votes (" + satisfiedVotes + ")."
                    ));
        }

        return issueRepository.save(issue);
    }

    public boolean hasVotedSatisfaction(Long issueId, User currentUser) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        return issueVoteRepository.findByIssueAndVoterEmailAndVoteCategory(issue, currentUser.getEmail(), "SATISFACTION")
                .isPresent();
    }

    @Transactional
    public Issue resolveIssue(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        LocalDateTime solvedAt = LocalDateTime.now();
        issue.setStatus("Solved");
        issue.setSolvedAt(solvedAt);
        issue.setSatisfiedVotes(0);
        issue.setUnsatisfiedVotes(0);
        issue.setSatisfactionEvaluationDue(solvedAt.plusDays(2));

        Issue saved = issueRepository.save(issue);

        if (saved.getReportedByEmail() != null) {
            notificationService.createNotificationForEmail(
                    saved.getReportedByEmail(),
                    saved.getId(),
                    "Issue \"" + saved.getDescription() + "\" has been marked as Solved."
            );
        }

        return saved;
    }

    @Transactional
    public IssueComment addComment(Long issueId, IssueCommentRequest request, User currentUser) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("Comment message is required");
        }

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setMessage(request.getMessage());
        comment.setUserEmail(currentUser.getEmail());
        comment.setUserName(currentUser.getName());
        comment.setUserAadhaar(currentUser.getAadhar());

        return issueCommentRepository.save(comment);
    }

    public List<IssueComment> getComments(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        return issueCommentRepository.findByIssueOrderByTimestampAsc(issue);
    }

    @Transactional
    public Map<String, Object> getIssueAnalytics(String village) {
        List<Issue> villageIssues = getIssues(village, null);
        long openCount = villageIssues.stream().filter(i -> "Open".equalsIgnoreCase(i.getStatus())).count();
        long solvedCount = villageIssues.stream().filter(i -> "Solved".equalsIgnoreCase(i.getStatus())).count();

        List<Map<String, Object>> topVoted = villageIssues.stream()
                .filter(i -> "Open".equalsIgnoreCase(i.getStatus()))
                .sorted(Comparator.comparingInt(Issue::getUrgentVotes).reversed())
                .limit(3)
                .map(issue -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", issue.getId());
                    data.put("description", issue.getDescription());
                    data.put("village", issue.getVillage());
                    data.put("status", issue.getStatus());
                    data.put("priority", issue.getPriority());
                    data.put("urgentVotes", issue.getUrgentVotes());
                    data.put("notUrgentVotes", issue.getNotUrgentVotes());
                    data.put("commentCount", issueCommentRepository.countByIssue(issue));
                    return data;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("openCount", openCount);
        response.put("solvedCount", solvedCount);
        response.put("topVoted", topVoted);

        return response;
    }

    private String prioritizeIssue(String description) {
        String text = description.toLowerCase();
        List<String> urgentKeywords = List.of("urgent", "hazard", "emergency", "water");
        return urgentKeywords.stream().anyMatch(text::contains) ? "High" : "Normal";
    }

    private String resolveVillage(String villageFromRequest, User currentUser) {
        if (villageFromRequest != null && !villageFromRequest.isBlank()) {
            return villageFromRequest;
        }

        if (currentUser.getSelectedVillage() != null && !currentUser.getSelectedVillage().isBlank()) {
            return currentUser.getSelectedVillage();
        }

        if (currentUser.getVillage() != null && !currentUser.getVillage().isBlank()) {
            return currentUser.getVillage();
        }

        throw new RuntimeException("Village is required");
    }

    private void processIssueLifecycle(List<Issue> issues) {
        List<Issue> toDelete = new ArrayList<>();

        for (Issue issue : issues) {
            if ("Open".equalsIgnoreCase(issue.getStatus())
                    && issue.getEscalatedTo() == null
                    && issue.getRegisteredAt() != null
                    && issue.getRegisteredAt().plusDays(5).isBefore(LocalDateTime.now())) {

                issue.setEscalatedTo("Tehsil Officer");
                issueRepository.save(issue);

                userRepository.findByRole("Tehsil Officer").stream().findFirst()
                        .ifPresent(tehsilOfficer -> notificationService.createNotificationForEmail(
                                tehsilOfficer.getEmail(),
                                issue.getId(),
                                "Issue \"" + issue.getDescription() + "\" in " + issue.getVillage()
                                        + " has been escalated to you due to Sarpanch inaction."
                        ));
            }

            if ("Solved".equalsIgnoreCase(issue.getStatus())
                    && issue.getSatisfactionEvaluationDue() != null
                    && !issue.getSatisfactionEvaluationDue().isAfter(LocalDateTime.now())) {

                int satisfiedVotes = issue.getSatisfiedVotes() == null ? 0 : issue.getSatisfiedVotes();
                int unsatisfiedVotes = issue.getUnsatisfiedVotes() == null ? 0 : issue.getUnsatisfiedVotes();

                if (unsatisfiedVotes >= satisfiedVotes) {
                    userRepository.findByRole("Sarpanch").stream()
                            .filter(u -> issue.getVillage() != null && issue.getVillage().equalsIgnoreCase(u.getSelectedVillage()))
                            .findFirst()
                            .ifPresent(sarpanch -> notificationService.createNotificationForEmail(
                                    sarpanch.getEmail(),
                                    issue.getId(),
                                    "After 2 days, issue \"" + issue.getDescription() + "\" in " + issue.getVillage()
                                            + " has " + unsatisfiedVotes + " unsatisfied votes, which is greater than or equal to "
                                            + satisfiedVotes + " satisfied votes."
                            ));

                    issue.setSatisfactionEvaluationDue(null);
                    issueRepository.save(issue);
                } else {
                    toDelete.add(issue);
                }
            }
        }

        if (!toDelete.isEmpty()) {
            issueRepository.deleteAll(toDelete);
            issues.removeAll(toDelete);
        }
    }
}
