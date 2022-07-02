package com.fajurion.learn.repository.account.invite;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("invites")
@Data
public class Invite {

    @Id
    private String code;

    @Column
    private String data;

    @Column
    private int creator;

}
