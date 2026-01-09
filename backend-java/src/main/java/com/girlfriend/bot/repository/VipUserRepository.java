package com.girlfriend.bot.repository;

import com.girlfriend.bot.model.entity.VipUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * VIP 用户仓库
 * 用于查询数据库 t_vip_user 表，判断某个微信ID是否是 VIP
 */
@Repository
public interface VipUserRepository extends JpaRepository<VipUser, String> {
}