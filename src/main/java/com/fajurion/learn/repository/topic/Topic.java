package com.fajurion.learn.repository.topic;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("topics")
@Data
public class Topic {

    @Id
    private int id;

    @Column
    final int parent;

    @Column
    final String name;

    @Column
    final int creator;

    @Column
    @NonNull
    private boolean locked, category;

}
