package com.fajurion.learn.repository.post;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("posts")
@Data
public class Post {

    @Id
    private int id;

    @Column
    final int topic, creator, likes;

    @Column
    final String content;

}
