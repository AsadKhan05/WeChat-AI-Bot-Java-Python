package com.girlfriend.bot.core.memory;

import com.girlfriend.bot.model.entity.ChatRecord;
import com.girlfriend.bot.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MemoryService {

    @Autowired private ChatRepository chatRepository;

    public void save(ChatRecord record) {
        chatRepository.save(record);
    }

    /**
     * 🌟 智能获取上下文：如果最后一条消息距离现在太久，则清空记忆（防止早起说晚安）
     * @param hours 记忆有效期（小时）
     */
    public String getShortTermContextSmart(String who, int limit, int hours) {
        LocalDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime().minusHours(hours);

        // 只查询最近 X 小时内的记录
        List<ChatRecord> rawList = chatRepository.findMessagesAfterTime(who, threshold, limit);

        if (rawList == null || rawList.isEmpty()) {
            return "[此对话刚开启，暂无近期历史记录，请直接开启新话题]";
        }

        Collections.reverse(rawList);
        StringBuilder sb = new StringBuilder();
        for (ChatRecord record : rawList) {
            String roleName = record.getRole() == ChatRecord.Role.USER ? "User" : "Assistant";
            sb.append(roleName).append(": ").append(record.getContent()).append("\n");
        }
        return sb.toString();
    }

    // 保留原有方法供普通对话使用
    public String getShortTermContext(String who, int limit) {
        List<ChatRecord> rawList = chatRepository.findRecentMessages(who, limit);
        if (rawList.isEmpty()) return "";
        Collections.reverse(rawList);
        StringBuilder sb = new StringBuilder();
        for (ChatRecord record : rawList) {
            String roleName = record.getRole() == ChatRecord.Role.USER ? "User" : "Assistant";
            sb.append(roleName).append(": ").append(record.getContent()).append("\n");
        }
        return sb.toString();
    }

    // 获取最近 AI 消息内容等其他方法保持不变...
    public List<String> getRecentAiMessages(String who, int limit) {
        List<ChatRecord> rawList = chatRepository.findRecentMessages(who, limit);
        List<String> aiContents = new ArrayList<>();
        for (ChatRecord record : rawList) {
            if (record.getRole() == ChatRecord.Role.AI) aiContents.add(record.getContent());
        }
        return aiContents;
    }

    // 不受时间限制，只取数量，用于防重复比对
    public List<String> getGlobalRecentAiMessages(String who, int limit) {
        // 调用不带时间条件的查询逻辑
        List<ChatRecord> rawList = chatRepository.findRecentMessages(who, limit);
        List<String> aiContents = new ArrayList<>();
        for (ChatRecord record : rawList) {
            if (record.getRole() == ChatRecord.Role.AI) aiContents.add(record.getContent());
        }
        return aiContents;
    }
}