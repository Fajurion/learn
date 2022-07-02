package com.fajurion.learn.repository.account;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "accounts")
@Getter
@Setter
@RequiredArgsConstructor
public class Account {

    @Id
    private int id;

    @Column
    @NonNull
    private String username, email, rank, password, data;

    @Column
    private int invitor;

}
