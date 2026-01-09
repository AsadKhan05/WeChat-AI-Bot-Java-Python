package com.girlfriend.bot.controller;

import com.girlfriend.bot.model.dto.WeChatMsgDTO;
import com.girlfriend.bot.model.entity.VipUser;
import com.girlfriend.bot.repository.VipUserRepository;
import com.girlfriend.bot.service.ChatService;
import com.girlfriend.bot.service.VipChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/wechat")
public class WeChatController {

    @Autowired
    private ChatService chatService;       // 普通服务

    @Autowired
    private VipChatService vipChatService; // 🟢 VIP 服务 (必须注入)

    @Autowired
    private VipUserRepository vipUserRepository; // 🟢 VIP 仓库 (必须注入)

    @PostMapping("/receive")
    public String receiveMessage(@RequestBody WeChatMsgDTO msg) {
        String sender = msg.getSender(); // 这里获取到的就是 "略略略"
        String content = msg.getContent();

        // 🟢 1. 核心逻辑：查询数据库，看这个人是不是 VIP
        boolean isVip = false;
        try {
            Optional<VipUser> vipUserOpt = vipUserRepository.findById(sender);
            if (vipUserOpt.isPresent() && vipUserOpt.get().isValid()) {
                isVip = true;
            }
        } catch (Exception e) {
            System.err.println("VIP鉴权出错，降级为普通用户: " + e.getMessage());
        }

        // 🟢 2. 路由分发
        if (isVip) {
            // 如果是 "略略略"，会进入这里 -> 限制级模式
            System.out.println("👑 尊贵VIP用户 [" + sender + "] 上线，启动限制级服务...");
            vipChatService.processVipMessage(sender, content);
        } else {
            // 其他人 -> 普通绿茶模式
            chatService.processMessage(sender, content);
        }

        return "OK";
    }
}