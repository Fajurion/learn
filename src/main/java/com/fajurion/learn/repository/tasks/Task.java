package com.fajurion.learn.repository.tasks;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tasks")
@Data
public class Task {

    @Id
    int id;

    @Column @NonNull
    int topic, creator, difficulty, likes;

    @Column @NonNull
    long date;

    @Column @NonNull
    String title, task, content, explanation;

}
