package com.fajurion.learn.repository.tasks.likes;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("task_likes")
@Data
public class TLike {

    @Id
    int id;

    @Column @NonNull
    int account;

    @Column @NonNull
    int task;

}
