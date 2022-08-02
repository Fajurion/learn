package com.fajurion.learn.controller.post.comment;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.Post;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.comments.CommentResponse;
import com.fajurion.learn.repository.post.comments.CommentService;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/post/comment")
public class CommentController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting post data
    private final PostRepository postRepository;

    // Service for getting comments
    private final CommentService commentService;

    @Autowired
    public CommentController(SessionService sessionService,
                                     CommentService commentService,
                                     PostRepository postRepository) {
        this.sessionService = sessionService;
        this.commentService = commentService;
        this.postRepository = postRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<CommentListResponse> list(@RequestBody CommentListForm form) {

        // Check if form is valid
        if(form.token() == null || form.offset() < 0) {
            return Mono.just(new CommentListResponse(false, false, "empty", null));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if post exists
            return postRepository.findById(form.post()).onErrorReturn(new Post(-1, -1, -1, -1, "", ""));
        }).flatMap(post -> {

            if(post == null) {
                return Mono.error(new CustomException("not_found"));
            }

            // Get comments
            return commentService.sortComments(form.post(), 20, form.offset());
        }).map(comments -> {

            // Turn into response
            return new CommentListResponse(true, false, "success", comments);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new CommentListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new CommentListResponse(false, true, "server.error", null));
    }

    // Form for requesting comment list
    public record CommentListForm(String token, int post, int offset) {}

    // Response to requesting a list of comments for a post
    public record CommentListResponse(boolean success, boolean error, String message, List<CommentResponse> comments) {}

}
