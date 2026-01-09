package com.girlfriend.bot.service;

import com.girlfriend.bot.config.AppConfig;
import com.girlfriend.bot.core.memory.MemoryService;
import com.girlfriend.bot.model.entity.ChatRecord;
import com.girlfriend.bot.model.entity.UserRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    @Autowired private AppConfig appConfig;
    @Autowired private MemoryService memoryService;
    @Autowired private PythonBridgeService pythonBridge;
    @Autowired private RelationshipService relationshipService;
    @Autowired private StickerFactory stickerFactory;

    @Value("${bot.ai-mode}")
    private String normalModel;

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    private final Map<String, List<String>> messageBuffer = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> debounceTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Map<String, AtomicBoolean> processingMap = new ConcurrentHashMap<>();
    private final Map<String, String> lastSentImgMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActiveTimeMap = new ConcurrentHashMap<>();

    private static final Map<String, String> T2S_MAP = new HashMap<>();
    static {
        String t = "現在幾該讓親愛園個們這歡点東會發時刻看麼样谁聽說夢覺臉飯過來頭帮寫優視話电影後裏還著冇喲餵媽愛響麼";
        String s = "现在几该让亲爱园个们这欢点东会发时刻看么样谁听说梦觉脸饭过来头帮写优视话电影后里还着没哟喂妈爱响么";
        for (int i = 0; i < t.length(); i++) T2S_MAP.put(String.valueOf(t.charAt(i)), String.valueOf(s.charAt(i)));
    }

    public ChatService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    public void processMessage(String who, String content) {
        lastActiveTimeMap.put(who, System.currentTimeMillis());
        messageBuffer.computeIfAbsent(who, k -> new ArrayList<>()).add(content);

        ScheduledFuture<?> existingTask = debounceTasks.get(who);
        if (existingTask != null) existingTask.cancel(false);

        long delay = 800 + random.nextInt(700);
        debounceTasks.put(who, scheduler.schedule(() -> executeBufferedMessage(who), delay, TimeUnit.MILLISECONDS));
    }

    @Transactional
    public void executeBufferedMessage(String who) {
        AtomicBoolean isProcessing = processingMap.computeIfAbsent(who, k -> new AtomicBoolean(false));
        if (!isProcessing.compareAndSet(false, true)) return;

        try {
            List<String> messages;
            synchronized (messageBuffer) {
                messages = new ArrayList<>(messageBuffer.getOrDefault(who, Collections.emptyList()));
                messageBuffer.remove(who);
            }
            if (messages.isEmpty()) return;
            String combinedContent = String.join("，", messages).replaceAll("\\s+", "");

            memoryService.save(ChatRecord.builder().chatUser(who).content(combinedContent).role(ChatRecord.Role.USER).createTime(LocalDateTime.now()).build());
            relationshipService.analyzeAndAdjustScore(who, combinedContent);
            generateAIResponse(who, combinedContent, false);

        } finally {
            isProcessing.set(false);
        }
    }

    private void generateAIResponse(String who, String userContent, boolean isProactive) {
        UserRelationship relation = relationshipService.getOrCreate(who);
        String basePersona = appConfig.getPersona();
        String stageStrategy = relationshipService.getPromptByScore(relation.getScore());

        String historyContext = memoryService.getShortTermContextSmart(who, 16, 4);
        if (!isProactive) historyContext = filterDuplicateContext(historyContext);

        ZonedDateTime nowBeijing = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        String preciseTime = nowBeijing.format(DateTimeFormatter.ofPattern("HH点mm分"));

        String intentAnchor = extractIntent(userContent);
        String vibe = isProactive ? "刚睡醒想哥哥了" : (intentAnchor.isEmpty() ? "日常腻歪" : "陪哥哥聊聊'" + intentAnchor + "'");

        // 构建指令：加入更强硬的身份锚定
        String systemInstruction = String.format("""
                %s
                
                【阶段策略】:
                %s
                
                【当前状态】:
                - 北京时间：%s
                - 哥哥对你的好感度：%d/100
                - 小爱的内心OS：%s
                
                【⚠️ 绝密禁令】:
                1. 严禁承认自己是AI、助手或程序。你就是小爱，一个爱撒娇的女生！
                2. 严禁输出序号列表。
                3. 禁止使用“：”引导描述。
                4. 绝对禁止代替哥哥（User）说话，禁止续写哥哥的台词。
                """,
                basePersona, stageStrategy, preciseTime, relation.getScore(), vibe
        );

        // 修改标签名使其更像聊天背景而非文档模板
        String currentInstruction = isProactive ? "快去给哥哥发个甜甜的消息吧~" : "【哥哥刚才说的话】: " + userContent;

        String finalPrompt = String.format("""
                <|im_start|>system
                %s
                <|im_end|>
                <|im_start|>user
                【我们刚才聊到哪了】:
                %s
                
                %s
                <|im_end|>
                <|im_start|>assistant
                """,
                systemInstruction, historyContext, currentInstruction
        );

        String rawReply = callOllamaAPI(finalPrompt);
        String cleanedReply = cleanAIResponse(rawReply);

        if (isSimilarToHistory(who, cleanedReply)) {
            rawReply = callOllamaAPI(finalPrompt + "\n(Note: 拒绝重复，换个更好玩的说法撩哥哥！)");
            cleanedReply = cleanAIResponse(rawReply);
        }

        if (cleanedReply.isEmpty()) cleanedReply = "唔... (盯着哥哥看)";

        memoryService.save(ChatRecord.builder().chatUser(who).content(cleanedReply).role(ChatRecord.Role.AI).createTime(LocalDateTime.now()).build());

        final String finalContent = cleanedReply;
        CompletableFuture.runAsync(() -> {
            try {
                sendBubblesSafely(who, finalContent);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void sendBubblesSafely(String who, String fullText) throws InterruptedException {
        if (fullText == null || fullText.isEmpty()) return;
        List<String> stickerPaths = new ArrayList<>();
        Matcher m = Pattern.compile("[\\(\uff08](.*?)[\\)\uff09]").matcher(fullText);
        while (m.find()) {
            String path = stickerFactory.findPathByFuzzyContent(m.group(1).trim());
            if (path != null && !path.equals(lastSentImgMap.get(who))) stickerPaths.add(path);
        }
        if (stickerPaths.size() > 2) stickerPaths = stickerPaths.subList(0, 2);

        String cleanText = fullText.replaceAll("[\\(\uff08].*?[\\)\uff09]", "").trim();
        String[] parts = cleanText.split("(?<=[！。？~])");
        List<String> textBubbles = new ArrayList<>();
        String temp = "";
        for (String s : parts) {
            if (temp.length() + s.length() < 15) temp += s;
            else { if(!temp.isEmpty()) textBubbles.add(temp); temp = s; }
        }
        if (!temp.isEmpty()) textBubbles.add(temp);

        int totalSent = 0;
        int imgIdx = 0;
        for (String content : textBubbles) {
            if (totalSent >= 6) break;
            content = content.trim().replaceAll("^[，。, .：:]+", "");
            if (content.isEmpty()) continue;
            Thread.sleep(500 + (content.length() * 80L) + random.nextInt(300));
            pythonBridge.sendText(who, content);
            totalSent++;
            if (imgIdx < stickerPaths.size() && totalSent < 6) {
                Thread.sleep(400);
                String img = stickerPaths.get(imgIdx++);
                pythonBridge.sendText(who, "[FILE]" + img);
                lastSentImgMap.put(who, img);
                totalSent++;
            }
        }
    }

    public String cleanAIResponse(String raw) {
        if (raw == null) return "";
        String c = raw.replaceAll("<think>[\\s\\S]*?</think>", "").replace("<|im_end|>", "").replace("<|im_start|>", "");

        // 🌟 核心修复：防止模型代入角色续写。发现这些词说明AI在写剧本，直接截断
        String[] splitters = {"用户：", "用户:", "User:", "Assistant:", "助手：", "小爱：", "小爱:", "System:", "系统:"};
        for (String splitter : splitters) {
            int idx = c.indexOf(splitter);
            if (idx != -1) c = c.substring(0, idx);
        }

        // 清理列表与标签
        c = c.replaceAll("\\d+\\.\\s*\\*\\*.*?\\*\\*[:：]?", "");
        c = c.replaceAll("\\d+\\.\\s+.*?[:：]", "");
        c = c.replaceAll("【.*?】", "").replaceAll("\\[.*?\\]", "");

        // 物理切断冒号旁白
        String[] lines = c.split("(?<=[！。？~\n])");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("：") || t.startsWith(":") || t.contains("场景") || t.contains("画面") || t.contains("背景")) continue;
            sb.append(t);
        }
        c = sb.toString();

        for (Map.Entry<String, String> entry : T2S_MAP.entrySet()) c = c.replace(entry.getKey(), entry.getValue());

        String[] sentences = c.split("(?<=[！。？~])");
        List<String> unique = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() < 2) continue;
            boolean isDup = false;
            for (String ex : unique) {
                if (calculateSimilarity(trimmed, ex) > 0.8) { isDup = true; break; }
            }
            if (!isDup) unique.add(trimmed);
        }
        c = String.join("", unique);
        c = c.replaceAll("(?<![\\(\uff08])[a-zA-Z]{3,}(?![\\)\uff09])", "");
        return c.replace("，，", "，").replaceAll("\\s+", " ").replaceAll("^[，。, .：:]+", "").trim();
    }

    private String callOllamaAPI(String prompt) {
        String ollamaUrl = "http://localhost:11434/api/generate";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", normalModel);
            body.put("prompt", prompt);
            body.put("stream", false);
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.9);
            options.put("top_p", 0.95);
            options.put("repeat_penalty", 1.3);
            options.put("seed", System.currentTimeMillis() + random.nextInt(1000));
            body.put("options", options);
            Map res = restTemplate.postForObject(ollamaUrl, body, Map.class);
            return res != null ? res.get("response").toString() : "";
        } catch (Exception e) { return ""; }
    }

    private String extractIntent(String content) {
        if (content == null || content.isEmpty()) return "";
        String[] keywords = {"去", "想", "做", "吃", "聊", "玩", "看", "喜欢", "爱"};
        for (String key : keywords) {
            if (content.contains(key)) {
                int start = content.indexOf(key);
                return content.substring(start, Math.min(start + 6, content.length()));
            }
        }
        return "";
    }

    private boolean isSimilarToHistory(String who, String current) {
        List<String> recentAi = memoryService.getRecentAiMessages(who, 8);
        for (String old : recentAi) {
            if (calculateSimilarity(current, old) > 0.75) return true;
        }
        return false;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        Set<Character> set1 = getCharSet(s1);
        Set<Character> set2 = getCharSet(s2);
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }

    private Set<Character> getCharSet(String s) {
        Set<Character> set = new HashSet<>();
        for (char c : s.toCharArray()) if (c > 127 || Character.isLetterOrDigit(c)) set.add(c);
        return set;
    }

    private String filterDuplicateContext(String context) {
        if (context == null) return "";
        String[] lines = context.split("\n");
        return String.join("\n", new LinkedHashSet<>(Arrays.asList(lines)));
    }

    public void triggerProactiveChat(String who) { generateAIResponse(who, "", true); }
    public long getLastActiveTime(String who) { return lastActiveTimeMap.getOrDefault(who, 0L); }
}