package com.girlfriend.bot.core.vip;

import com.girlfriend.bot.model.entity.VipChatRecord;
import com.girlfriend.bot.repository.VipChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class VipMemoryService {

    @Autowired
    private VipChatRepository vipChatRepository;

    // 保存记录
    public void save(VipChatRecord record) {
        try {
            vipChatRepository.save(record);
            System.out.println("💾 [Memory Saved] " + record.getRole() + ": " +
                    (record.getContent().length() > 10 ? record.getContent().substring(0, 10) + "..." : record.getContent()));
        } catch (Exception e) {
            System.err.println("❌ 保存记忆失败: " + e.getMessage());
        }
    }

    /**
     * 获取上下文，并格式化为 Llama3 能看懂的样子
     */
    public String getVipContext(String who, int limit) {
        // 1. 查数据库 (倒序：最新的在前面)
        List<VipChatRecord> rawList = vipChatRepository.findRecentMessages(who, limit);

        if (rawList.isEmpty()) {
            System.out.println("⚠️ [Memory] 用户 " + who + " 没有历史记录");
            return "（无历史记录，这是对话的开始）";
        }

        // 2. 反转 List (变成：旧 -> 新，符合人类阅读顺序)
        Collections.reverse(rawList);

        // 3. 拼接字符串
        StringBuilder sb = new StringBuilder();
        for (VipChatRecord record : rawList) {
            String role = (record.getRole() == VipChatRecord.Role.USER) ? "User" : "Lilith";
            String content = record.getContent().replace("\n", " "); // 去除换行，防止破坏格式

            // 🔴 关键修改：使用明确的对话标记，增加时间感
            // 格式示例 ->  [User]: 你好骚啊
            //             [Lilith]: 哼，主人...
            sb.append("[").append(role).append("]: ").append(content).append("\n");
        }

        // 打印出来看看，确保查到了！
        System.out.println("🧠 [Context Constructed] 读取了 " + rawList.size() + " 条历史记录");

        return sb.toString();
    }
}