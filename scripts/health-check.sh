#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

if [ -f ".env" ]; then
    set -a
    source .env
    set +a
fi

DB_PORT="${MYSQL_PUBLISH_PORT:-12753}"
BACKEND_PORT="${BACKEND_PUBLISH_PORT:-22753}"
FRONTEND_PORT="${FRONTEND_PUBLISH_PORT:-32753}"
BACKEND_USERNAME="${HEALTH_CHECK_USERNAME:-admin}"
BACKEND_PASSWORD="${HEALTH_CHECK_PASSWORD:-admin123}"

EXIT_CODE=0

log_info "========== 健康检查开始 =========="
log_info "时间: $(date '+%Y-%m-%d %H:%M:%S')"

log_info ""
log_info "1. 容器状态检查"

for service in db backend frontend; do
    state=$(docker compose ps --format json "$service" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('State','unknown'))" 2>/dev/null || echo "not-found")
    health=$(docker compose ps --format json "$service" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Health','none'))" 2>/dev/null || echo "none")

    if [ "$state" = "running" ]; then
        if [ "$health" = "healthy" ] || [ "$health" = "none" ]; then
            log_ok "  $service: running (health: $health)"
        else
            log_warn "  $service: running (health: $health)"
            EXIT_CODE=1
        fi
    else
        log_error "  $service: $state"
        EXIT_CODE=1
    fi
done

log_info ""
log_info "2. MySQL 连接检查"
if command -v mysql &> /dev/null; then
    if mysqladmin ping -h 127.0.0.1 -P "$DB_PORT" -u root -p"${MYSQL_ROOT_PASSWORD:-root}" --silent 2>/dev/null; then
        log_ok "  MySQL 连接成功 (端口: $DB_PORT)"
    else
        log_error "  MySQL 连接失败"
        EXIT_CODE=1
    fi
else
    log_warn "  mysql 客户端不可用，跳过 MySQL 连接测试"
fi

log_info ""
log_info "3. Backend 健康检查端点"

check_http() {
    local url="$1"
    local expected_status="${2:-200}"
    local name="${3:-$url}"

    if command -v curl &> /dev/null; then
        local status
        status=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$url" || echo "000")
        if [ "$status" = "$expected_status" ]; then
            log_ok "  $name [$status]"
            return 0
        else
            log_error "  $name [$status] (expected: $expected_status)"
            return 1
        fi
    else
        log_warn "  curl 不可用，跳过 $name"
        return 0
    fi
}

check_http "http://localhost:${BACKEND_PORT}/actuator/health" 200 "/actuator/health" || EXIT_CODE=1
check_http "http://localhost:${BACKEND_PORT}/api/health" 200 "/api/health" || EXIT_CODE=1

log_info ""
log_info "4. Backend 详细健康状态"
if command -v curl &> /dev/null; then
    response=$(curl -s --connect-timeout 5 "http://localhost:${BACKEND_PORT}/api/health/detail" 2>/dev/null || echo "{}")
    status=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status','N/A'))" 2>/dev/null || echo "parse-failed")
    db_status=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('database',{}).get('status','N/A'))" 2>/dev/null || echo "parse-failed")
    app_name=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('applicationName','N/A'))" 2>/dev/null || echo "parse-failed")
    uptime=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('uptimeSeconds','N/A'))" 2>/dev/null || echo "parse-failed")

    log_info "  应用名: $app_name"
    log_info "  系统状态: $status"
    log_info "  数据库状态: $db_status"
    log_info "  运行时长: $uptime 秒"

    if [ "$status" = "UP" ]; then
        log_ok "  系统整体健康"
    else
        log_error "  系统状态异常: $status"
        EXIT_CODE=1
    fi
fi

log_info ""
log_info "5. Frontend 访问检查"
check_http "http://localhost:${FRONTEND_PORT}/" 200 "Frontend 首页" || EXIT_CODE=1

log_info ""
log_info "6. API 登录验证"
if command -v curl &> /dev/null; then
    login_response=$(curl -s -X POST \
        "http://localhost:${BACKEND_PORT}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${BACKEND_USERNAME}\",\"password\":\"${BACKEND_PASSWORD}\"}" \
        --connect-timeout 5 --max-time 10 2>/dev/null || echo "{}")

    login_code=$(echo "$login_response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('code',-1))" 2>/dev/null || echo "-1")
    if [ "$login_code" = "200" ] || [ "$login_code" = "0" ]; then
        log_ok "  API 登录验证成功"
    else
        log_warn "  API 登录验证返回 code=$login_code (可能是测试账号问题)"
    fi
fi

log_info ""
if [ $EXIT_CODE -eq 0 ]; then
    log_ok "========== 健康检查全部通过 =========="
else
    log_error "========== 健康检查存在问题 (exit=$EXIT_CODE) =========="
fi

exit $EXIT_CODE
