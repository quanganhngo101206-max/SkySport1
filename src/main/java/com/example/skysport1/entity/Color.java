package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Color")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Color {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String hexCode;
    private Integer status;
    private Boolean deleteFlag;
}
