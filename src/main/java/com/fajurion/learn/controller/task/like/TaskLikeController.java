package com.fajurion.learn.controller.task.like;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tasks.Task;
import com.fajurion.learn.repository.tasks.TaskRepository;
import com.fajurion.learn.repository.tasks.likes.TLike;
import com.fajurion.learn.repository.tasks.likes.TLikeRepository;
import com.fajurion.learn.util.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/task")
public class TaskLikeController {

    // Service for checking sessions
    private final SessionService sessionService;

    // Repository for getting tasks
    private final TaskRepository taskRepository;

    // Repository for liking tasks
    private final TLikeRepository tLikeRepository;

    @Autowired
    public TaskLikeController(SessionService sessionService,
                              TaskRepository taskRepository,
                              TLikeRepository tLikeRepository) {
        this.sessionService = sessionService;
        this.taskRepository = taskRepository;
        this.tLikeRepository = tLikeRepository;
    }

    @PostMapping("/like")
    @CrossOrigin
    public Mono<TaskLikeResponse> like(@RequestBody TaskLikeForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new TaskLikeResponse(false, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get task and check if it exists (and zip with account id)
            return Mono.zip(taskRepository.findById(form.task()).onErrorReturn(new Task(-1, -1, -1, -1, -1, "", "", "", "")),
                    Mono.just(session.getAccount()));
        }).flatMap(tuple -> {

            if(tuple.getT1().getTopic() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if user has already liked the task (and zip with account id)
            return Mono.zip(tLikeRepository.getLikeByTaskAndAccount(form.task(), tuple.getT2()).hasElement(),
                    Mono.just(tuple.getT2()), Mono.just(tuple.getT1()));
        }).flatMap(tuple -> {

            if(tuple.getT1()) {
                return Mono.error(new CustomException("already.liked"));
            }

            // Increment like counter
            Task task = tuple.getT3();
            task.setLikes(task.getLikes() + 1);

            // Like task and return response
            return Mono.zip(tLikeRepository.save(new TLike(tuple.getT2(), form.task())), taskRepository.save(task));
        }).map(like -> new TaskLikeResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskLikeResponse(false, false, e.getMessage())))
                .onErrorReturn(new TaskLikeResponse(false, true, "server.error"));
    }

    @PostMapping("/unlike")
    @CrossOrigin
    public Mono<TaskLikeResponse> unlike(@RequestBody TaskLikeForm form) {

        // Check if form is valid
        if(form.token() == null) {
            return Mono.just(new TaskLikeResponse(false, false, "invalid"));
        }

        // Check if session token is valid
        return sessionService.checkAndRefreshSession(form.token()).flatMap(session -> {

            if(session == null) {
                return Mono.error(new CustomException("session.expired"));
            }

            // Get task and check if it exists (and zip with account id)
            return Mono.zip(taskRepository.findById(form.task()).onErrorReturn(new Task(-1, -1, -1, -1, -1, "", "", "", "")),
                    Mono.just(session.getAccount()));
        }).flatMap(tuple -> {

            if(tuple.getT1().getTopic() == -1) {
                return Mono.error(new CustomException("not_found"));
            }

            // Check if user has not liked the task (and zip with account id)
            return Mono.zip(tLikeRepository.getLikeByTaskAndAccount(form.task(), tuple.getT2()).hasElement(),
                    Mono.just(tuple.getT2()), Mono.just(tuple.getT1()));
        }).flatMap(tuple -> {

            if(!tuple.getT1()) {
                return Mono.error(new CustomException("not.liked"));
            }

            // Decrement like counter
            Task task = tuple.getT3();
            task.setLikes(task.getLikes() - 1);

            // Delete like
            return Mono.zip(tLikeRepository.deleteByTaskAndAccount(tuple.getT3().getId(), tuple.getT2()).thenReturn("t"), taskRepository.save(task).thenReturn("t"));
        }).map(like -> new TaskLikeResponse(true, false, "success"))

                // Error handling
                .onErrorResume(CustomException.class, e -> Mono.just(new TaskLikeResponse(false, false, e.getMessage())))
                .onErrorReturn(new TaskLikeResponse(false, true, "server.error"));
    }

    // Record for liking a task
    public record TaskLikeForm(String token, int task) {}

    // Response to liking a task
    public record TaskLikeResponse(boolean success, boolean error, String message) {}

}
