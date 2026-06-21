param(
    [int]$TimeoutSec = 5
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
Set-Location $ProjectRoot

function Write-Info($msg)  { Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "[OK]    $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Error($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

$envFile = Join-Path $ProjectRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

$DB_PORT = if ($env:MYSQL_PUBLISH_PORT) { $env:MYSQL_PUBLISH_PORT } else { "12753" }
$BACKEND_PORT = if ($env:BACKEND_PUBLISH_PORT) { $env:BACKEND_PUBLISH_PORT } else { "22753" }
$FRONTEND_PORT = if ($env:FRONTEND_PUBLISH_PORT) { $env:FRONTEND_PUBLISH_PORT } else { "32753" }
$MYSQL_ROOT_PASSWORD = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "root" }

$EXIT_CODE = 0

Write-Info "========== 健康检查开始 =========="
Write-Info ("时间: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))

Write-Info ""
Write-Info "1. 容器状态检查"

foreach ($service in @("db", "backend", "frontend")) {
    try {
        $json = docker compose ps --format json $service 2>$null
        if ($LASTEXITCODE -eq 0 -and $json) {
            $info = $json | ConvertFrom-Json -ErrorAction SilentlyContinue
            $state = if ($info.State) { $info.State } else { "unknown" }
            $health = if ($info.Health) { $info.Health } else { "none" }

            if ($state -eq "running") {
                if ($health -eq "healthy" -or $health -eq "none") {
                    Write-Ok "  $service : running (health: $health)"
                } else {
                    Write-Warn "  $service : running (health: $health)"
                    $EXIT_CODE = 1
                }
            } else {
                Write-Error "  $service : $state"
                $EXIT_CODE = 1
            }
        } else {
            Write-Error "  $service : not found"
            $EXIT_CODE = 1
        }
    } catch {
        Write-Error "  $service : check failed"
        $EXIT_CODE = 1
    }
}

Write-Info ""
Write-Info "2. Backend 健康检查端点"

function Test-HttpEndpoint {
    param(
        [string]$Url,
        [int]$ExpectedStatus = 200,
        [string]$Name = $Url
    )

    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSec
        if ($resp.StatusCode -eq $ExpectedStatus) {
            Write-Ok "  $Name [$($resp.StatusCode)]"
            return $resp
        } else {
            Write-Error "  $Name [$($resp.StatusCode)] (expected: $ExpectedStatus)"
            $script:EXIT_CODE = 1
            return $null
        }
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        Write-Error "  $Name [$status] $($_.Exception.Message)"
        $script:EXIT_CODE = 1
        return $null
    }
}

Test-HttpEndpoint -Url "http://localhost:${BACKEND_PORT}/actuator/health" -Name "/actuator/health" | Out-Null
Test-HttpEndpoint -Url "http://localhost:${BACKEND_PORT}/api/health" -Name "/api/health" | Out-Null

Write-Info ""
Write-Info "3. Backend 详细健康状态"

try {
    $resp = Invoke-WebRequest -Uri "http://localhost:${BACKEND_PORT}/api/health/detail" -UseBasicParsing -TimeoutSec $TimeoutSec
    $data = ($resp.Content | ConvertFrom-Json).data
    Write-Info "  应用名: $($data.applicationName)"
    Write-Info "  系统状态: $($data.status)"
    Write-Info "  数据库状态: $($data.database.status)"
    Write-Info "  运行时长: $($data.uptimeSeconds) 秒"

    if ($data.status -eq "UP") {
        Write-Ok "  系统整体健康"
    } else {
        Write-Error "  系统状态异常: $($data.status)"
        $EXIT_CODE = 1
    }
} catch {
    Write-Error "  获取详细健康状态失败: $($_.Exception.Message)"
    $EXIT_CODE = 1
}

Write-Info ""
Write-Info "4. Frontend 访问检查"
Test-HttpEndpoint -Url "http://localhost:${FRONTEND_PORT}/" -Name "Frontend 首页" | Out-Null

Write-Info ""
if ($EXIT_CODE -eq 0) {
    Write-Ok "========== 健康检查全部通过 =========="
    Write-Info "访问地址:"
    Write-Info "  前端: http://localhost:$FRONTEND_PORT"
    Write-Info "  后端API: http://localhost:$BACKEND_PORT/api/health"
} else {
    Write-Error "========== 健康检查存在问题 (exit=$EXIT_CODE) =========="
}

exit $EXIT_CODE
