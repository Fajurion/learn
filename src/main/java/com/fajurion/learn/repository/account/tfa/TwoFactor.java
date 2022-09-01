package com.fajurion.learn.repository.account.tfa;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tfa")
@Data
public class TwoFactor {

    @Id
    private int id;

    @Column
    private final int account;

    @Column @NonNull
    private String secret, backup;

}
