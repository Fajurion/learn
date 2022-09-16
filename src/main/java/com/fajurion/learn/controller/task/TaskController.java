package com.fajurion.learn.controller.task;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tasks.*;
import com.fajurion.learn.repository.tasks.likes.TLikeRepository;
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

    // Repository for getting tasks
    private final TaskRepository taskRepository;

    // Repository for finding like data
    private final TLikeRepository tLikeRepository;

    @Autowired
    public TaskController(TaskService taskService,
                          SessionService sessionService,
                          TaskRepository taskRepository,
                          TLikeRepository tLikeRepository) {
        this.taskService = taskService;
        this.sessionService = sessionService;
        this.taskRepository = taskRepository;
        this.tLikeRepository = tLikeRepository;
    }

    @PostMapping("/list")
    @CrossOrigin
    public Mono<TaskListResponse> list(@RequestBody TaskListForm form) {

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
            return taskService.sortTasksByLikes(form.topic(), form.limit(), form.offset(), form.query(), form.difficulty(), form.sorting());
        }).map(list -> {

            // Return response
            return new TaskListResponse(true, false, "success", list);
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskListResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new TaskListResponse(false, true, "server.error", null));
    }

    // Form for listing tasks
    public record TaskListForm(String token, int topic, int offset, int limit, String query, int sorting, int difficulty) {}

    // Response to return tasks
    public record TaskListResponse(boolean success, boolean error, String message, List<TaskResponse> tasks) {}

    @PostMapping("/get")
    @CrossOrigin
    public Mono<TaskGetResponse> get(@RequestBody TaskGetForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new TaskGetResponse(false, false, "invalid", null));
        }

        // Check if session is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get task, owned state and liked state
            return Mono.zip(taskRepository.findById(form.task()).onErrorReturn(new Task(-1, -1, -1, -1, -1, "", "", "", "")),
                    Mono.just(session.getAccount()), tLikeRepository.getLikeByTaskAndAccount(form.task(), session.getAccount()).hasElement());
        }).map(tuple -> {

            // Check for error
            if(tuple.getT1().getCreator() == -1) {
                return new TaskGetResponse(false, false, "not_found", null);
            }

            // Return response
            return new TaskGetResponse(true, false, "success", new TaskInfoResponse(tuple.getT1(), tuple.getT3(),
                    tuple.getT2() == tuple.getT1().getCreator()));
        })
                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskGetResponse(false, false, e.getMessage(), null)))
                .onErrorReturn(new TaskGetResponse(false, true, "server.error", null));
    }

    // Form for getting tasks
    public record TaskGetForm(String token, int task) {}

    // Response to requesting tasks
    public record TaskGetResponse(boolean success, boolean error, String message, TaskInfoResponse task) {}

}
