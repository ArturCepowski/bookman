# Builds the backend and installs it on the server (Windows PowerShell, run from the repository root):
#   .\deploy\deploy-backend.ps1 -Key C:\Users\<you>\.ssh\bookman-oracle.key
# Every step reports OK or stops with "DEPLOYMENT FAILED" (exit code 1). At the end the script waits until the
# backend answers over HTTPS. Flyway migrations run automatically when the service restarts. See docs/deployment.md.
# Keep this file ASCII-only: Windows PowerShell 5.1 reads scripts without a BOM in the legacy code page.
param(
    [Parameter(Mandatory = $true)] [string] $Key,
    [string] $Server = "ubuntu@bookman-artur.duckdns.org",
    [string] $HealthUrl = "https://bookman-artur.duckdns.org/api/users/me",
    [int] $StartupTimeoutSeconds = 120
)
$ErrorActionPreference = "Stop"
$started = Get-Date
$steps = 5

function Step([int] $number, [string] $text) {
    Write-Host ""
    Write-Host "[$number/$steps] $text" -ForegroundColor Cyan
}

function Ok([string] $text) {
    Write-Host "      OK  $text" -ForegroundColor Green
}

function Warn([string] $text) {
    Write-Host "      !   $text" -ForegroundColor Yellow
}

function Fail([string] $text) {
    Write-Host ""
    Write-Host "DEPLOYMENT FAILED: $text" -ForegroundColor Red
    exit 1
}

# HTTP status of the health URL; 0 when the server cannot be reached at all.
function Get-StatusCode([string] $url) {
    try {
        return [int](Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5).StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

# Windows PowerShell 5.1 may not offer TLS 1.2 by default.
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

Step 1 "Checking prerequisites"
if (-not (Test-Path "backend\gradlew.bat")) { Fail "run the script from the repository root (backend\gradlew.bat not found)" }
if (-not (Test-Path $Key)) { Fail "SSH key not found: $Key" }
foreach ($tool in "ssh", "scp") {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { Fail "$tool not found (install the Windows OpenSSH client)" }
}
$version = "unknown"
if ((Test-Path ".git") -and (Get-Command git -ErrorAction SilentlyContinue)) {
    $version = "$(git rev-parse --abbrev-ref HEAD) @ $(git rev-parse --short HEAD)"
    if (git status --porcelain) { Warn "uncommitted changes will be deployed too" }
    if ((git rev-parse --abbrev-ref HEAD) -ne "main") { Warn "not on main" }
}
Ok "version $version -> $Server"

Step 2 "Building the backend (gradlew bootJar)"
Push-Location backend
& .\gradlew.bat bootJar
$buildExitCode = $LASTEXITCODE
Pop-Location
if ($buildExitCode -ne 0) { Fail "backend build failed (see the Gradle output above)" }
$jar = Get-ChildItem backend\build\libs\*.jar | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1
if (-not $jar) { Fail "no jar found in backend\build\libs" }
Ok ("{0} ({1:N1} MB)" -f $jar.Name, ($jar.Length / 1MB))

Step 3 "Uploading to the server"
scp -i $Key $jar.FullName "${Server}:/tmp/bookman.jar"
if ($LASTEXITCODE -ne 0) { Fail "upload failed (server reachable? correct key?)" }
Ok "uploaded to /tmp/bookman.jar"

Step 4 "Installing and restarting the service"
ssh -i $Key $Server "sudo mv /tmp/bookman.jar /opt/bookman/bookman.jar && sudo chown bookman:bookman /opt/bookman/bookman.jar && sudo systemctl restart bookman"
if ($LASTEXITCODE -ne 0) { Fail "could not install the jar or restart the service" }
Ok "bookman.service restarted"

Step 5 "Waiting for the backend to answer ($HealthUrl)"
# The endpoint needs a login, so 401 means: backend up, HTTPS working. 502 = Caddy is up, backend still starting.
$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
$status = 0
do {
    Start-Sleep -Seconds 3
    $status = Get-StatusCode $HealthUrl
    Write-Host "." -NoNewline
} until ($status -eq 401 -or (Get-Date) -gt $deadline)
Write-Host ""
if ($status -ne 401) {
    Warn "last answer: $(if ($status -eq 0) { 'no connection' } else { "HTTP $status" }); last log lines:"
    ssh -i $Key $Server "sudo journalctl -u bookman -n 30 --no-pager"
    Fail "backend did not come up within $StartupTimeoutSeconds s (full log: journalctl -u bookman -n 200)"
}
Ok "backend answers (HTTP 401 UNAUTHORIZED as expected)"

Write-Host ""
Write-Host ("DEPLOYMENT SUCCEEDED: {0} in {1:N0} s" -f $version, ((Get-Date) - $started).TotalSeconds) -ForegroundColor Green
