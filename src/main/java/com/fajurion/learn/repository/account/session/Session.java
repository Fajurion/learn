package com.fajurion.learn.repository.account.session;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sessions")
@Getter
@Setter
@RequiredArgsConstructor
public class Session {

    @Id
    @NonNull
    private String token;

    @Column
    final int id;

    @Column @NonNull
    private long creation;

}
