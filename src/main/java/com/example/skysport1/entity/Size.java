package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Size")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Size {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private Integer status;
    private Boolean deleteFlag;
}
