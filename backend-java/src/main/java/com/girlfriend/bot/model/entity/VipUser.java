package com.girlfriend.bot.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_vip_user") // 对应数据库表名
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipUser {

    @Id
    @Column(name = "wx_id") // 🟢 必须加这行！对应 SQL 里的 wx_id 列
    private String wxId;    // 对应你截图里的 "略略略"

    @Column(name = "expire_time") // 对应 SQL 里的 expire_time 列
    private LocalDateTime expireTime;

    public boolean isValid() {
        return expireTime != null && expireTime.isAfter(LocalDateTime.now());
    }
}