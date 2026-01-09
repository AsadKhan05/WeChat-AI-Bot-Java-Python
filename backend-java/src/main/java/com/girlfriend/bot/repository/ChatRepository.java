package com.girlfriend.bot.repository;

import com.girlfriend.bot.model.entity.ChatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatRecord, Long> {

    @Query(value = "SELECT * FROM chat_history WHERE chat_user = ?1 ORDER BY create_time DESC LIMIT ?2", nativeQuery = true)
    List<ChatRecord> findRecentMessages(String chatUser, int limit);

    // 🌟 新增：获取某个时间点之后的记录
    @Query(value = "SELECT * FROM chat_history WHERE chat_user = ?1 AND create_time > ?2 ORDER BY create_time DESC LIMIT ?3", nativeQuery = true)
    List<ChatRecord> findMessagesAfterTime(String chatUser, java.time.LocalDateTime time, int limit);
}