package com.fajurion.learn.repository.post;

import com.fajurion.learn.repository.account.Account;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.post.likes.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    // Repository for getting post data
    private final PostRepository postRepository;

    // Repository for getting likes
    private final LikeRepository likeRepository;

    // Repository for getting account
    private final AccountRepository accountRepository;

    @Autowired
    public PostService(PostRepository postRepository, LikeRepository likeRepository, AccountRepository accountRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.accountRepository = accountRepository;
    }

    public Mono<List<PostResponse>> getPosts(int topicID, int currentScroll, int userID, int filter, String query) {

        // Check if query exists
        return (query != null && !query.equals("") ?

                // Search posts
                filter == 0 ? postRepository.searchPostsByLikes(query, topicID, 7, currentScroll) : postRepository.searchPostsByDate(query, topicID, 7, currentScroll)

                // Get normal list
                : filter == 0 ? postRepository.sortPostsByLikes(topicID, 7, currentScroll) : postRepository.sortPostsByDate(topicID, 7, currentScroll))
                .flatMap(post -> Mono.zip(likeRepository.getLikeByPostAndAccount(post.getId(), userID).hasElement(), Mono.just(post),
                        accountRepository.findById(post.getCreator()).onErrorReturn(new Account("", "", "", "", "", -1)),
                        Mono.just(userID)))
                .collectList().map(list -> {

            // List for the posts
            ArrayList<PostResponse> postList = new ArrayList<>();

            // Add the like status to all the posts
            for(Tuple4<Boolean, Post, Account, Integer> tuple4 : list) {
                if(tuple4.getT3().getInvitor() == -1) continue;
                tuple4.getT2().setContent(shortenContent(tuple4.getT2().getContent(), 10));

                postList.add(new PostResponse(tuple4.getT2(), tuple4.getT1(), tuple4.getT4(), tuple4.getT3()));
            }

            // Return the posts
            return postList;
        });
    }

    private String shortenContent(String content, int maxLines) {
        StringBuilder newContent = new StringBuilder();

        int lines = 0;
        for(String s : content.split("\n")) {
            if(lines > maxLines) {
                newContent.append("\n ...");
                break;
            }

            newContent.append(s).append("\n");
            lines += Math.max(1, s.length() / 100);
        }

        return newContent.toString();
    }

}
