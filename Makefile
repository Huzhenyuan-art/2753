.PHONY: help start dev stop restart build rebuild test lint status logs health clean

help:
	@echo "可用命令:"
	@echo "  make start      - 启动生产环境"
	@echo "  make dev        - 启动开发环境"
	@echo "  make stop       - 停止所有服务"
	@echo "  make restart    - 重启服务"
	@echo "  make build      - 构建镜像"
	@echo "  make rebuild    - 无缓存重新构建并启动"
	@echo "  make test       - 运行自动化测试"
	@echo "  make lint       - 代码检查"
	@echo "  make status     - 查看服务状态"
	@echo "  make logs       - 查看服务日志"
	@echo "  make health     - 健康检查"
	@echo "  make clean      - 清理容器和卷"

start:
	@./run.sh start

dev:
	@./run.sh dev

stop:
	@./run.sh stop

restart:
	@./run.sh restart

build:
	@./run.sh build

rebuild:
	@./run.sh rebuild

test:
	@./run.sh test

lint:
	@./run.sh lint

status:
	@./run.sh status

logs:
	@./run.sh logs

health:
	@./run.sh health

clean:
	@./run.sh clean
