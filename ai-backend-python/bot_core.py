import time
import os
import queue
from wxauto import WeChat
import config
from bridge_java import push_to_java

class WeChatBot:
    def __init__(self):
        print("--- 正在初始化微信客户端 ---")
        self.wx = WeChat()
        
        # 发送任务队列 (由 server.py 入队)
        self.send_queue = queue.Queue()
        
        # 消息去重缓存 { '好友名': '最后一条消息指纹' }
        self.msg_cache = {}
        
        # 记录机器人当前打开的聊天窗口名
        self.current_chat = None
        
        # 从 config 获取自身昵称用于过滤
        self.my_name = getattr(config, 'SELF_WX_NAME', None)
        if not self.my_name:
            print("⚠️ 警告: config.py 中未设置 SELF_WX_NAME，防回环能力将减弱！")
        
        # 初始化：先清空一次当前窗口的旧消息指纹
        self._init_cache()

    def _init_cache(self):
        """初始化时，将当前窗口最后一条消息设为已读"""
        try:
            self.wx.SwitchToChat()
            msgs = self.wx.GetAllMessage()
            if msgs:
                self.msg_cache["__INIT__"] = self._gen_fp(msgs[-1])
        except:
            pass

    def _gen_fp(self, msg):
        """生成消息唯一指纹：发送者 + 内容 + ID (如果有)"""
        sender = getattr(msg, 'sender', 'Unknown')
        content = getattr(msg, 'content', '')
        msg_id = getattr(msg, 'id', '0')
        return f"{sender}_{content}_{msg_id}"

    def send_msg(self, who, content):
        """由 server.py 调用，将任务放入队列"""
        self.send_queue.put({"who": who, "content": content})

    def _handle_send_queue_step(self):
        """
        [核心修改] 单步发送：每次只处理队列中的一个气泡。
        发完立即返回，以便主循环能穿插执行“读取消息”逻辑。
        """
        if not self.send_queue.empty():
            try:
                task = self.send_queue.get_nowait()
                who = task['who']
                content = task['content']

                # 只有当目标窗口不是当前窗口时，才执行切换 (耗时操作)
                if self.current_chat != who:
                    self.wx.ChatWith(who)
                    self.current_chat = who

                # 执行发送
                if content.startswith("[FILE]"):
                    file_path = content.replace("[FILE]", "")
                    if os.path.exists(file_path):
                        self.wx.SendFiles(file_path)
                    else:
                        print(f"❌ 找不到文件: {file_path}")
                else:
                    self.wx.SendMsg(content)
                
                print(f"📤 发送完毕: [{who}] -> {content[:15]}...")

                # 发送后关键动作：立即强制更新该窗口的指纹缓存，防止读回自己的话
                time.sleep(0.2) 
                self._sync_cache(who)
                
            except queue.Empty:
                pass
            except Exception as e:
                print(f"❌ 发送异常: {e}")

    def _sync_cache(self, who):
        """同步特定窗口的最后一条消息到缓存"""
        try:
            msgs = self.wx.GetAllMessage()
            if msgs:
                self.msg_cache[who] = self._gen_fp(msgs[-1])
        except:
            pass

    def _process_msg(self, msg, who):
        """解析并推送消息给 Java"""
        # 🌟 防回复回环：核心逻辑
        # 1. 检查微信底层标识 'Self'
        # 2. 检查 sender 字符串是否等于 config 中的自己名字
        if msg.sender == 'Self' or msg.sender == self.my_name:
            # print(f"DEBUG: 过滤掉自发消息: {msg.content[:10]}")
            return

        # 过滤系统消息和思维链
        if msg.type == 'sys' or "<think>" in msg.content:
            return

        print(f"📩 收到新消息 [{who}]: {msg.content}")

        final_content = msg.content
        if msg.type in ('image', 'video', 'file'):
            try:
                save_path = msg.download()
                final_content = f"[FILE]{save_path}"
            except:
                final_content = "[文件下载失败]"

        # 推送到 Java 异步处理
        push_to_java({
            "sender": who,
            "content": final_content,
            "type": msg.type,
            "timestamp": int(time.time() * 1000)
        })

    def _read_current_window(self):
        """读取当前正停留窗口的新消息"""
        if not self.current_chat:
            return
        
        try:
            msgs = self.wx.GetAllMessage()
            if not msgs:
                return

            last_msg = msgs[-1]
            last_fp = self._gen_fp(last_msg)

            # 比对缓存，如果是新消息且不是刚才初始化的那条
            if self.msg_cache.get(self.current_chat) != last_fp and self.msg_cache.get("__INIT__") != last_fp:
                self._process_msg(last_msg, self.current_chat)
                # 更新指纹
                self.msg_cache[self.current_chat] = last_fp
        except Exception as e:
            # print(f"读取窗口失败: {e}")
            pass

    def _scan_unreads(self):
        """扫描左侧列表的红点未读消息"""
        try:
            # 获取有未读标记的聊天
            new_data = self.wx.GetNextNewMessage()
            
            target_chat = None
            if new_data:
                # 兼容不同版本的 wxauto 返回格式
                if isinstance(new_data, dict):
                    if 'chat_name' in new_data:
                        target_chat = new_data['chat_name']
                    else:
                        for name in new_data.keys():
                            if name not in ['chat_name', 'msg', 'chat_type', 'type']:
                                target_chat = name
                                break
            
            if target_chat:
                print(f"👀 发现未读消息来自: {target_chat}")
                self.wx.ChatWith(target_chat)
                self.current_chat = target_chat
                # 切换后立即读一次
                self._read_current_window()
        except:
            pass

    def run_listener(self):
        """主循环： UI 操作必须在主线程"""
        print(f"✅ 微信监听器就绪 (自身过滤名: {self.my_name})")
        
        while True:
            try:
                # 1. 发送逻辑：每次循环只发一个气泡
                self._handle_send_queue_step()
                
                # 2. 监听逻辑 A：看一眼当前窗口有没有新出的消息 (应对插嘴)
                self._read_current_window()
                
                # 3. 监听逻辑 B：看一眼左侧有没有其他人的红点
                self._scan_unreads()
                
                # 控制循环频率，避免 CPU 占用过高，同时保持灵敏
                time.sleep(0.2)
                
            except KeyboardInterrupt:
                print("🛑 停止监听")
                break
            except Exception as e:
                print(f"⚠️ 监听循环异常: {e}")
                time.sleep(1)

# 创建单例对象供 server.py 使用
bot = WeChatBot()