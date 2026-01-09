/* 1. 切换到 ai_girlfriend 数据库 */
USE ai_girlfriend;

/* 2. 如果表已存在则删除（防止结构冲突，新项目没数据时可用） */
DROP TABLE IF EXISTS `chat_history`;

/* 3. 创建聊天记录表 */
CREATE TABLE `chat_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  `chat_user` varchar(255) NOT NULL COMMENT '聊天对象的微信昵称（例如：略略略）',
  
  `content` text NOT NULL COMMENT '聊天内容（支持长文本和Emoji）',
  
  `role` varchar(20) NOT NULL COMMENT '角色：USER(用户) 或 AI(机器人)',
  
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
  
  PRIMARY KEY (`id`),
  
  /* 索引优化：因为我们经常查“某个用户最近的N条记录”，所以建立联合索引 */
  KEY `idx_user_time` (`chat_user`, `create_time`) USING BTREE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI女友聊天记忆表';


CREATE TABLE user_relationship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_user VARCHAR(255) NOT NULL UNIQUE, -- 微信昵称
    score INT DEFAULT 0,                    -- 好感度分数 (0-100)
    level_name VARCHAR(50),                 -- 当前等级名称 (如: 陌生人)
    update_time DATETIME
);

ALTER DATABASE ai_girlfriend CHARACTER SET = utf8mb4 COLLATE = UTF8MB4_UNICODE_CI;
ALTER TABLE user_relationship CONVERT TO CHARACTER SET utf8mb4 COLLATE UTF8MB4_UNICODE_CI;
ALTER TABLE chat_history CONVERT TO CHARACTER SET utf8mb4 COLLATE UTF8MB4_UNICODE_CI;
SHOW CREATE TABLE user_relationship;
