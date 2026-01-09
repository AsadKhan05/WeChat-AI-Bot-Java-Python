package com.girlfriend.bot.repository;

import com.girlfriend.bot.model.entity.VipChatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VipChatRepository extends JpaRepository<VipChatRecord, Long> {

    @Query(value = "SELECT * FROM t_vip_chat_history WHERE chat_user = ?1 ORDER BY create_time DESC LIMIT ?2", nativeQuery = true)
    List<VipChatRecord> findRecentMessages(String chatUser, int limit);
}