package com.girlfriend.bot.core.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工厂模式：根据名字获取对应的 AI 策略
 */
@Service
public class AIStrategyFactory {

    // Spring 会自动把所有实现了 AIStrategy 的 Bean 放进这个 Map
    // Key 是 @Service("名字") 定义的名字
    @Autowired
    private Map<String, AIStrategy> strategyMap;

    public AIStrategy getStrategy(String strategyName) {
        AIStrategy strategy = strategyMap.get(strategyName);
        if (strategy == null) {
            throw new RuntimeException("找不到名为 " + strategyName + " 的 AI 策略，请检查配置或代码！");
        }
        return strategy;
    }
}