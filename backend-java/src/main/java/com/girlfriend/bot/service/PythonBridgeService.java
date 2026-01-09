package com.girlfriend.bot.service;

import com.girlfriend.bot.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PythonBridgeService {

    @Autowired
    private AppConfig appConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送文本消息给 Python (带重试机制)
     */
    public void sendText(String who, String content) {
        // 1. 必须在这里先定义 payload，否则后面会报 Cannot resolve symbol
        Map<String, String> payload = new HashMap<>();
        payload.put("who", who);
        payload.put("content", content);

        // 2. 开始发送逻辑
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 发送 POST 请求
                restTemplate.postForLocation(appConfig.getPythonApiUrl(), payload);

                System.out.println(">>> [Java -> Python] 发送成功: " + content);
                return; // 发送成功，直接结束方法
            } catch (Exception e) {
                System.out.println("发送失败，第 " + (i + 1) + " 次重试...");
                try {
                    Thread.sleep(1000); // 失败稍微等1秒再试
                } catch (InterruptedException ig) {}
            }
        }

        // 如果循环结束还没 return，说明全失败了
        System.err.println("!!! 彻底发送失败，请检查 Python 服务是否启动！");
    }
}