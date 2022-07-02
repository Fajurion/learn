package com.fajurion.learn.repository.account.ranks;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ranks")
@Data
@RequiredArgsConstructor
public class Rank {

    @Id
    int id;

    @Column
    final String name;

    @Column
    final int level;

}
