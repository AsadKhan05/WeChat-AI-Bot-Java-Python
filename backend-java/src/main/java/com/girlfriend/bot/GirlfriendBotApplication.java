package com.girlfriend.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 开启定时任务支持 (ScheduleService需要)
@EnableAsync       // 开启异步线程池支持 (ChatService需要)
public class GirlfriendBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GirlfriendBotApplication.class, args);
    }
}