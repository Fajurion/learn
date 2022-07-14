package com.fajurion.learn.controller.topic;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.topic.Topic;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/topic")
public class TopicController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting topics
    private final TopicRepository topicRepository;

    @Autowired
    public TopicController(SessionService sessionService, TopicRepository topicRepository) {
        this.sessionService = sessionService;
        this.topicRepository = topicRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<ListTopicsResponse> listSubTopics(@RequestBody ListTopicsForm topicsForm) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(topicsForm.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get all topics from the form
            return topicRepository.getTopicsByParent(topicsForm.topic()).collectList();
        }).flatMap(topics -> {

            if(topics == null) {
                return Mono.error(new CustomException("not_found"));
            }

            return Mono.just(new ListTopicsResponse(true, false, "success", (ArrayList<Topic>) topics));
        })
                // Error handling
                .onErrorResume(CustomException.class, error -> Mono.just(new ListTopicsResponse(false, false, error.getMessage(), new ArrayList<>())))
                .onErrorResume(error -> {
                    System.out.println(error.getMessage());
                    return Mono.just(new ListTopicsResponse(false, true, "server.error", new ArrayList<>()));
                });
    }

    // Record for listing sub topic form
    public record ListTopicsForm(String token, int topic) {}

    // Record for sub topics response
    public record ListTopicsResponse(boolean success, boolean error, String message, ArrayList<Topic> topics) {}


    @RequestMapping("/get")
    @ResponseBody @CrossOrigin
    public Mono<GetTopicResponse> get(@RequestBody ListTopicsForm form) {

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new RuntimeException("session.expired"));
            }

            // Get the topic
            return topicRepository.findById(form.topic());
        }).flatMap(topic -> {

            if(topic == null) {
                return Mono.error(new RuntimeException("not_found"));
            }

            // Return topic
            return Mono.just(new GetTopicResponse(true, false, "success", topic));
        })
                // Error handling
                .onErrorResume(RuntimeException.class, error -> Mono.just(new GetTopicResponse(false, false, error.getMessage(), null)))
                .onErrorResume(error -> Mono.just(new GetTopicResponse(false, true, "server.error", null)));
    }

    // Record for topic get response
    public record GetTopicResponse(boolean success, boolean error, String message, Topic topic) {}

}
