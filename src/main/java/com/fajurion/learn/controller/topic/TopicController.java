package com.fajurion.learn.controller.topic;

import com.fajurion.learn.controller.account.AccountController;
import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.topic.Topic;
import com.fajurion.learn.repository.topic.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/topic")
public class TopicController {

    // Repository for checking sessions
    private final SessionRepository sessionRepository;

    // Repository for getting topics
    private final TopicRepository topicRepository;

    @Autowired
    public TopicController(SessionRepository sessionRepository, TopicRepository topicRepository) {
        this.sessionRepository = sessionRepository;
        this.topicRepository = topicRepository;
    }

    @RequestMapping("/list")
    @ResponseBody
    public Mono<ListTopicsResponse> listSubTopics(@RequestBody ListTopicsForm topicsForm) {

        // Check if session is valid
        return sessionRepository.findById(topicsForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get all topics from the form
            return topicRepository.getTopicsByParent(topicsForm.topic());
        }).flatMap(topics -> {

            if(topics == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            return Mono.just(new ListTopicsResponse(true, false, "success", topics));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new ListTopicsResponse(false, false, error.getMessage(), new ArrayList<>())))
                .onErrorResume(error -> Mono.just(new ListTopicsResponse(false, true, "server.error", new ArrayList<>())));
    }

    // Record for listing sub topic form
    public record ListTopicsForm(String token, int topic) {}

    // Record for sub topics response
    public record ListTopicsResponse(boolean success, boolean error, String message, ArrayList<Topic> topics) {}

    @EventListener
    public void onStartup(ApplicationStartedEvent event) {

        // Check if topic exists
        topicRepository.findById(1).flatMap(topic -> {

            if(topic == null) {
                return Mono.error(new RuntimeException("topic.exists"));
            }

            return topicRepository.save(new Topic(-1, "Learn", 0, true, false));
        }).doOnNext(topic -> {
            System.out.println(topic.getName() + " created.");
        }).block();
    }

}
