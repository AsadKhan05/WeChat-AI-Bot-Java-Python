package com.girlfriend.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 开启异步注解支持
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：平时保留5个线程等待消息
        executor.setCorePoolSize(5);
        // 最大线程数：忙的时候最多开10个
        executor.setMaxPoolSize(10);
        // 队列大小：如果线程都满了，最多排队25条消息
        executor.setQueueCapacity(25);
        // 线程前缀名，方便日志排查
        executor.setThreadNamePrefix("AI-Brain-");
        // 拒绝策略：如果队列也满了，由调用者所在线程自己执行（防止丢消息）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}