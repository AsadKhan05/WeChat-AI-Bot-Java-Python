package com.girlfriend.bot.model.entity;

import jakarta.persistence.*; // Spring Boot 3 必须用 jakarta
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history", indexes = {
    @Index(name = "idx_user_time", columnList = "chatUser, createTime") // 加索引优化查询
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 聊天对象的微信昵称 (例如: "略略略")
    @Column(nullable = false)
    private String chatUser;

    // 消息内容 (使用 TEXT 类型以支持长文本)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 角色：是用户说的，还是 AI 说的
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 创建时间
    @Column(nullable = false)
    private LocalDateTime createTime;

    /**
     * 内部枚举：角色定义
     */
    public enum Role {
        USER, // 用户
        AI    // 机器人
    }
}