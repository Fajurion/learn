package com.fajurion.learn.controller.post;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.Post;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.PostResponse;
import com.fajurion.learn.repository.post.PostService;
import com.fajurion.learn.repository.post.likes.Like;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/post")
public class PostController {

    // Repository for accessing posts
    private final PostRepository postRepository;

    // Service for getting more post data
    private final PostService postService;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for accessing topics
    private final LikeRepository likeRepository;

    @Autowired
    public PostController(PostRepository postRepository,
                          SessionService sessionService,
                          LikeRepository likeRepository,
                          PostService postService) {
        this.postRepository = postRepository;
        this.sessionService = sessionService;
        this.likeRepository = likeRepository;
        this.postService = postService;
    }

    /**
     * Endpoint for listing posts (sorted by likes)
     *
     * @param form Post list form
     * @return List of posts with additional information
     */
    @PostMapping("/list")
    @CrossOrigin
    public Mono<PostListResponse> list(@RequestBody PostListForm form) {

        // Check if request is valid
        if(form.currentScroll() < 0 || form.filter() > 1 || form.filter() < 0) {
            return Mono.just(new PostListResponse(false, true, "server.error", new ArrayList<>()));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // List the posts
            return postService.getPosts(form.topic(), form.currentScroll(), session.getAccount(), form.filter(), form.query());
        }).flatMap(list -> {

            if(list == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Return the response
            return Mono.just(new PostListResponse(true, false, "success", (ArrayList<PostResponse>) list));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new PostListResponse(false, false, e.getMessage(), new ArrayList<>())))
                .onErrorResume(e -> Mono.just(new PostListResponse(false, true, "server.error", new ArrayList<>())));
    }

    // Record for post list form
    public record PostListForm(String token, String query, int topic, int currentScroll, int filter) {}

    // Record for post list response
    public record PostListResponse(boolean success, boolean error, String message, ArrayList<PostResponse> posts) {}

    /**
     * Endpoint for liking posts
     *
     * @param form Post like form
     * @return Response to the like form
     */
    @PostMapping("/like")
    @CrossOrigin
    public Mono<PostLikeResponse> like(@RequestBody PostLikeForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new PostLikeResponse(false, false, "empty"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if the post exists
            return Mono.zip(postRepository.findById(form.post()).onErrorReturn(new Post(-1, -1, -1, -1, "", "")), Mono.just(session.getAccount()));
        }).flatMap(tuple2 -> {

            if(tuple2.getT1().getCreator() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if the post has already been liked
            return Mono.zip(likeRepository.getLikeByPostAndAccount(form.post(), tuple2.getT2()).hasElement(),
                    Mono.just(tuple2.getT2()), Mono.just(tuple2.getT1()));
        }).flatMap(tuple -> {

            if(tuple.getT1()) {
                return Mono.error(new CustomException("already.liked"));
            }

            // Update post
            tuple.getT3().setLikes(tuple.getT3().getLikes() + 1);

            // Update database
            return Mono.zip(likeRepository.save(new Like(tuple.getT2(), form.post())), postRepository.save(tuple.getT3()));
        }).map(response -> {

            // Return response
            return new PostLikeResponse(true, false, "success");
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new PostLikeResponse(false, false, e.getMessage())))
                .onErrorReturn(new PostLikeResponse(false, true, "server.error"));
    }

    // Record for post like form
    public record PostLikeForm(String token, int post) {}

    // Record for post like response
    public record PostLikeResponse(boolean success, boolean error, String message) {}

    @PostMapping("/unlike")
    @CrossOrigin
    public Mono<PostLikeResponse> unlike(@RequestBody PostLikeForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new PostLikeResponse(false, false, "empty"));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Check if the post exists
            return Mono.zip(postRepository.findById(form.post()).onErrorReturn(new Post(-1, -1, -1, -1, "", "")), Mono.just(session.getAccount()));
        }).flatMap(tuple2 -> {

            if(tuple2.getT1().getCreator() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if the post hasn't been liked
            return Mono.zip(likeRepository.getLikeByPostAndAccount(form.post(), tuple2.getT2()).onErrorReturn(new Like(-1, -1)),
                    Mono.just(tuple2.getT2()), Mono.just(tuple2.getT1()));
        }).flatMap(tuple -> {

            if(tuple.getT1().getAccount() == -1) {
                return Mono.error(new CustomException("not.liked"));
            }

            // Update post
            tuple.getT3().setLikes(tuple.getT3().getLikes() - 1);

            // Update database
            return Mono.zip(likeRepository.delete(tuple.getT1()).thenReturn(new Like(-1, -1)), postRepository.save(tuple.getT3()));
        }).map(response -> {

            // Return response
            return new PostLikeResponse(true, false, "success");
        })
            // Error handling
            .onErrorResume(CustomException.class, e -> Mono.just(new PostLikeResponse(false, false, e.getMessage())))
            .onErrorReturn(new PostLikeResponse(false, true, "server.error"));
    }

}
