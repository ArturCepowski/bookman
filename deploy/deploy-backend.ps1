# Builds the backend and installs it on the server (Windows PowerShell, run from the repository root):
#   .\deploy\deploy-backend.ps1 -Key C:\Users\<you>\.ssh\bookman-oracle.key
# Flyway migrations run automatically when the service restarts. See docs/deployment.md.
param(
    [Parameter(Mandatory = $true)] [string] $Key,
    [string] $Server = "ubuntu@bookman-artur.duckdns.org"
)
$ErrorActionPreference = "Stop"

Push-Location backend
try {
    .\gradlew bootJar
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
} finally {
    Pop-Location
}
$jar = Get-ChildItem backend\build\libs\*.jar | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1

scp -i $Key $jar.FullName "${Server}:/tmp/bookman.jar"
if ($LASTEXITCODE -ne 0) { throw "Upload failed" }
ssh -i $Key $Server "sudo mv /tmp/bookman.jar /opt/bookman/bookman.jar && sudo chown bookman:bookman /opt/bookman/bookman.jar && sudo systemctl restart bookman && sleep 20 && sudo systemctl --no-pager status bookman | head -n 5"
if ($LASTEXITCODE -ne 0) { throw "Restart failed" }
Write-Host "Deployed. Check https://bookman-artur.duckdns.org/api/users/me (expects UNAUTHORIZED)."
