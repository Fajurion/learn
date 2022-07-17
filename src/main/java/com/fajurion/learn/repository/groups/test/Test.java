package com.fajurion.learn.repository.groups.test;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tests")
@Data
public class Test {

    @Id
    int id;

    @Column
    String name, description;

    @Column
    long date;

    @Column
    int creator;
}
