package com.fajurion.learn.repository.report;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reports")
@Data
public class Report {

    @Id
    private int id;

    @Column
    private final int creator;

    @Column
    private final long date;

    @Column @NonNull
    private String url, description;

}
