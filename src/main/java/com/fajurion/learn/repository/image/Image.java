package com.fajurion.learn.repository.image;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("images")
@Data
public class Image {

    @Id
    private int id;

    @Column
    final String type;

    @Column
    final int creator;

    @Column
    final byte[] image;

}
