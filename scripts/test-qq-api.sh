#!/usr/bin/env bash
# 测试 QQ 消息 HTTP API 是否可读（无需 Java）
# 用法:
#   ./scripts/test-qq-api.sh
#   ./scripts/test-qq-api.sh http://192.168.0.251:8765 <api_key>
#   ./scripts/test-qq-api.sh http://127.0.0.1:8765   # 在跑 API 的手机本机上

set -euo pipefail

BASE_URL="${1:-http://192.168.0.251:8765}"
API_KEY="${2:-decb6c40bd4e70aff273a71bc25583ace73749972b11874b}"
CURSOR="${3:-0}"
LIMIT="${4:-10}"

BASE_URL="${BASE_URL%/}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }

echo "========================================"
echo " QQ 消息 API 测试"
echo " 地址: $BASE_URL"
echo " key:  ${API_KEY:0:8}...${API_KEY: -6}"
echo " cursor=$CURSOR limit=$LIMIT"
echo "========================================"
echo

echo "[1/4] GET /health (无需鉴权)"
HEALTH=$(curl -sS -m 8 "$BASE_URL/health" || true)
if echo "$HEALTH" | grep -q '"ok":true'; then
  green "  OK: $HEALTH"
else
  red "  失败: $HEALTH"
  yellow "  提示: 若在本机测手机 API，请用 http://192.168.0.251:8765"
  exit 1
fi
echo

echo "[2/4] GET /api/v1/qq/messages (Bearer 鉴权)"
RESP_BEARER=$(curl -sS -m 8 \
  -H "Authorization: Bearer $API_KEY" \
  "$BASE_URL/api/v1/qq/messages?cursor=$CURSOR&limit=$LIMIT" || true)
if echo "$RESP_BEARER" | grep -q '"ok":true'; then
  green "  OK (Bearer)"
else
  red "  失败 (Bearer): $RESP_BEARER"
fi
echo

echo "[3/4] GET /api/v1/qq/messages (?key= 鉴权)"
RESP_QUERY=$(curl -sS -m 8 \
  "$BASE_URL/api/v1/qq/messages?cursor=$CURSOR&limit=$LIMIT&key=$API_KEY" || true)
if echo "$RESP_QUERY" | grep -q '"ok":true'; then
  green "  OK (query key)"
else
  red "  失败 (query key): $RESP_QUERY"
fi
echo

echo "[4/4] 解析最新消息"
if command -v python3 >/dev/null 2>&1; then
  python3 - "$RESP_BEARER" <<'PY'
import json, sys
raw = sys.argv[1]
try:
    data = json.loads(raw)
except json.JSONDecodeError:
    print("  无法解析 JSON")
    sys.exit(0)
if not data.get("ok"):
    print("  API 返回 ok=false:", data.get("error", data))
    sys.exit(0)
print(f"  next_cursor: {data.get('next_cursor')}")
msgs = data.get("messages") or []
if not msgs:
    print("  (暂无新消息)")
else:
    print(f"  共 {len(msgs)} 条:")
    for m in msgs:
        group = m.get("group", "?")
        sender = m.get("sender", "?")
        text = m.get("message", "")
        ts = m.get("ts", "")
        print(f"    [{ts}] [{group}] {sender}: {text}")
PY
else
  echo "  (安装 python3 可美化输出，原始 JSON 见上)"
  echo "$RESP_BEARER"
fi

echo
if echo "$RESP_BEARER" | grep -q '"ok":true'; then
  green "结论: API 可读，key 正确。"
  yellow "若 MC 插件仍 401，请检查手机端 plugins/GoodMC/config.yml 里的 qq-bridge.api-key 是否与手机 api.key 一致。"
else
  red "结论: 鉴权失败。请在手机上执行:"
  echo '  adb shell "su -c cat /data/adb/mcserver/qq-notify/api.key"'
  echo "  把输出的 key 作为第二个参数传给本脚本。"
fi
