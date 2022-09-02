package com.fajurion.learn.controller.post;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.comments.CommentRepository;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import com.fajurion.learn.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/post")
public class PostDeletionController {

    // Repository for accessing posts
    private final PostRepository postRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting accounts
    private final AccountRepository accountRepository;

    // Repository for getting ranks
    private final RankRepository rankRepository;

    // Repository for deleting comments
    private final CommentRepository commentRepository;

    // Repository for deleting likes
    private final LikeRepository likeRepository;

    @Autowired
    public PostDeletionController(PostRepository postRepository,
                                  SessionService sessionService,
                                  AccountRepository accountRepository,
                                  RankRepository rankRepository,
                                  CommentRepository commentRepository,
                                  LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }


    @RequestMapping("/delete")
    @ResponseBody @CrossOrigin
    public Mono<DeletePostResponse> delete(@RequestBody DeletePostForm form) {

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account data to access rank
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Get rank of the account
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check if rank has required permissions
            if(rank.getLevel() < Configuration.permissions.get("delete.post")) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Delete post
            return postRepository.findById(form.post());
        }).flatMap(post -> {

            if(post == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Delete post
            return postRepository.delete(post).thenReturn(post);
        }).flatMap(v -> {

            // Delete comments related to post and likes
            return Mono.zip(commentRepository.deleteAllByPost(form.post()).thenReturn(v),
                    likeRepository.deleteAllByPost(form.post()).thenReturn(v));
        }).map(v -> new DeletePostResponse(true, false, "success"))

                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new DeletePostResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new DeletePostResponse(false, true, "server.error")));
    }

    // Record for delete post form
    public record DeletePostForm(String token, int post) {}

    // Record for delete post response
    public record DeletePostResponse(boolean success, boolean error, String message) {}

}
