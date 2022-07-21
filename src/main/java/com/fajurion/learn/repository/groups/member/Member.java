package com.fajurion.learn.repository.groups.member;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("members")
@Data
public class Member {

    @Id
    int id;

    @Column("class") @NonNull
    int group;

    @Column @NonNull
    int account;

    @Column @NonNull
    final long joined;

}
