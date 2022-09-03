package com.fajurion.learn.repository.tasks;

import lombok.Data;

@Data
public class TaskResponse {

    private int id, likes, difficulty;
    private String title, task;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.likes = task.getLikes();
        this.difficulty = task.getDifficulty();
        this.title = task.getTitle();
        this.task = task.getTask();
    }

}
