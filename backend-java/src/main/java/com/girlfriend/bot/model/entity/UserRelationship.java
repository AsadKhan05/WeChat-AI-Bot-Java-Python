package com.girlfriend.bot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_relationship")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String chatUser;

    private Integer score; // 0 - 100

    private String levelName;

    private LocalDateTime updateTime;
}