package com.fajurion.learn.controller.post;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.Post;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.PostResponse;
import com.fajurion.learn.repository.post.PostService;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/post")
public class PostOverviewController {

    // Repository for accessing posts
    private final PostRepository postRepository;

    // Service for getting more post data
    private final PostService postService;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for accessing topics
    private final LikeRepository likeRepository;

    @Autowired
    public PostOverviewController(PostRepository postRepository,
                          SessionService sessionService,
                          LikeRepository likeRepository,
                          PostService postService) {
        this.postRepository = postRepository;
        this.sessionService = sessionService;
        this.likeRepository = likeRepository;
        this.postService = postService;
    }

    @PostMapping("/info")
    @CrossOrigin
    public Mono<PostInfoResponse> info(@RequestBody PostInfoForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new PostInfoResponse(false, false, "empty", null));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get post and check if it exists (and zip with other needed data)
            return Mono.zip(postRepository.findById(form.post()).onErrorReturn(new Post(-1, -1, -1, 1, "", "")),
                    likeRepository.getLikeByPostAndAccount(form.post(), session.getAccount()).hasElement());
        }).flatMap(tuple2 -> {

            // Check if post exists
            if(tuple2.getT1().getCreator() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Turn into post response and return
            return Mono.just(new PostInfoResponse(true, false, "success", new PostResponse(tuple2.getT1(), tuple2.getT2())));
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new PostInfoResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new PostInfoResponse(false, true, "server.error", null));
    }

    // Form for requesting post info
    public record PostInfoForm(String token, int post) {}

    // Response to post info request
    public record PostInfoResponse(boolean success, boolean error, String message, PostResponse info) {}

}
