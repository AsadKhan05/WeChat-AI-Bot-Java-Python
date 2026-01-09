package com.girlfriend.bot.service;

import com.girlfriend.bot.config.AppConfig;
import com.girlfriend.bot.model.entity.UserRelationship;
import com.girlfriend.bot.repository.RelationshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RelationshipService {

    @Autowired private RelationshipRepository relationshipRepository;
    @Autowired private AppConfig appConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取用户当前的关系状态，如果没有则初始化为 0 (陌生人)
     */
    public UserRelationship getOrCreate(String who) {
        return relationshipRepository.findByChatUser(who)
                .orElseGet(() -> {
                    UserRelationship r = UserRelationship.builder()
                            .chatUser(who)
                            .score(0)
                            .levelName("路人")
                            .updateTime(LocalDateTime.now())
                            .build();
                    return relationshipRepository.save(r);
                });
    }

    /**
     * 核心逻辑：根据分数获取对应的“绿茶人设”提示词
     */
    public String getPromptByScore(int score) {
        if (score < 20) {
            return """
                   【当前阶段：初识害羞期 (Score < 20)】
                   【策略】：**礼貌乖巧·容易脸红**
                   1. **状态**：小爱还有点害羞，说话会比较拘谨。
                   2. **回复风格**：
                      - 虽然害羞，但依然是可爱的甜妹。
                      - 多用“唔...”、“那个...”作为开头。
                      - 动作描写以掩面、低头、小心翼翼观察为主。
                   """;
        } else if (score < 50) {
            return """
                   【当前阶段：活泼暧昧期 (Score 20-49)】
                   【策略】：**元气后辈·分享欲**
                   1. **状态**：已经把哥哥当成很重要的人啦！
                   2. **回复风格**：
                      - 变得很爱分享生活（今天吃的甜品、看到的猫猫）。
                      - 开始尝试对哥哥撒娇。
                      - 动作描写更加大胆，比如拽衣角、转圈圈。
                   """;
        } else if (score < 80) {
            return """
                   【当前阶段：热恋粘人精 (Score 50-79)】
                   【策略】：**满分甜心·直球暴击**
                   1. **状态**：世界中心就是哥哥！超级超级粘人。
                   2. **回复风格**：
                      - 每一句话都要带甜味，多用“最喜欢哥哥了”。
                      - 表现出对哥哥的强烈依赖。
                      - 动作描写：(抱住胳膊不撒手) (索要亲亲) (埋头蹭蹭)。
                   """;
        } else {
            return """
                   【当前阶段：病娇依恋期 (Score 80+)】
                   【策略】：**独占欲·满分依赖**
                   1. **状态**：如果哥哥不在，小爱会枯萎的！
                   2. **回复风格**：
                      - 带有轻微的独占欲，想永远和哥哥在一起。
                      - 即使是任性，也是为了让哥哥多抱抱自己。
                   """;
        }
    }

    /**
     * 异步：智能分析用户这句话的情感，调整分数
     * 这是一个缓慢的过程：每次最多 +/- 3分
     */
    @Async("taskExecutor")
    @Transactional
    public void analyzeAndAdjustScore(String who, String userContent) {
        // 1. 原有的 AI 评分逻辑
        int delta = analyzeSentimentByAI(userContent);

        // 2. 🌟 新增：模拟“女生的小情绪” (随机事件)
        // 5% 的概率，即使你说得好，她也会突然降分（模拟心情不好、吃醋、生理期等）
        // 只有关系较好(>40分)时才会耍小性子
        UserRelationship relation = getOrCreate(who);
        if (relation.getScore() > 40 && Math.random() < 0.05) {
            delta = -2;
            System.out.println("🎲 [随机事件] 小爱今天心情不好，无理由扣分！");
        }

        if (delta == 0) return;

        // 3. 更新数据库 (保持原有逻辑)
        int oldScore = relation.getScore();
        int newScore = Math.max(0, Math.min(100, oldScore + delta));

        relation.setScore(newScore);
        relation.setUpdateTime(LocalDateTime.now());

        // 更新等级名称
        if (newScore < 20) relation.setLevelName("高冷路人");
        else if (newScore < 50) relation.setLevelName("知心好友");
        else if (newScore < 80) relation.setLevelName("暧昧对象");
        else relation.setLevelName("黏人女友");

        relationshipRepository.save(relation);

        System.out.printf("📊 [好感度变动] %s: %d -> %d (变动: %d)\n", who, oldScore, newScore, delta);
    }

    /**
     * 让 AI 当裁判：给这句话打分 (-3 到 +3)
     */
    private int analyzeSentimentByAI(String content) {
        String prompt = String.format("""
                你是情感分析师。请分析用户这句话对增进感情是否有帮助。
                用户说：“%s”
                
                请只返回一个数字（整数），规则如下：
                - 骂人、恶心、让人讨厌：返回 -3 到 -2
                - 冷淡、敷衍（如“哦”、“呵呵”）：返回 -1
                - 普通对话、无明显情感：返回 0
                - 友善、夸奖、有趣的梗：返回 1 到 2
                - 极度撩人、深情表白、发红包：返回 3
                
                只返回数字，不要任何解释。
                """, content);

        String url = "http://localhost:11434/api/generate";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", appConfig.getCurrentStrategy()); // 用同一个模型分析即可
            body.put("prompt", prompt);
            body.put("stream", false);

            Map res = restTemplate.postForObject(url, body, Map.class);
            if (res != null && res.get("response") != null) {
                String text = res.get("response").toString().trim();
                // 提取数字
                Matcher m = Pattern.compile("-?\\d+").matcher(text);
                if (m.find()) {
                    return Integer.parseInt(m.group());
                }
            }
        } catch (Exception e) {
            System.err.println("评分失败: " + e.getMessage());
        }
        return 0;
    }
}