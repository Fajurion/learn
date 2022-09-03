package com.fajurion.learn.repository.tests;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("exams")
@Data
public class Exam {

    @Id
    private int id;

    @Column @NonNull
    private String name, board;

    @Column @NonNull
    private long date;

    @Column
    private final int groupID;

}
