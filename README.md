# 🚀 微信 AI 恋爱机器人 (本地全栈版) 部署指南

欢迎使用本系统！这是一个基于 **Python + Java + MySQL + Ollama** 的全栈 AI 机器人项目。
本系统具备**时间感知、好感度积累、表情包互动**等高级功能，且数据完全本地化，安全隐私。

---

## 📂 1. 项目目录结构说明
解压后，您将看到以下文件结构，请先核对：

```text
Project_Root/
├── 📂 backend-java/          # Java Spring Boot 后端源码
│   ├── resources/application.yml  # 核心配置文件 (修改数据库/AI配置在这里)
│   └── GirlfriendBotApplication  # 运行文件夹点击运行
├── 📂 ai-backend-python/         # Python 前端源码
│   ├── main.py          # 启动入口
│── 📂 素材/表情包       # 表情包图片存放目录
├── 📂 sql/              # 数据库初始化文件
│   └── init.txt
└── README.md            # 本说明书
```

## 🛠️ 2. 环境准备 (必读)
在运行代码前，请确保您的电脑已安装以下环境：
操作系统：建议 Windows 10/11。
Java 环境：JDK 17 或以上版本。
Python 环境：Python 3.9 或以上版本。
数据库：MySQL
AI 模型运行器：Ollama (用于加载本地大模型)。
微信 PC 版：微信3.9 版本，一定要这个版本的

## ⚡ 3. 极速部署步骤

以下的东西全部要有，否则可能会报错。
## 第 1 步：下载微信3.9版本
下载过程请参照着下面网址教程来：
https://github.com/Skyler1n/WeChat3.9-32bit-Compatibility-Launcher

## 第 2 步：配置数据库 (MySQL) 🗄️
如果没有MySql的话，需要先安装一个MySql数据库
打开您的数据库管理工具 (HeidSQL/Navicat / DBeaver / SQLyog)。
新建一个数据库，命名为 ai_girlfriend。
运行项目目录 sql/init.txt的sql命令文件，导入数据表结构（包含消息记录表、好感度表等）。

## 第 3 步：部署 AI 模型 (Ollama) 🧠
照着ollama安装文档进行安装：https://www.cnblogs.com/pcdoctor/p/19322600
安装好 Ollama 后，打开命令行终端 (CMD / PowerShell)。依次输入：
```
ollama pull qwen2.5:7b
ollama run qwen2.5:7b
```
测试：在终端输入“你好”，如果 AI 能回复，说明部署成功。
保持ollama不要关闭就行了

## 第 4 步：启动 Java 后端 (中枢神经) ☕
通过软件idea打开并进入"backend-java"文件夹。
修改配置：打开 application.yml，找到以下部分并修改：
```
Yaml
spring:
  application:
    name: ai-girlfriend-bot
  datasource:
    url: jdbc:mysql://localhost:3306/ai_girlfriend?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true
    username: root          # 改为你的数据库账号
    password: your_password # 改为你的数据库密码
    
bot:
  target-user: "略略略"           #这个不用设置也行，可有可无
  python-api: "http://localhost:5000/send"
  
  ai-mode: "qwen2.5:7b" #Ollama下载的ai模型
```
还需要改一个表情包存放的路径，打开"src/main/java/com/girlfriend/bot/service/StickerFactory.java"的代码，修改第14行代码，修改为下载的表情包的位置
```
String basePath = "C:\\Users\\user\\Desktop\\code\\自娱自乐小代码\\后端\\ai对话机器人\\素材\\表情包\\"; #修改成自己的位置
```

找到并进入GirlfriendBotApplication文件点击运行

## 第 5 步：启动 Python 前端 (手眼交互) 🐍
使用vs code启动“ai-backend-python“文件夹，先在config.py文件中设置自己这个ai号的微信名字，
```
# config.py
# Java 接收消息的接口地址
JAVA_API_URL = "http://localhost:8080/api/wechat/receive" 

# Python 监听端口
PYTHON_SERVER_PORT = 5000

# 自己的微信名 (用于过滤)
SELF_WX_NAME = "嘻嘻嘻" #在这里设置本微信号的名字
```
运行main.py文件，
”import 包名”报错，就运行“pip install 包名”安装相应的包名即可，很快的

## 🌟 4. 功能说明
好感度系统：
系统会自动分析聊天内容，根据情绪正负值调整好感度。
数据存储在数据库 favorability_table 表中，您可以手动修改数据库数值来“作弊”。
时间感知：
AI 会自动读取系统时间。深夜聊天时，它会提示你早点睡；早晨会说早安。
表情包发送：
将你喜欢的表情包图片放入 frontend/assets 文件夹。
触发特定关键词或情绪时，系统会自动发送对应的图片。
自动化任务：
好感度满级会触发自动化任务，比如固定时间点触发早安、晚安
固定时长没有消息会主动发消息等




















