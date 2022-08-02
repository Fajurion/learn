package com.fajurion.learn.controller.task;

import com.fajurion.learn.repository.account.session.SessionService;
import com.fajurion.learn.repository.tasks.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskDeletionController {

    // Repository for creating tasks
    private final TaskRepository taskRepository;

    // Service for checking sessions
    private final SessionService sessionService;

    @Autowired
    public TaskDeletionController(TaskRepository taskRepository,
                                  SessionService sessionService) {
        this.taskRepository = taskRepository;
        this.sessionService = sessionService;
    }

}
