param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "dev", "stop", "restart", "build", "rebuild", "test", "lint", "status", "logs", "health", "clean", "help")]
    [string]$Command = "start",

    [string]$Service = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

function Write-Info($msg)  { Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "[OK]    $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Error($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

function Test-EnvFile {
    if (-not (Test-Path ".env")) {
        Write-Warn ".env 文件不存在，正在从 .env.example 创建..."
        if (Test-Path ".env.example") {
            Copy-Item ".env.example" ".env"
            Write-Warn "请编辑 .env 文件修改默认密码和密钥！"
        } else {
            Write-Error ".env.example 不存在"
            exit 1
        }
    }
}

function Test-Docker {
    try {
        $null = docker --version 2>&1
    } catch {
        Write-Error "Docker 未安装"
        exit 1
    }
    try {
        $null = docker info 2>&1
    } catch {
        Write-Error "Docker 守护进程未运行"
        exit 1
    }
}

function Show-Help {
    Write-Host @"
用法: .\run.ps1 [命令] [-Service <名称>]

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
  logs          查看服务日志 (-Service 指定服务)
  health        健康检查
  clean         清理容器和卷
  help          显示此帮助信息

示例:
  .\run.ps1                    # 一键启动生产环境
  .\run.ps1 dev                # 启动开发环境
  .\run.ps1 test               # 运行测试
  .\run.ps1 health             # 健康检查
  .\run.ps1 logs -Service db   # 查看数据库日志
"@
}

function Invoke-Start {
    Write-Info "启动生产环境服务..."
    Test-EnvFile
    Test-Docker
    docker compose up -d --build
    Write-Ok "生产环境启动完成"
    Invoke-Health
}

function Invoke-Dev {
    Write-Info "启动开发环境服务..."
    Test-Docker
    docker compose -f docker-compose.dev.yml up -d --build
    Write-Ok "开发环境启动完成"
    Write-Info "前端开发服务: http://localhost:32753"
    Write-Info "后端调试端口: 5005"
}

function Invoke-Stop {
    Write-Info "停止所有服务..."
    docker compose down 2>$null
    docker compose -f docker-compose.dev.yml down 2>$null
    Write-Ok "所有服务已停止"
}

function Invoke-Restart {
    Write-Info "重启生产环境服务..."
    docker compose restart
    Write-Ok "服务已重启"
}

function Invoke-Build {
    Write-Info "构建生产镜像..."
    docker compose build
    Write-Ok "镜像构建完成"
}

function Invoke-Rebuild {
    Write-Info "无缓存重新构建..."
    Invoke-Stop
    docker compose build --no-cache
    Invoke-Start
}

function Invoke-Lint {
    Write-Info "运行代码检查..."
    $failed = $false

    Write-Info "检查前端 TypeScript..."
    if (Test-Path "frontend\package.json") {
        Push-Location frontend
        try {
            npm ci
            npm run type-check
        } catch {
            $failed = $true
        }
        Pop-Location
    }

    Write-Info "检查后端编译..."
    if (Test-Path "backend\pom.xml") {
        Push-Location backend
        try {
            mvn -s settings.xml compile -q -B
        } catch {
            $failed = $true
        }
        Pop-Location
    }

    if (-not $failed) {
        Write-Ok "代码检查通过"
    } else {
        Write-Error "代码检查失败"
        exit 1
    }
}

function Invoke-Test {
    Write-Info "运行自动化测试..."
    $failed = $false

    Write-Info "运行后端测试..."
    if (Test-Path "backend\pom.xml") {
        Push-Location backend
        try {
            mvn -s settings.xml test -B
        } catch {
            $failed = $true
        }
        Pop-Location
    }

    Write-Info "运行前端类型检查..."
    if (Test-Path "frontend\package.json") {
        Push-Location frontend
        try {
            npm ci --silent
            npm run type-check
        } catch {
            $failed = $true
        }
        Pop-Location
    }

    if (-not $failed) {
        Write-Ok "所有测试通过"
    } else {
        Write-Error "测试失败"
        exit 1
    }
}

function Invoke-Status {
    Write-Info "服务状态:"
    docker compose ps 2>$null
    docker compose -f docker-compose.dev.yml ps 2>$null
}

function Invoke-Logs {
    if ($Service) {
        docker compose logs -f $Service
    } else {
        docker compose logs -f
    }
}

function Invoke-Health {
    Write-Info "执行健康检查..."

    $dbPort = "12753"
    $backendPort = "22753"
    $frontendPort = "32753"

    $allHealthy = $true

    Write-Info "检查 MySQL (端口 $dbPort)..."
    $dbStatus = docker compose ps --format json db 2>$null | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($dbStatus -and $dbStatus.Health -eq "healthy") {
        Write-Ok "MySQL 服务健康"
    } else {
        Write-Warn "MySQL 服务未就绪"
        $allHealthy = $false
    }

    Write-Info "检查 Backend (端口 $backendPort)..."
    $beStatus = docker compose ps --format json backend 2>$null | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($beStatus -and $beStatus.Health -eq "healthy") {
        Write-Ok "Backend 服务健康"
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:$backendPort/actuator/health" -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -eq 200) {
                Write-Ok "Backend HTTP 健康检查通过"
            }
        } catch {
            Write-Warn "Backend HTTP 健康检查失败"
        }
    } else {
        Write-Warn "Backend 服务未就绪"
        $allHealthy = $false
    }

    Write-Info "检查 Frontend (端口 $frontendPort)..."
    $feStatus = docker compose ps --format json frontend 2>$null | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($feStatus -and $feStatus.State -eq "running") {
        Write-Ok "Frontend 服务运行中"
        try {
            $resp = Invoke-WebRequest -Uri "http://localhost:$frontendPort/" -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -eq 200) {
                Write-Ok "Frontend HTTP 健康检查通过"
            }
        } catch {
            Write-Warn "Frontend HTTP 健康检查失败"
        }
    } else {
        Write-Warn "Frontend 服务未运行"
        $allHealthy = $false
    }

    if ($allHealthy) {
        Write-Ok "所有服务健康"
        Write-Info "访问地址:"
        Write-Info "  前端: http://localhost:$frontendPort"
        Write-Info "  后端API: http://localhost:$backendPort/api/health"
    } else {
        Write-Warn "部分服务未就绪，请稍后重试"
    }
}

function Invoke-Clean {
    Write-Warn "将清理所有容器和数据卷！"
    $confirm = Read-Host "确认继续? (y/N)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Info "已取消"
        return
    }
    docker compose down -v --remove-orphans 2>$null
    docker compose -f docker-compose.dev.yml down -v --remove-orphans 2>$null
    Write-Ok "清理完成"
}

switch ($Command) {
    "start"   { Invoke-Start }
    "dev"     { Invoke-Dev }
    "stop"    { Invoke-Stop }
    "restart" { Invoke-Restart }
    "build"   { Invoke-Build }
    "rebuild" { Invoke-Rebuild }
    "test"    { Invoke-Test }
    "lint"    { Invoke-Lint }
    "status"  { Invoke-Status }
    "logs"    { Invoke-Logs }
    "health"  { Invoke-Health }
    "clean"   { Invoke-Clean }
    "help"    { Show-Help }
    default   { Show-Help }
}
