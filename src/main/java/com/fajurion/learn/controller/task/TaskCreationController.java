package com.fajurion.learn.controller.task;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tasks.Task;
import com.fajurion.learn.repository.tasks.TaskRepository;
import com.fajurion.learn.repository.topic.Topic;
import com.fajurion.learn.repository.topic.TopicRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/api/task")
@RestController
public class TaskCreationController {

    // Repository for creating tasks
    private final TaskRepository taskRepository;

    // Repository for getting topic data
    private final TopicRepository topicRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    @Autowired
    public TaskCreationController(TaskRepository taskRepository,
                                  TopicRepository topicRepository,
                                  SessionService sessionService) {
        this.taskRepository = taskRepository;
        this.topicRepository = topicRepository;
        this.sessionService = sessionService;
    }

    @PostMapping("/create")
    @CrossOrigin
    public Mono<TaskCreateResponse> create(@RequestBody TaskCreateForm form) {

        // Check if form is valid
        if(form.token() == null || form.title() == null || form.content() == null || form.task() == null || form.explanation() == null) {
            return Mono.just(new TaskCreateResponse(false, false, "empty"));
        }

        // Check if form requirements are met
        if(form.title().length() < 3 || form.title().length() > 50 || form.task().length() < 3 || form.task().length() > 200
        || form.content().length() < 3 || form.content().length() > 200 || form.explanation().length() > 512 || form.difficulty() > 3
        || form.difficulty() < 0) {
            return Mono.just(new TaskCreateResponse(false, false, "invalid"));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get topic of task (and zip with account id (needed later))
            return Mono.zip(topicRepository.findById(form.topic()).onErrorReturn(new Topic(-1, "", -1, false, false)),
                    Mono.just(session.getAccount()));
        }).flatMap(tuple -> {

            // Check if topic exists
            if(tuple.getT1().getCreator() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if topic is locked
            if(tuple.getT1().isLocked()) {
                return Mono.error(new CustomException("locked"));
            }

            // Create task
            return taskRepository.save(new Task(tuple.getT1().getId(), tuple.getT2(), form.difficulty(), 0,
                    System.currentTimeMillis(), form.title(), form.task(), form.content(), form.explanation()));
        }).map(task -> {

            // Return response
            return new TaskCreateResponse(true, false, "success");
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskCreateResponse(false, false, e.getMessage())))
                .onErrorResume( e -> {
                    System.out.println(e.getMessage());
                    return Mono.just(new TaskCreateResponse(false, true, "server.error"));
                });
    }

    // Form for creating tasks
    public record TaskCreateForm(String token, int topic, int difficulty, String title, String task, String content, String explanation) {}

    // Response to creating tasks
    public record TaskCreateResponse(boolean success, boolean error, String message) {}

}
