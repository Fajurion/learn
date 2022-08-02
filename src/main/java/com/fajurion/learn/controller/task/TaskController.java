package com.fajurion.learn.controller.task;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tasks.TaskResponse;
import com.fajurion.learn.repository.tasks.TaskService;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    // Repository for getting tasks
    private final TaskService taskService;

    // Service for checking sessions
    private final SessionService sessionService;

    @Autowired
    public TaskController(TaskService taskService,
                                  SessionService sessionService) {
        this.taskService = taskService;
        this.sessionService = sessionService;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<TaskListResponse> list(@RequestBody TaskListForm form) {

        System.out.println(form.topic());
        // Check if form is valid
        if(form.token() == null || form.offset() < 0 || form.limit() > 20 || form.limit() < 0) {
            return Mono.just(new TaskListResponse(false, false, "empty", null));
        }

        // Check if token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get tasks in topic
            return taskService.sortTasksByLikes(form.topic(), form.limit(), form.offset());
        }).map(list -> {

            // Return response
            return new TaskListResponse(true, false, "success", list);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new TaskListResponse(false, true, "server.error", null));
    }

    // Form for listing tasks
    public record TaskListForm(String token, int topic, int offset, int limit) {}

    // Response to return tasks
    public record TaskListResponse(boolean success, boolean error, String message, List<TaskResponse> tasks) {}

}
