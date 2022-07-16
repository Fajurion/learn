package com.fajurion.learn.controller.post;

import com.fajurion.learn.controller.image.ImageController;
import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.Rank;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.Post;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/post")
public class PostCreationController {

    // Repository for accessing posts
    private final PostRepository postRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting accounts
    private final AccountRepository accountRepository;

    // Repository for getting ranks
    private final RankRepository rankRepository;

    // Repository for accessing topics
    private final TopicRepository topicRepository;

    @Autowired
    public PostCreationController(PostRepository postRepository, SessionService sessionService, AccountRepository accountRepository, RankRepository rankRepository, TopicRepository topicRepository) {
        this.postRepository = postRepository;
        this.sessionService = sessionService;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
        this.topicRepository = topicRepository;
    }

    @RequestMapping("/create")
    @ResponseBody @CrossOrigin
    public Mono<PostCreateResponse> create(@RequestBody PostCreateForm form) {

        // Check if content is too long
        if(form.content().length() > ConstantConfiguration.MAXIMUM_CHARACTERS_POST) {
            return Mono.just(new PostCreateResponse(false, false, "content.too_long"));
        }

        // Check if title is too long
        if(form.title().length() > ConstantConfiguration.MAXIMUM_CHARACTERS_POST_TITLE) {
            return Mono.just(new PostCreateResponse(false, false, "title.too_long"));
        }

        // Variable for saving rank
        AtomicReference<Rank> userRank = new AtomicReference<>();
        AtomicReference<Integer> userID = new AtomicReference<>();

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account to get rank data
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Set user id
            userID.set(account.getId());

            // Get rank
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Set rank
            userRank.set(rank);

            // Get topic
            return topicRepository.findById(form.topic());
        }).flatMap(topic -> {

            // Check if topic exists
            if(topic == null) {
                return Mono.error(new RuntimeException("topic.not_found"));
            }

            // Check if topic is locked
            if(topic.isLocked() && userRank.get().getLevel() < ConstantConfiguration.PERMISSION_LEVEL_CREATE_POST_LOCKED) {
                return Mono.error(new RuntimeException("topic.locked"));
            }

            // Create post
            return postRepository.save(new Post(topic.getId(), userID.get(), 0, System.currentTimeMillis(), form.title(), form.content()));
        }).flatMap(post -> {

            if(post == null) {
                return Mono.error(new RuntimeException("server.error"));
            }

            return Mono.just(new PostCreateResponse(true, false, "success"));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, e -> Mono.just(new PostCreateResponse(false, false, e.getMessage())))
                .onErrorResume(e -> Mono.just(new PostCreateResponse(false, true, "server.error")));
    }

    // Record for post create form
    public record PostCreateForm(String token, int topic, String title, String content) {}

    // Record for post create response
    public record PostCreateResponse(boolean success, boolean error, String message) {}

}
