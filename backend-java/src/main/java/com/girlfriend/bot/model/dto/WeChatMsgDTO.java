package com.girlfriend.bot.model.dto;

import lombok.Data;

@Data
public class WeChatMsgDTO {
    private String sender;    // 发送者昵称
    private String content;   // 消息内容
    private String type;      // 消息类型 (text, image...)
    private Long timestamp;   // 时间戳
}