from flask import Flask, request, jsonify
from bot_core import bot
import config

app = Flask(__name__)

@app.route('/send', methods=['POST'])
def handle_send_command():
    data = request.json
    if not data or 'who' not in data or 'content' not in data:
        return jsonify({"status": "error", "message": "参数缺失"}), 400

    who = data['who']
    content = data['content']

    try:
        # 这里只负责入队
        bot.send_msg(who, content)
        return jsonify({
            "status": "queued", 
            "message": f"消息已加入发送队列: {who}"
        }), 200
    except Exception as e:
        print(f"❌ 入队失败: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500