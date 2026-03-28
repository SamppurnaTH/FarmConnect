$ErrorActionPreference = "Stop"

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

Write-Host "Build successful. Launching microservices..." -ForegroundColor Green

foreach ($svc in $services) {
    Write-Host "Starting $svc in a new window..." -ForegroundColor Yellow
    # Open a new PowerShell window running mvn spring-boot:run
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $svc; mvn spring-boot:run"
}

Write-Host "All 9 services have been launched in separate terminal windows." -ForegroundColor Cyan
Write-Host "Remember to start PostgreSQL (e.g., via Docker) before the services can successfully connect to the DB." -ForegroundColor Yellow
