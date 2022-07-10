package com.fajurion.learn.controller.post.comment;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.comments.Comment;
import com.fajurion.learn.repository.post.comments.CommentRepository;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/post/comment")
public class CommentCreationController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting post data
    private final PostRepository postRepository;

    // Repository for creating comment
    private final CommentRepository commentRepository;

    @Autowired
    public CommentCreationController(SessionService sessionService,
                                     CommentRepository commentRepository,
                                     PostRepository postRepository) {
        this.sessionService = sessionService;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @RequestMapping("/create")
    @ResponseBody @CrossOrigin
    public Mono<CommentCreateResponse> create(@RequestBody CommentCreateForm form) {

        // Check if content fits requirements
        if(form.content().length() > ConstantConfiguration.MAXIMUM_CHARACTERS_COMMENT) {
            return Mono.just(new CommentCreateResponse(false, false, "too_long"));
        }

        // Reference for user identifier
        AtomicReference<Integer> userID = new AtomicReference<>();

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Save user ID
            userID.set(session.getId());

            // Check if post exists
            return postRepository.findById(form.post());
        }).flatMap(post -> {

            if(post == null) {
                return Mono.error(new RuntimeException("post.not_found"));
            }

            // Create comment
            return commentRepository.save(new Comment(userID.get(), form.content(), post.getId(), post.getTopic(), System.currentTimeMillis()));
        }).flatMap(comment -> {

            if(comment == null) {
                return Mono.error(new RuntimeException("server.error"));
            }

            // Return response
            return Mono.just(new CommentCreateResponse(true, false, "success"));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new CommentCreateResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new CommentCreateResponse(false, true, "server.error")));
    }

    // Record for comment create form
    public record CommentCreateForm(String token, String content, int post) {}

    // Record for comment create response
    public record CommentCreateResponse(boolean success, boolean error, String message) {}

}
