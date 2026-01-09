import requests
import config 
from concurrent.futures import ThreadPoolExecutor

# 创建一个最大容纳 5 个并发请求的线程池
executor = ThreadPoolExecutor(max_workers=5)

def _do_post(payload):
    """实际执行 HTTP POST 请求的函数"""
    try:
        # 调试输出
        content_snippet = str(payload.get('content', 'No Content'))[:20].replace('\n', ' ')
        print(f"[Python -> Java] 推送: {content_snippet}...")
        
        # 设置超时时间
        response = requests.post(config.JAVA_API_URL, json=payload, timeout=3)
        
        if response.status_code != 200:
            print(f"❌ Java端返回错误: {response.status_code}, Response: {response.text[:50]}")
    except Exception as e:
        print(f"❌ 推送 Java 失败: {e}")

def push_to_java(msg_data):
    """
    将消息推送任务提交给线程池
    """
    executor.submit(_do_post, msg_data)