import threading
from server import app
from bot_core import bot
import config

def run_flask():
    """在独立线程运行 Flask Server"""
    app.run(host='0.0.0.0', port=config.PYTHON_SERVER_PORT, debug=False, use_reloader=False)

if __name__ == '__main__':
    print("--- 启动程序 ---")
    
    # 1. 启动 Flask 线程 (API 服务)
    t_flask = threading.Thread(target=run_flask, daemon=True)
    t_flask.start()
    print(f"🚀 Flask 服务器运行在端口 {config.PYTHON_SERVER_PORT}")

    # 2. 主线程运行 Bot 逻辑 (控制 UI 必须在主线程)
    bot.run_listener()



    