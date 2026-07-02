package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Role")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    private LocalDateTime createDate;
    private LocalDateTime updateDate;
}
