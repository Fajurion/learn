package com.fajurion.learn.repository.tasks.reports;

import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("task_reports")
public class TReport {

    @Id
    int id;

    @Column @NonNull
    int task, creator;

    @Column
    long date;

    @Column
    String content;

}
