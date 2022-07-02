package com.fajurion.learn.repository.account.invite;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("invites")
@Data
@RequiredArgsConstructor
public class Invite {

    @Id
    final String code;

    @Column
    final String data;

    @Column
    final int creator;

}
