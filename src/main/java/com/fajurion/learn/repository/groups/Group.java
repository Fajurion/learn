package com.fajurion.learn.repository.groups;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("groups")
@Data
public class Group {

    @Id
    int id;

    @Column @NonNull
    String name, description;

    @Column
    final int creator;

}
