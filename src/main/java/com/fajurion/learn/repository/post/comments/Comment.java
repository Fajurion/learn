package com.fajurion.learn.repository.post.comments;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("comments")
@Data
public class Comment {

    @Id
    private int id;

    @Column
    final int creator;

    @Column
    final String content;

}
