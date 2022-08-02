package com.fajurion.learn.repository.post.comments;

import lombok.Data;

@Data
public class CommentResponse {

    private int id, creator;
    private String content, creatorName;
    private long date;

    public CommentResponse(Comment comment, String creatorName) {
        this.id = comment.getId();
        this.creator = comment.getCreator();
        this.content = comment.getContent();
        this.date = comment.getDate();
        this.creatorName = creatorName;
    }

}
