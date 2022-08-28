package com.fajurion.learn.repository.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

    public Mono<List<TaskResponse>> sortTasksByLikes(int topic, int limit, int offset, String query, int filter, int sorting) {

        // Get tasks

        // Differentiate between the filters
        return getTasks(topic, limit, offset, query, filter, sorting).collectList().onErrorReturn(new ArrayList<>()).map(list -> {
            ArrayList<TaskResponse> taskResponses = new ArrayList<>();

            // Turn tasks into task responses
            for(Task task : list) {
                taskResponses.add(new TaskResponse(task));
            }

            // Return task responses
            return taskResponses;
        });
    }

    public Flux<Task> getTasks(int topic, int limit, int offset, String query, int filter, int sorting) {

        // Check for sorting
        if(sorting == 0) { // Sorted by likes

            // Check for search query
            if(query != null && !query.equals("")) {

                // Check for difficulty filter and return
                return filter == -1 ? taskRepository.searchTasksByLikes(topic, limit, offset, query)
                        : taskRepository.searchTasksByLikesDifficulty(topic, limit, offset, query, filter);
            }

            // Check for difficulty filter and return
            return filter == -1 ? taskRepository.sortTasksByLikes(topic, limit, offset)
                    : taskRepository.sortTasksByLikesDifficulty(topic, limit, offset, filter);
        }

        // Sorted by date
        // Check for search query
        if(query != null && !query.equals("")) {

            // Check for difficulty filter and return
            return filter == -1 ? taskRepository.searchTasksByDate(topic, limit, offset, query)
                    : taskRepository.searchTasksByDateDifficulty(topic, limit, offset, query, filter);
        }

        // Check for difficulty filter and return
        return filter == -1 ? taskRepository.sortTasksByDate(topic, limit, offset)
                : taskRepository.sortTasksByDateDifficulty(topic, limit, offset, filter);
    }

    public Mono<List<TaskResponse>> sortTasksByDifficulty(int topic, int limit, int offset) {

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
