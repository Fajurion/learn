package com.fajurion.learn.repository.post;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("posts")
@Data
public class Post {

    @Id
    private int id;

    @Column @NonNull
    int topic, creator, likes;

    @Column
    final long date;

    @Column @NonNull
    String title, content;

}
