package com.fajurion.learn.repository.account.invite;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("invites")
@Data
public class Invite {

    @Id
    @NonNull
    private String code;

    @Column
    @NonNull
    private String creator, data;

}
