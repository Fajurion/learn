package com.fajurion.learn.repository.post.likes;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("likes")
@Data
public class Like {

    @Id
    @NonNull
    int account;

    @Column
    @NonNull
    int post;

}
