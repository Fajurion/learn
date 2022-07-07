package com.fajurion.learn.repository.post.likes;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("likes")
@Data
public class Like {

    @Id
    private int account;

    @Column
    final int post;

}
