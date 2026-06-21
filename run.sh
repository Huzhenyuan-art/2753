#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

check_env_file() {
    if [ ! -f ".env" ]; then
        log_warn ".env 文件不存在，正在从 .env.example 创建..."
        if [ -f ".env.example" ]; then
            cp .env.example .env
            log_warn "请编辑 .env 文件修改默认密码和密钥！"
        else
            log_error ".env.example 不存在，无法创建 .env"
            exit 1
        fi
    fi
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    if ! docker info &> /dev/null; then
        log_error "Docker 守护进程未运行，请启动 Docker"
        exit 1
    fi
    if ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装或版本不支持"
        exit 1
    fi
}

usage() {
    cat <<EOF
用法: $0 [命令] [选项]

命令:
  start         启动生产环境服务 (默认)
  dev           启动开发环境服务
  stop          停止所有服务
  restart       重启生产环境服务
  build         构建镜像
  rebuild       无缓存重新构建并启动
  test          运行自动化测试
  lint          运行代码检查
  status        查看服务状态
  logs          查看服务日志
  health        健康检查
  clean         清理容器和卷
  help          显示此帮助信息

选项:
  -s, --service <name>   指定单个服务操作
  -h, --help             显示帮助信息

示例:
  $0                    # 一键启动生产环境
  $0 dev                # 启动开发环境
  $0 test               # 运行测试
  $0 health             # 健康检查
EOF
}

cmd_start() {
    log_info "启动生产环境服务..."
    check_env_file
    check_docker
    docker compose up -d --build
    log_ok "生产环境启动完成"
    cmd_health
}

cmd_dev() {
    log_info "启动开发环境服务..."
    check_docker
    docker compose -f docker-compose.dev.yml up -d --build
    log_ok "开发环境启动完成"
    log_info "前端开发服务: http://localhost:32753"
    log_info "后端调试端口: 5005"
    cmd_health
}

cmd_stop() {
    log_info "停止所有服务..."
    docker compose down 2>/dev/null || true
    docker compose -f docker-compose.dev.yml down 2>/dev/null || true
    log_ok "所有服务已停止"
}

cmd_restart() {
    log_info "重启生产环境服务..."
    docker compose restart
    log_ok "服务已重启"
}

cmd_build() {
    log_info "构建生产镜像..."
    docker compose build
    log_ok "镜像构建完成"
}

cmd_rebuild() {
    log_info "无缓存重新构建..."
    cmd_stop
    docker compose build --no-cache
    cmd_start
}

cmd_lint() {
    log_info "运行代码检查..."
    local failed=0

    log_info "检查前端 TypeScript..."
    if [ -d "frontend" ] && [ -f "frontend/package.json" ]; then
        (cd frontend && npm ci && npm run type-check) || failed=1
    fi

    log_info "检查后端编译..."
    if [ -d "backend" ] && [ -f "backend/pom.xml" ]; then
        (cd backend && mvn -s settings.xml compile -q -B) || failed=1
    fi

    if [ $failed -eq 0 ]; then
        log_ok "代码检查通过"
    else
        log_error "代码检查失败"
        exit 1
    fi
}

cmd_test() {
    log_info "运行自动化测试..."
    local failed=0

    log_info "运行后端测试..."
    if [ -d "backend" ] && [ -f "backend/pom.xml" ]; then
        (cd backend && mvn -s settings.xml test -B) || failed=1
    fi

    log_info "运行前端类型检查..."
    if [ -d "frontend" ] && [ -f "frontend/package.json" ]; then
        (cd frontend && npm ci --silent && npm run type-check) || failed=1
    fi

    if [ $failed -eq 0 ]; then
        log_ok "所有测试通过"
    else
        log_error "测试失败"
        exit 1
    fi
}

cmd_status() {
    log_info "服务状态:"
    docker compose ps 2>/dev/null || true
    docker compose -f docker-compose.dev.yml ps 2>/dev/null || true
}

cmd_logs() {
    local service="${1:-}"
    if [ -n "$service" ]; then
        docker compose logs -f "$service"
    else
        docker compose logs -f
    fi
}

cmd_health() {
    log_info "执行健康检查..."

    local db_port="${MYSQL_PUBLISH_PORT:-12753}"
    local backend_port="${BACKEND_PUBLISH_PORT:-22753}"
    local frontend_port="${FRONTEND_PUBLISH_PORT:-32753}"

    local all_healthy=true

    log_info "检查 MySQL (端口 $db_port)..."
    if docker compose ps db 2>/dev/null | grep -q "healthy"; then
        log_ok "MySQL 服务健康"
    else
        log_warn "MySQL 服务未就绪"
        all_healthy=false
    fi

    log_info "检查 Backend (端口 $backend_port)..."
    if docker compose ps backend 2>/dev/null | grep -q "healthy"; then
        log_ok "Backend 服务健康"
        if command -v curl &> /dev/null; then
            local status
            status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$backend_port/actuator/health" || echo "000")
            if [ "$status" = "200" ]; then
                log_ok "Backend HTTP 健康检查通过"
            else
                log_warn "Backend HTTP 健康检查返回 $status"
            fi
        fi
    else
        log_warn "Backend 服务未就绪"
        all_healthy=false
    fi

    log_info "检查 Frontend (端口 $frontend_port)..."
    if docker compose ps frontend 2>/dev/null | grep -q "Up"; then
        log_ok "Frontend 服务运行中"
        if command -v curl &> /dev/null; then
            local status
            status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$frontend_port/" || echo "000")
            if [ "$status" = "200" ]; then
                log_ok "Frontend HTTP 健康检查通过"
            else
                log_warn "Frontend HTTP 健康检查返回 $status"
            fi
        fi
    else
        log_warn "Frontend 服务未运行"
        all_healthy=false
    fi

    if $all_healthy; then
        log_ok "所有服务健康"
        log_info "访问地址:"
        log_info "  前端: http://localhost:$frontend_port"
        log_info "  后端API: http://localhost:$backend_port/api/health"
    else
        log_warn "部分服务未就绪，请稍后重试"
    fi
}

cmd_clean() {
    log_warn "将清理所有容器和数据卷！"
    read -p "确认继续? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "已取消"
        exit 0
    fi
    docker compose down -v --remove-orphans 2>/dev/null || true
    docker compose -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true
    log_ok "清理完成"
}

main() {
    local command="start"
    local service=""

    while [ $# -gt 0 ]; do
        case "$1" in
            start|dev|stop|restart|build|rebuild|test|lint|status|logs|health|clean|help)
                command="$1"
                shift
                ;;
            -s|--service)
                service="${2:-}"
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
    done

    case "$command" in
        start)   cmd_start ;;
        dev)     cmd_dev ;;
        stop)    cmd_stop ;;
        restart) cmd_restart ;;
        build)   cmd_build ;;
        rebuild) cmd_rebuild ;;
        test)    cmd_test ;;
        lint)    cmd_lint ;;
        status)  cmd_status ;;
        logs)    cmd_logs "$service" ;;
        health)  cmd_health ;;
        clean)   cmd_clean ;;
        help)    usage ;;
        *)       usage; exit 1 ;;
    esac
}

main "$@"
