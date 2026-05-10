$ErrorActionPreference = "Stop"

# ── Phase 1: Discovery + Gateway ─────────────────────────────────────────────
$infra = @(
    "eureka-service",
    "gateway-service"
)

# ── Phase 2: Microservices ───────────────────────────────────────────────────
$services = @(
    "identity-service",
    "farmer-service",
    "crop-service",
    "transaction-service",
    "subsidy-service",
    "compliance-service",
    "reporting-service",
    "notification-service",
    "trader-service"
)

Write-Host "Compiling all modules (including common) without tests..." -ForegroundColor Cyan
mvn clean install -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Please check the Maven output." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Build successful. Launching infrastructure first..." -ForegroundColor Green

foreach ($svc in $infra) {
    Write-Host "Starting $svc in a new window..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $svc; mvn spring-boot:run"
}

Write-Host "Waiting 15s for Eureka and Gateway to start..." -ForegroundColor Cyan
Start-Sleep -Seconds 15

Write-Host "Launching microservices..." -ForegroundColor Green

foreach ($svc in $services) {
    Write-Host "Starting $svc in a new window..." -ForegroundColor Yellow
    # Open a new PowerShell window running mvn spring-boot:run
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $svc; mvn spring-boot:run"
}

Write-Host ""
Write-Host "=== FarmConnect Startup Complete ===" -ForegroundColor Cyan
Write-Host "Eureka Dashboard: http://localhost:8761" -ForegroundColor Yellow
Write-Host "Gateway (API):    http://localhost:8080" -ForegroundColor Yellow
Write-Host "Frontend:         http://localhost:80" -ForegroundColor Yellow
Write-Host ""
Write-Host "Note: All backend traffic now routes through the Gateway." -ForegroundColor White
Write-Host "Remember to start PostgreSQL (e.g., via Docker) before the services can successfully connect to the DB." -ForegroundColor Yellow
