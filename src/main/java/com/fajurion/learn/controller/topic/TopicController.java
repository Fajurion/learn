package com.fajurion.learn.controller.topic;

import com.fajurion.learn.repository.account.session.SessionRepository;
import com.fajurion.learn.repository.topic.Topic;
import com.fajurion.learn.repository.topic.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping("/subtopics")
    public ListTopicsResponse listSubTopics(@RequestBody ListTopicsForm topicsForm) {
        return new ListTopicsResponse(false, new ArrayList<>());
    }

    // Record for listing sub topic form
    public record ListTopicsForm(String token, int topic) {}

    // Record for sub topics response
    public record ListTopicsResponse(boolean success, ArrayList<Topic> topics) {}

}
