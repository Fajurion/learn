package com.fajurion.learn.controller.post.comment;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.comments.CommentRepository;
import com.fajurion.learn.util.Configuration;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/post/comment")
public class CommentDeletionController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting rank data
    private final RankRepository rankRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for deleting comments
    private final CommentRepository commentRepository;

    @Autowired
    public CommentDeletionController(SessionService sessionService,
                                     CommentRepository commentRepository,
                                     AccountRepository accountRepository,
                                     RankRepository rankRepository) {
        this.sessionService = sessionService;
        this.commentRepository = commentRepository;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
    }

    @PostMapping("/delete")
    @CrossOrigin
    public Mono<CommentDeleteResponse> delete(@RequestBody CommentDeleteForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new CommentDeleteResponse(false, false, "empty"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get the account of the user to check rank
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get the rank of the user
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check if rank has required permission level
            if(rank.getLevel() < Configuration.permissions.get("delete.comment")) {
                return Mono.error(new CustomException("no_permission"));
            }

            // Delete comment
            return commentRepository.deleteById(form.comment()).thenReturn(rank);
        }).map(rank -> {

            // Return response
            return new CommentDeleteResponse(true, false, "success");
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new CommentDeleteResponse(false, false, e.getMessage())))
                .onErrorReturn(new CommentDeleteResponse(false, true, "server.error"));
    }

    // Form for deleting a comment
    public record CommentDeleteForm(String token, int comment) {}

    // Response to delete a comment
    public record CommentDeleteResponse(boolean success, boolean error, String message) {}

}
