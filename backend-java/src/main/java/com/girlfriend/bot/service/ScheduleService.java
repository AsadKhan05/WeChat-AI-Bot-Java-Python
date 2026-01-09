package com.girlfriend.bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduleService {

    @Autowired private ChatService chatService;

    // 目标用户
    @Value("${bot.target-user:略略略}")
    private String targetUser;

    // 用于执行延迟任务的线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();

    /**
     * ☀️ 早安任务 (时间抖动版)
     * 策略：每天 07:30 唤醒程序，然后随机等待 0~60 分钟再发消息。
     * 最终发送时间范围：07:30 ~ 08:30
     */
    @Scheduled(cron = "0 30 7 * * ?")
    public void scheduleGoodMorning() {
        // 生成随机延迟：0 到 60 分钟
        int delayMinutes = random.nextInt(60);
        long delayMs = delayMinutes * 60 * 1000L;

        System.out.println("☀️ [早安计划] 已启动，将在 " + delayMinutes + " 分钟后(" +
                LocalTime.now().plusMinutes(delayMinutes).toString().substring(0,5) + ") 发送问候...");

        // 放入线程池延迟执行
        scheduler.schedule(() -> {
            System.out.println("☀️ [定时任务] 执行早安问候 -> " + targetUser);
            chatService.triggerProactiveChat(targetUser);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 🌙 晚安任务 (时间抖动版)
     * 策略：每天 22:30 唤醒程序，然后随机等待 0~60 分钟再发消息。
     * 最终发送时间范围：22:30 ~ 23:30
     */
    @Scheduled(cron = "0 30 22 * * ?")
    public void scheduleGoodNight() {
        // 生成随机延迟：0 到 60 分钟
        int delayMinutes = random.nextInt(60);
        long delayMs = delayMinutes * 60 * 1000L;

        System.out.println("🌙 [晚安计划] 已启动，将在 " + delayMinutes + " 分钟后(" +
                LocalTime.now().plusMinutes(delayMinutes).toString().substring(0,5) + ") 发送问候...");

        // 放入线程池延迟执行
        scheduler.schedule(() -> {
            System.out.println("🌙 [定时任务] 执行晚安问候 -> " + targetUser);
            chatService.triggerProactiveChat(targetUser);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * ⏰ [粘人模式] 智能查岗：每隔 4 小时主动找你
     *
     * 逻辑说明：
     * 1. 定时器每小时启动一次检查。
     * 2. 判断逻辑：如果 (当前时间是白天) 且 (你超过4小时没理她)。
     * 3. 结果：主动发消息撩你。
     *
     * 【开启方法】：删除下面代码块首尾的 /* 和 *\/ 即可生效
     */
    /*
    @Scheduled(cron = "0 0 0/1 * * ?") // 每小时检查一次
    public void checkAndProactive() {
        // 1. 获取当前时间
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        // 🌟 智能避让：只有白天 (9:00 - 22:00) 才主动打扰，半夜绝不吵醒用户
        if (hour < 9 || hour > 22) {
            return;
        }

        // 2. 检查距离上次说话过了多久
        long lastTime = chatService.getLastActiveTime(targetUser);

        // 如果系统刚重启没记录，或者从未说过话，暂不触发防止误判
        if (lastTime == 0) return;

        long diff = System.currentTimeMillis() - lastTime;
        // 设定阈值：4小时 (4 * 60 * 60 * 1000 毫秒)
        long threshold = 4L * 60 * 60 * 1000;

        // 3. 如果超过 4 小时没理她，触发主动关心
        if (diff > threshold) {
            System.out.println("⏰ [粘人模式] 发现用户 " + targetUser + " 消失超过4小时，主动发起聊天...");
            chatService.triggerProactiveChat(targetUser);
        }
    }
    */
}