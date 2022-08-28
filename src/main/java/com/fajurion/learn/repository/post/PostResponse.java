package com.fajurion.learn.repository.post;

import com.fajurion.learn.repository.account.Account;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostResponse {

    private int id;

    int topic, creator, likes;

    final long date;

    String title, content, creatorName;

    private boolean liked, created;

    public PostResponse(Post post, boolean liked, int userID, Account account) {
        this.id = post.getId();
        this.topic = post.getTopic();
        this.creator = post.getCreator();
        this.date = post.getDate();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.likes = post.getLikes();
        this.liked = liked;
        this.creatorName = account.getUsername();
        this.created = userID == post.getCreator();
    }

}
