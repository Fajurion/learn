package com.fajurion.learn.repository.post;

import com.fajurion.learn.repository.post.likes.Like;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PostService {

    // Repository for getting post data
    private final PostRepository postRepository;

    // Repository for getting likes
    private final LikeRepository likeRepository;

    @Autowired
    public PostService(PostRepository postRepository, LikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
    }

    public Mono<List<PostResponse>> getPostsByLikes(int topicID, int currentScroll, int userID) {

        // Get all posts sorted by likes with a limit of 10
        return postRepository.sortPostsByLikes(topicID, 7, currentScroll)
                .flatMap(post -> Mono.zip(likeRepository.getLikeByPostAndAccount(post.getId(), userID).hasElement(), Mono.just(post)))
                .collectList().map(list -> {

            // List for the posts
            ArrayList<PostResponse> postList = new ArrayList<>();

            // Add the like status to all the posts
            for(Tuple2<Boolean, Post> tuple2 : list) {
                postList.add(new PostResponse(tuple2.getT2(), tuple2.getT1()));
            }

            // Return the posts
            return postList;
        });
    }

}
