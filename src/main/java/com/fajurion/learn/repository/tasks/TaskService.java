package com.fajurion.learn.repository.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    // Repository for getting tasks
    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Mono<List<TaskResponse>> sortTasksByLikes(int topic, int limit, int offset) {

        // Get tasks
        return taskRepository.sortTasksByLikes(topic, limit, offset).collectList().onErrorReturn(new ArrayList<>()).map(list -> {
            ArrayList<TaskResponse> taskResponses = new ArrayList<>();

            // Turn tasks into task responses
            for(Task task : list) {
                taskResponses.add(new TaskResponse(task));
            }

            // Return task responses
            return taskResponses;
        });
    }

}
