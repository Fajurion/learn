package com.fajurion.learn.repository.account.invite;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("invites")
@Data
public class Invite {

    @Id
    private int id;

    @Column
    final String code;

    @Column
    final int creator;

    @Column
    final long date;

}
