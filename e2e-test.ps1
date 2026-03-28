# ============================================================
# KrishiSetu - Integrated E2E Validation Suite (v4 FINAL)
# ============================================================
$BASE = "http://localhost"
$PASS = 0; $FAIL = 0; $ERRORS = @()

function Invoke-Api {
    param($Name, $Method, $Url, $Body, $Token, $ExpectedStatus, $ExtractField, $ExtraHeaders = @{})
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    foreach ($k in $ExtraHeaders.Keys) { $headers[$k] = $ExtraHeaders[$k] }
    
    $content = ""
    $status = 0
    try {
        $params = @{ Method=$Method; Uri=$Url; Headers=$headers; ErrorAction="Stop"; UseBasicParsing=$true }
        if ($Body) { $params["Body"] = ($Body | ConvertTo-Json -Depth 10) }
        $resp = Invoke-WebRequest @params
        $status = $resp.StatusCode
        $content = $resp.Content
    } catch {
        if ($_.Exception.Response) {
            $status = $_.Exception.Response.StatusCode.value__
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            $reader.Close(); $stream.Close()
        } else {
            $status = 0
            $content = $_.Exception.Message
        }
    }
    
    $ok = $status -eq $ExpectedStatus
    $sym = if ($ok) { "PASS" } else { "FAIL" }
    $col = if ($ok) { "Green" } else { "Red" }
    
    Write-Host "[$sym] $Name (got $status, expected $ExpectedStatus)" -ForegroundColor $col
    if (-not $ok) { 
        Write-Host "      DEBUG: Response Content => $content" -ForegroundColor Gray
        $script:ERRORS += "$Name => got $status expected $ExpectedStatus (Error: $content)" 
        $script:FAIL++ 
    } else {
        $script:PASS++
    }

    if ($ExtractField -and $ok -and $content) {
        try { return ($content | ConvertFrom-Json).$ExtractField } catch { return $content.Trim('"') }
    }
    if ($ok -and $content -and $content -match '^"?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"?$') {
        return $content.Trim('"')
    }
    return $null
}

function Register-Or-Skip {
    param($Body, $Label)
    try {
        $r = Invoke-WebRequest -Method POST -Uri "$BASE/api/auth/register" -Headers @{"Content-Type"="application/json"} -Body ($Body | ConvertTo-Json) -UseBasicParsing -ErrorAction Stop
        Write-Host "[PASS] POST /api/auth/register ($Label)  (got $($r.StatusCode))" -ForegroundColor Green
        $script:PASS++
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409) { 
            Write-Host "[PASS] POST /api/auth/register ($Label already exists)  (409 ok)" -ForegroundColor Green; $script:PASS++ 
        }
        else { Write-Host "[FAIL] POST /api/auth/register ($Label)  (got 400 or error)" -ForegroundColor Red; $script:FAIL++; $script:ERRORS += "register $Label failed" }
    }
}

# ── 1. IDENTITY & AUTH ─────────────────────────────────────────
Write-Host "`n=== 1. IDENTITY SERVICE ===" -ForegroundColor Cyan
Register-Or-Skip @{username="v4_admin";password="Admin@1234";email="v4_admin@test.com";role="Administrator"} "admin"
Register-Or-Skip @{username="v4_co";password="Co@12345";email="v4_co@test.com";role="Compliance_Officer"} "co"

$adminToken = Invoke-Api "POST /api/auth/login (admin)" POST "$BASE/api/auth/login" @{username="v4_admin";password="Admin@1234"} $null 200 "token"
$coToken    = Invoke-Api "POST /api/auth/login (co)"    POST "$BASE/api/auth/login" @{username="v4_co";password="Co@12345"}      $null 200 "token"
$fixedCoId  = "00000000-0000-0000-0000-000000000002"

# ── 2. FARMER ONBOARDING ───────────────────────────────────────
Write-Host "`n=== 2. FARMER SERVICE ===" -ForegroundColor Cyan
$farmerReg = @{username="v4_farmer";password="Farmer@1234";email="v4_farmer@test.com";name="V4 Farmer";dateOfBirth="1990-01-01";gender="Male";address="V4 Address";contactInfo="1234567890";landDetails="V4 Land"}
$farmerId = Invoke-Api "POST /api/farmers (register)" POST "$BASE/api/farmers" $farmerReg $null 201 $null
$fToken = Invoke-Api "POST /api/auth/login (farmer)" POST "$BASE/api/auth/login" @{username="v4_farmer";password="Farmer@1234"} $null 200 "token"

if ($farmerId) {
    Invoke-Api "PUT /api/farmers/$farmerId/status" PUT "$BASE/api/farmers/$farmerId/status" @{status="Active"} $adminToken 204 $null | Out-Null
}

# ── 3. SUBSIDY PROGRAMS ────────────────────────────────────────
Write-Host "`n=== 3. SUBSIDY SERVICE ===" -ForegroundColor Cyan
# Removed [decimal] to prevent JSON objectification
$programBody = @{title="V4 Program";description="E2E Validation";startDate="2026-01-01";endDate="2026-12-31";budgetAmount=1000000;createdBy=$fixedCoId}
$programId = Invoke-Api "POST /api/subsidies/programs" POST "$BASE/api/subsidies/programs" $programBody $adminToken 201 $null

if ($programId) {
    Invoke-Api "PUT /api/subsidies/programs/$programId/status" PUT "$BASE/api/subsidies/programs/$programId/status?status=Active" $null $adminToken 204 $null | Out-Null
}

# ── 4. COMPLIANCE RECORDS ──────────────────────────────────────
Write-Host "`n=== 4. COMPLIANCE SERVICE ===" -ForegroundColor Cyan
if ($farmerId) {
    $compBody = @{entityType="Farmer";entityId=$farmerId;checkResult="Pass";checkDate="2026-03-28";notes="E2E Validated";createdBy=$fixedCoId}
    Invoke-Api "POST /api/compliance/records" POST "$BASE/api/compliance/records" $compBody $coToken 201 $null | Out-Null
}

# ── 5. REPORTING DASHBOARD ─────────────────────────────────────
Write-Host "`n=== 5. REPORTING SERVICE ===" -ForegroundColor Cyan
Invoke-Api "GET /api/reports/dashboard" GET "$BASE/api/reports/dashboard" $null $adminToken 200 $null -ExtraHeaders @{"X-User-ID"=$fixedCoId} | Out-Null

# ── 6. NOTIFICATION SYSTEM ─────────────────────────────────────
Write-Host "`n=== 6. NOTIFICATION SERVICE ===" -ForegroundColor Cyan
if ($farmerId) {
    Invoke-Api "GET /api/notifications/me" GET "$BASE/api/notifications/me" $null $fToken 200 $null -ExtraHeaders @{"X-User-ID"=$farmerId} | Out-Null
}

# ── FINAL REPORT ───────────────────────────────────────────────
Write-Host "`n========================================" -ForegroundColor Cyan
$total = $PASS + $FAIL
Write-Host "V4 RESULTS: $PASS/$total passed, $FAIL failed" -ForegroundColor $(if ($FAIL -eq 0) { "Green" } else { "Yellow" })
if ($ERRORS.Count -gt 0) {
    Write-Host "`nIssues Captured:" -ForegroundColor Red
    $ERRORS | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
