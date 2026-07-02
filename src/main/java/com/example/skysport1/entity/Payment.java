package com.example.skysport1.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private Integer status;

    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;
}
