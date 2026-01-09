package com.girlfriend.bot.core.strategy;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.PostConstruct; // ✅ Spring Boot 3 正确的导入
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service("Ollama") // Bean 名字叫 Ollama，方便工厂调用
public class OllamaStrategy implements AIStrategy {

    @Value("${bot.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${bot.ai-mode:qwen2.5:7b}")
    private String modelName;

    private ChatLanguageModel model;

    /**
     * 初始化 LangChain4j 的 Ollama 模型
     */
    @PostConstruct
    public void init() {
        System.out.println("正在连接本地 Ollama 模型: " + modelName + " ...");
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(120)) // 设置超时时间，本地跑可能会慢
                .temperature(0.7) // 温度：0.7 比较适合聊天，有创造性又不会太疯
                .build();
        System.out.println("Ollama 模型加载完毕！");
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        // 🟢 修改点：在 systemPrompt 后追加长度限制指令
        String lengthLimit = "\n[System Note: Keep your response concise, strictly within 4 sentences.]";

        String fullPrompt = systemPrompt + lengthLimit + "\n\nUser: " + userMessage + "\nAssistant:";

        try {
            return model.generate(fullPrompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "（脑子突然短路了... 请检查 Ollama 是否启动）";
        }
    }
}