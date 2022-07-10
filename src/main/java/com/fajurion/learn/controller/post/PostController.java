package com.fajurion.learn.controller.post;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.Post;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.likes.Like;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/post")
public class PostController {

    // Repository for accessing posts
    private final PostRepository postRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for accessing topics
    private final LikeRepository likeRepository;

    @Autowired
    public PostController(PostRepository postRepository,
                          SessionService sessionService,
                          LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.sessionService = sessionService;
        this.likeRepository = likeRepository;
    }

    /**
     * Endpoint for listing posts (sorted by likes)
     *
     * @param form Post list form
     * @return List of posts with additional information
     */
    @RequestMapping("/list")
    @ResponseBody @CrossOrigin
    public Mono<PostListResponse> list(@RequestBody PostListForm form) {

        // Check if request is valid
        if(form.currentScroll() < 0) {
            return Mono.just(new PostListResponse(false, true, "server.error", new ArrayList<>()));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // List the posts
            return postRepository.sortPostsByLikes(form.topic(), 20, form.currentScroll());
        }).flatMap(list -> {

            if(list == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Return the response
            return Mono.just(new PostListResponse(true, false, "success", list));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new PostListResponse(false, false, e.getMessage(), new ArrayList<>())))
                .onErrorResume(e -> Mono.just(new PostListResponse(false, true, "server.error", new ArrayList<>())));
    }

    // Record for post list form
    public record PostListForm(String token, int topic, int currentScroll) {}

    // Record for post list response
    public record PostListResponse(boolean success, boolean error, String message, ArrayList<Post> posts) {}

    /**
     * Endpoint for liking posts
     *
     * @param form Post like form
     * @return Response to the like form
     */
    @RequestMapping("/like")
    @ResponseBody @CrossOrigin
    public Mono<PostLikeResponse> like(@RequestBody PostLikeForm form) {

        // Reference for user identifier
        AtomicReference<Integer> userID = new AtomicReference<>();

        // Reference for post to like
        AtomicReference<Post> postToLike = new AtomicReference<>();

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Set user id
            userID.set(session.getId());

            // Check if the post exists
            return postRepository.findById(form.post());
        }).flatMap(post -> {

            if(post == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Set post to like
            postToLike.set(post);

            // Check if the post has already been liked
            return likeRepository.findById(userID.get());
        }).flatMap(like -> {

            if(like != null) {
                return Mono.error(new RuntimeException("already.liked"));
            }

            // Like the post
            Post post = postToLike.get();
            post.setLikes(post.getLikes() + 1);

            // Update post
            postToLike.set(post);

            // Update database
            return Mono.zip(likeRepository.save(new Like(userID.get(), form.post())), postRepository.save(postToLike.get()));
        }).flatMap(response -> {

            if(response == null) {
                return Mono.error(new RuntimeException("server.error"));
            }

            // Return response
            return Mono.just(new PostLikeResponse(true, false, "success"));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new PostLikeResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new PostLikeResponse(false, true, "server.error")));
    }

    // Record for post like form
    public record PostLikeForm(String token, int post) {}

    // Record for post like response
    public record PostLikeResponse(boolean success, boolean error, String message) {}

}
