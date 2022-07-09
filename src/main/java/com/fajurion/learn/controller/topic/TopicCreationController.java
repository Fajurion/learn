package com.fajurion.learn.controller.topic;

import com.fajurion.learn.repository.account.AccountRepository;
import com.fajurion.learn.repository.account.ranks.RankRepository;
import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.topic.Topic;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.ConstantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/topic")
public class TopicCreationController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for saving topics
    private final TopicRepository topicRepository;

    // Repository for getting account data
    private final AccountRepository accountRepository;

    // Repository for getting rank permission level
    private final RankRepository rankRepository;

    @Autowired
    public TopicCreationController(SessionService sessionService, TopicRepository topicRepository, AccountRepository accountRepository, RankRepository rankRepository) {
        this.sessionService = sessionService;
        this.topicRepository = topicRepository;
        this.accountRepository = accountRepository;
        this.rankRepository = rankRepository;
    }

    @RequestMapping("/create")
    @ResponseBody @CrossOrigin
    public Mono<CreateTopicResponse> createTopic(@RequestBody CreateTopicForm topicForm) {

        // Check if name is valid
        if(topicForm.name().length() > 50) {
            return Mono.just(new CreateTopicResponse(false, false, "name_too_long"));
        }

        // Atomic reference for creator id
        AtomicReference<Integer> creatorID = new AtomicReference<>();

        // Check if session is valid
        return sessionService.checkAndRefreshSession(topicForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Check if the account has the required permissions
            return accountRepository.findById(session.getId());
        }).flatMap(account -> {

            if(account == null) {
                return Mono.error(new RuntimeException("session.expired.deleted"));
            }

            // Set creator ID
            creatorID.set(account.getId());

            // Get the rank of the account
            return rankRepository.getRankByName(account.getRank());
        }).flatMap(rank -> {

            // Check if the rank has the required permission level
            if(rank.getLevel() < ConstantConfiguration.PERMISSION_LEVEL_CREATE_TOPIC) {
                return Mono.error(new RuntimeException("no_permission"));
            }

            // Get the parent topic
            return topicRepository.findById(topicForm.parent());
        }).flatMap(topic -> {

            // Check if parent topic exists
            if(topic == null) {
                return Mono.error(new RuntimeException("no_parent"));
            }

            // Make parent aware
            topic.setCategory(true);

            // Save topic
            return topicRepository.save(topic);
        }).flatMap(topic -> {

            // Check if it worked
            if(topic == null) {
                return Mono.error(new RuntimeException("server.error"));
            }

            // Check if parent has too many topics
            return topicRepository.countTopicsByParent(topic.getId());
        }).flatMap(count -> {

            // Check if there are too many topics
            if(count >= 100) {
                return Mono.error(new RuntimeException("too_many_topics"));
            }

            // Create topic
            return topicRepository.save(new Topic(topicForm.parent(), topicForm.name(), creatorID.get(), false, false));
        }).flatMap(topic -> {

            if(topic == null) {
                return Mono.error(new RuntimeException("server.error"));
            }

            // Return create topic response
            return Mono.just(new CreateTopicResponse(true, false, "success"));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new CreateTopicResponse(false, false, error.getMessage())))
                .onErrorResume(error -> Mono.just(new CreateTopicResponse(false, true, "server.error")));
    }

    // Record for creating topic form
    public record CreateTopicForm(String token, String name, int parent) {}

    // Record for topic creation response
    public record CreateTopicResponse(boolean success, boolean error, String message) {}

}
