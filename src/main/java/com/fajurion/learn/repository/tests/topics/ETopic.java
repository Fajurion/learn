package com.fajurion.learn.repository.tests.topics;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("exam_topics")
@Data
public class ETopic {

    @Id
    private final int id;

    @Column @NonNull
    private int test, topic;

}
