package com.girlfriend.bot.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class StickerFactory {

    private final Map<String, List<String>> stickerMap = new HashMap<>();
    private final Random random = new Random();

    public StickerFactory() {
        // 🛑 请确认这个路径是你电脑上真实的路径
        String basePath = "C:\\Users\\user\\Desktop\\code\\自娱自乐小代码\\后端\\ai对话机器人\\素材\\表情包\\";

        // ==========================================
        // 1. 暧昧 & 亲密 (整合了 kiss1 和 kiss2)
        // ==========================================
        // 关键词包含：亲、吻、木马、kiss、嘴一个、亲亲
        register(basePath + "kiss.gif",  "亲", "吻", "木马", "kiss", "嘴一个", "亲亲");
        register(basePath + "love.gif",  "亲", "吻", "木马", "kiss", "嘴一个", "亲亲");
        register(basePath + "rub.gif",   "亲", "吻", "木马", "kiss", "嘴一个", "亲亲");
        register(basePath + "kiss1.jpg", "亲", "吻", "木马", "kiss", "嘴一个", "亲亲"); // 新成员
        register(basePath + "kiss2.jpg", "亲", "吻", "木马", "kiss", "嘴一个", "亲亲"); // 新成员

        // 当关键词是 "贴贴" 时
        register(basePath + "rub.gif",  "贴贴", "蹭", "抱", "钻怀里", "黏");
        register(basePath + "kiss.gif", "贴贴", "蹭", "抱", "钻怀里", "黏");

        // 当关键词是 "爱" 时
        register(basePath + "love.gif", "爱", "笔芯", "比心", "love", "心动");
        register(basePath + "wink.gif", "爱", "笔芯", "比心", "love", "心动");

        // ==========================================
        // 2. 日常互动 (整合了 hello.jpg)
        // ==========================================
        // 打招呼：嗨、你好、hello、挥手、早、在吗
        register(basePath + "hello.jpg", "嗨", "你好", "hello", "挥手", "早", "在吗"); // 新成员
        register(basePath + "hi.jpg",    "嗨", "你好", "hello", "挥手", "早", "在吗");
        register(basePath + "sky.gif",   "嗨", "你好", "hello", "挥手", "早", "在吗");

        // 肯定回复
        register(basePath + "ok.gif", "好", "收到", "ok", "没问题", "遵命", "恩", "嗯");

        // 否定回复
        register(basePath + "no.jpg", "不", "拒绝", "达咩", "不行", "不可以");

        // 疑问
        register(basePath + "question.jpg", "疑", "问", "啥", "？", "不懂");

        // 哈哈大笑
        register(basePath + "laugh.jpg", "哈", "笑", "嘿");
        register(basePath + "sky.gif",   "哈", "笑", "嘿");

        // ==========================================
        // 3. 负面情绪
        // ==========================================
        register(basePath + "cry.jpg",     "哭", "呜", "难过", "泪");
        register(basePath + "wronged.jpg", "哭", "呜", "难过", "泪");

        register(basePath + "angry.jpg",  "气", "发火", "怒", "哼");
        register(basePath + "ignore.jpg", "气", "发火", "怒", "哼", "不理", "不想理");

        register(basePath + "hit.jpg", "打", "锤", "揍", "敲", "拳");

        // ==========================================
        // 4. 生活状态
        // ==========================================
        register(basePath + "eat.jpg",   "吃", "饿", "饭", "嚼");
        register(basePath + "sleep.jpg", "困", "睡", "晚安", "梦");
        register(basePath + "bed.jpg",   "困", "睡", "晚安", "梦", "赖床", "躺");
        register(basePath + "bath.jpg",  "洗澡", "泡澡", "洗", "吹头发", "香");
    }

    /**
     * 注册方法：给关键词绑定图片路径
     */
    private void register(String filePath, String... keywords) {
        for (String keyword : keywords) {
            stickerMap.computeIfAbsent(keyword, k -> new ArrayList<>()).add(filePath);
        }
    }

    /**
     * 根据内容查找图片路径
     */
    public String findPathByFuzzyContent(String content) {
        if (content == null || content.isEmpty()) return null;

        // 遍历所有注册过的关键词
        for (Map.Entry<String, List<String>> entry : stickerMap.entrySet()) {
            String keyword = entry.getKey();
            // 如果 AI 的回复（content）里包含这个关键词
            if (content.contains(keyword)) {
                List<String> paths = entry.getValue();
                if (paths != null && !paths.isEmpty()) {
                    // 从该关键词对应的图片列表中随机选一张
                    String selected = paths.get(random.nextInt(paths.size()));
                    System.out.println("🎲 命中关键词 [" + keyword + "]，随机选中: " + selected);
                    return selected;
                }
            }
        }
        return null;
    }
}