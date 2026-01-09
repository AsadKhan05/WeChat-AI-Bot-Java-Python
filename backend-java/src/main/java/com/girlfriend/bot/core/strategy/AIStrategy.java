package com.girlfriend.bot.core.strategy;

/**
 * 策略接口：定义 AI 聊天的行为
 */
public interface AIStrategy {
    /**
     * @param systemPrompt 系统人设 + 记忆上下文
     * @param userMessage  用户的最新一句话
     * @return AI 的回复
     */
    String chat(String systemPrompt, String userMessage);
}