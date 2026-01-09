package com.girlfriend.bot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_vip_chat_history") // 🌟 独立表名
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipChatRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chatUser; // 微信ID

    @Column(columnDefinition = "TEXT")
    private String content;  // 内容

    @Enumerated(EnumType.STRING)
    private Role role;       // USER 或 AI

    private LocalDateTime createTime;

    public enum Role {
        USER, AI
    }
}