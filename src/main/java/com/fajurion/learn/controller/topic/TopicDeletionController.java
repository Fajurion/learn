package com.fajurion.learn.controller.topic;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.post.PostRepository;
import com.fajurion.learn.repository.post.comments.CommentRepository;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/topic")
public class TopicDeletionController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for saving topics
    private final TopicRepository topicRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for getting rank permission level
    private final RankRepository rankRepository;

    // Repository for deleting comments
    private final CommentRepository commentRepository;

    // Repository for deleting posts
    private final PostRepository postRepository;

    @Autowired
    public TopicDeletionController(SessionService sessionService,
                                   TopicRepository topicRepository,
                                   AccountRepository accountRepository,
                                   RankRepository rankRepository,
                                   CommentRepository commentRepository,
                                   PostRepository postRepository) {
        this.sessionService = sessionService;
        this.topicRepository = topicRepository;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @RequestMapping("/delete")
    @ResponseBody @CrossOrigin
    public Mono<DeleteTopicResponse> deleteTopic(@RequestBody DeleteTopicForm topicForm) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(topicForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get account to check rank details
            return accountRepository.findById(session.getAccount());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Get rank
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check the permission level of the rank
            if(rank.getLevel() < ConstantConfiguration.PERMISSION_LEVEL_DELETE_TOPIC) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Check if topic exists
            return topicRepository.findById(topicForm.topic());
        }).flatMap(topic -> {

            if(topic == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Check if the topic is a parent topic
            if(topic.isCategory()) {
                return Mono.error(new RuntimeException("is_parent"));
            }

            // Delete comments and posts in topic
            return Mono.zip(commentRepository.deleteAllByTopic(topic.getId()),
                    postRepository.deleteAllByTopic(topic.getId()));
        }).flatMap(deletion -> {

            // Delete topic
            return topicRepository.deleteById(topicForm.topic());
        }).map(deletion -> new DeleteTopicResponse(true, false, "success"))

                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new DeleteTopicResponse(false, false, error.getMessage())))
                .onErrorResume(error -> Mono.just(new DeleteTopicResponse(false, true, "server.error")));
    }

    // Record for delete topics form
    public record DeleteTopicForm(String token, int topic) {}

    // Record for delete topics response
    public record DeleteTopicResponse(boolean success, boolean error, String message) {}


}
