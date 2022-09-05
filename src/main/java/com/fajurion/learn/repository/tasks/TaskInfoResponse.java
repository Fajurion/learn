package com.fajurion.learn.repository.tasks;

public class TaskInfoResponse {

    public int id;

    public int topic, creator, difficulty, likes;

    public long date;

    public String title, task, content, explanation;

    public boolean liked, created;

    public TaskInfoResponse(Task task, boolean liked, boolean created) {
        this.id = task.getId();
        this.topic = task.getTopic();
        this.creator = task.getCreator();
        this.difficulty = task.getDifficulty();
        this.likes = task.getLikes();
        this.date = task.getDate();
        this.title = task.getTitle();
        this.content = task.getContent();
        this.explanation = task.getExplanation();
        this.task = task.getTask();
        this.liked = liked;
        this.created = created;
    }

}
