# ============================================================
# Agri Chain - End-to-End API Test Script
# ============================================================
$BASE = "http://localhost"
$PASS = 0; $FAIL = 0; $ERRORS = @()

function Invoke-Api {
    param($Name, $Method, $Url, $Body, $Token, $ExpectedStatus, $ExtractField, $ExtraHeaders = @{})
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    foreach ($k in $ExtraHeaders.Keys) { $headers[$k] = $ExtraHeaders[$k] }
    try {
        $params = @{ Method=$Method; Uri=$Url; Headers=$headers; ErrorAction="Stop"; UseBasicParsing=$true }
        if ($Body) { $params["Body"] = ($Body | ConvertTo-Json -Depth 10) }
        $resp = Invoke-WebRequest @params
        $status = $resp.StatusCode; $content = $resp.Content
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $content = $_.ErrorDetails.Message
        if (-not $status) { $status = 0; $content = $_.Exception.Message }
    }
    $ok = $status -eq $ExpectedStatus
    $sym = if ($ok) { "PASS" } else { "FAIL" }
    $col = if ($ok) { "Green" } else { "Red" }
    Write-Host "[$sym] $Name  (got $status, expected $ExpectedStatus)" -ForegroundColor $col
    if (-not $ok) { $script:ERRORS += "$Name => got $status expected $ExpectedStatus" }
    if ($ok) { $script:PASS++ } else { $script:FAIL++ }
    if ($ExtractField -and $ok -and $content) {
        try { return ($content | ConvertFrom-Json).$ExtractField } catch { return $content.Trim('"') }
    }
    # If no ExtractField but response is a plain UUID string, return it
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
        $s = $_.Exception.Response.StatusCode.value__
        if ($s -eq 409) { Write-Host "[PASS] POST /api/auth/register ($Label already exists)  (409 ok)" -ForegroundColor Green; $script:PASS++ }
        else { Write-Host "[FAIL] POST /api/auth/register ($Label)  (got $s)" -ForegroundColor Red; $script:FAIL++; $script:ERRORS += "register $Label => $s" }
    }
}

# ── IDENTITY SERVICE ──────────────────────────────────────────
Write-Host "`n=== IDENTITY SERVICE ===" -ForegroundColor Cyan

Register-Or-Skip @{username="admin_e2e";password="Admin@1234";email="admin_e2e@test.com";role="Administrator"} "admin"
Register-Or-Skip @{username="co_e2e";password="Co@12345";email="co_e2e@test.com";role="Compliance_Officer"} "compliance_officer"
Register-Or-Skip @{username="auditor_e2e";password="Aud@12345";email="auditor_e2e@test.com";role="Government_Auditor"} "gov_auditor"

$adminToken  = Invoke-Api "POST /api/auth/login (admin)"   POST "$BASE/api/auth/login" @{username="admin_e2e";password="Admin@1234"} $null 200 "token"
$coToken     = Invoke-Api "POST /api/auth/login (co)"      POST "$BASE/api/auth/login" @{username="co_e2e";password="Co@12345"}      $null 200 "token"
$auditorToken= Invoke-Api "POST /api/auth/login (auditor)" POST "$BASE/api/auth/login" @{username="auditor_e2e";password="Aud@12345"} $null 200 "token"

Invoke-Api "POST /api/auth/refresh"                        POST "$BASE/api/auth/refresh" $null $adminToken 200 $null | Out-Null
Invoke-Api "POST /api/auth/login (wrong creds → 401)"      POST "$BASE/api/auth/login" @{username="admin_e2e";password="wrong"} $null 401 $null | Out-Null
Invoke-Api "GET /api/audit-log (Compliance_Officer)"       GET  "$BASE/api/audit-log" $null $coToken 200 $null | Out-Null
Invoke-Api "GET /api/audit-log (Government_Auditor)"       GET  "$BASE/api/audit-log" $null $auditorToken 200 $null | Out-Null
Invoke-Api "GET /api/audit-log (admin → 403)"              GET  "$BASE/api/audit-log" $null $adminToken 403 $null | Out-Null
Invoke-Api "PUT /api/roles/{userId} (admin → 404 for unknown user)" PUT "$BASE/api/roles/00000000-0000-0000-0000-000000000001" @{role="Farmer"} $adminToken 404 $null | Out-Null

# ── FARMER SERVICE ────────────────────────────────────────────
Write-Host "`n=== FARMER SERVICE ===" -ForegroundColor Cyan

$farmerReg = @{username="farmer_e2e3";password="Farmer@1234";email="farmer_e2e3@test.com";name="E2E Farmer";dateOfBirth="1985-06-15";gender="Male";address="456 Farm Lane";contactInfo="+98765432$(Get-Random -Minimum 10 -Maximum 99)";landDetails="10 acres rice"}
$farmerId = Invoke-Api "POST /api/farmers (register)" POST "$BASE/api/farmers" $farmerReg $null 201 $null
$farmerToken = Invoke-Api "POST /api/auth/login (farmer)" POST "$BASE/api/auth/login" @{username="farmer_e2e3";password="Farmer@1234"} $null 200 "token"

if ($farmerId) {
    Invoke-Api "GET /api/farmers/$farmerId"              GET "$BASE/api/farmers/$farmerId"       $null $adminToken 200 $null | Out-Null
    Invoke-Api "GET /api/farmers/$farmerId/status"       GET "$BASE/api/farmers/$farmerId/status" $null $adminToken 200 $null | Out-Null
    Invoke-Api "GET /api/farmers/count"                  GET "$BASE/api/farmers/count"            $null $adminToken 200 $null | Out-Null
    Invoke-Api "PUT /api/farmers/$farmerId/status (approve)" PUT "$BASE/api/farmers/$farmerId/status" @{status="Active"} $adminToken 204 $null | Out-Null
    Invoke-Api "PUT /api/farmers/$farmerId (update profile)" PUT "$BASE/api/farmers/$farmerId" @{name="Updated Farmer";gender="Male";address="New Address";landDetails="12 acres"} $farmerToken 200 $null | Out-Null
}

# ── CROP SERVICE ──────────────────────────────────────────────
Write-Host "`n=== CROP SERVICE ===" -ForegroundColor Cyan

Invoke-Api "GET /api/listings (browse)"        GET "$BASE/api/listings"              $null $farmerToken 200 $null | Out-Null
Invoke-Api "GET /api/listings/total-volume"    GET "$BASE/api/listings/total-volume" $null $adminToken  200 $null | Out-Null

$listingBody = @{farmerId=$farmerId;cropType="Rice";quantity=500;pricePerUnit=2.50;location="Punjab"}
$listingId = Invoke-Api "POST /api/listings (create)" POST "$BASE/api/listings" $listingBody $farmerToken 201 $null

if ($listingId) {
    Invoke-Api "GET /api/listings/$listingId"                    GET "$BASE/api/listings/$listingId"          $null $farmerToken 200 $null | Out-Null
    Invoke-Api "PUT /api/listings/$listingId/status (approve)"   PUT "$BASE/api/listings/$listingId/status" @{status="Active"} $adminToken 204 $null | Out-Null
}

# ── TRADER SERVICE ────────────────────────────────────────────
Write-Host "`n=== TRADER SERVICE ===" -ForegroundColor Cyan

$traderReg = @{username="trader_e2e3";password="Trader@1234";email="trader_e2e3@test.com";name="E2E Trader";organization="TradeOrg";contactInfo="+11223344$(Get-Random -Minimum 10 -Maximum 99)"}
$traderId = Invoke-Api "POST /api/traders (register)" POST "$BASE/api/traders" $traderReg $null 201 $null
$traderToken = Invoke-Api "POST /api/auth/login (trader)" POST "$BASE/api/auth/login" @{username="trader_e2e3";password="Trader@1234"} $null 200 "token"

if ($traderId) {
    Invoke-Api "GET /api/traders/$traderId" GET "$BASE/api/traders/$traderId" $null $traderToken 200 $null | Out-Null
}

# Place order if listing is active
if ($listingId -and $traderToken -and $traderId) {
    $orderBody = @{listingId=$listingId;traderId=$traderId;quantity=10}
    $orderId = Invoke-Api "POST /api/orders (place order)" POST "$BASE/api/orders" $orderBody $traderToken 201 $null
    if ($orderId) {
        Invoke-Api "GET /api/orders/$orderId" GET "$BASE/api/orders/$orderId" $null $traderToken 200 $null | Out-Null
        Invoke-Api "PUT /api/orders/$orderId/status (accept)" PUT "$BASE/api/orders/$orderId/status" @{status="Confirmed"} $farmerToken 200 $null | Out-Null
    }
}

# ── TRANSACTION SERVICE ───────────────────────────────────────
Write-Host "`n=== TRANSACTION SERVICE ===" -ForegroundColor Cyan
Invoke-Api "GET /api/transactions/total-value" GET "$BASE/api/transactions/total-value" $null $adminToken 200 $null | Out-Null

# ── SUBSIDY SERVICE ───────────────────────────────────────────
Write-Host "`n=== SUBSIDY SERVICE ===" -ForegroundColor Cyan

$programBody = @{title="Rice Subsidy 2026";description="Support for rice farmers";startDate="2026-01-01";endDate="2026-12-31";budgetAmount=1000000;createdBy="00000000-0000-0000-0000-000000000001"}
$programId = Invoke-Api "POST /api/subsidies/programs (create)" POST "$BASE/api/subsidies/programs" $programBody $adminToken 201 $null

if ($programId) {
    Invoke-Api "PUT /api/subsidies/programs/$programId/status?status=Active (activate)" PUT "$BASE/api/subsidies/programs/$programId/status?status=Active" $null $adminToken 204 $null | Out-Null

    if ($farmerId) {
        $disbBody = @{programId=$programId;farmerId=$farmerId;amount=5000;programCycle="2026-Q1"}
        $disbId = Invoke-Api "POST /api/subsidies/disbursements (create)" POST "$BASE/api/subsidies/disbursements" $disbBody $adminToken 201 $null
        if ($disbId) {
            Invoke-Api "PUT /api/subsidies/disbursements/$disbId/approve?reviewerId=$farmerId" PUT "$BASE/api/subsidies/disbursements/$disbId/approve?reviewerId=$farmerId" $null $adminToken 204 $null | Out-Null
        }
    }
}
Invoke-Api "GET /api/subsidies/total-disbursed" GET "$BASE/api/subsidies/total-disbursed" $null $adminToken 200 $null | Out-Null

# ── COMPLIANCE SERVICE ────────────────────────────────────────
Write-Host "`n=== COMPLIANCE SERVICE ===" -ForegroundColor Cyan

# Need a valid createdBy UUID — use a placeholder
$coUserId = "00000000-0000-0000-0000-000000000002"
$compBody = @{entityType="Farmer";entityId=$farmerId;checkResult="Pass";checkDate="2026-03-28";notes="All docs verified";createdBy=$coUserId}
Invoke-Api "POST /api/compliance/records" POST "$BASE/api/compliance/records" $compBody $coToken 201 $null | Out-Null

if ($farmerId) {
    Invoke-Api "GET /api/compliance/records?entityType=Farmer&entityId=$farmerId" GET "$BASE/api/compliance/records?entityType=Farmer&entityId=$farmerId" $null $coToken 200 $null | Out-Null
}

$auditBody = @{scope="Q1 2026 Farmer Audit";initiatedBy=$coUserId}
$auditId = Invoke-Api "POST /api/compliance/audits (create)" POST "$BASE/api/compliance/audits" $auditBody $coToken 201 $null
if ($auditId) {
    Invoke-Api "PUT /api/compliance/audits/$auditId/findings" PUT "$BASE/api/compliance/audits/$auditId/findings" @{findings="All farmers compliant"} $coToken 204 $null | Out-Null
    Invoke-Api "GET /api/compliance/audits/$auditId/export" GET "$BASE/api/compliance/audits/$auditId/export" $null $auditorToken 200 $null | Out-Null
}

# ── REPORTING SERVICE ─────────────────────────────────────────
Write-Host "`n=== REPORTING SERVICE ===" -ForegroundColor Cyan

Invoke-Api "GET /api/reports/dashboard" GET "$BASE/api/reports/dashboard" $null $adminToken 200 $null | Out-Null

# POST /reports requires X-User-ID header and format field
$reportBody = @{scope="TRANSACTIONS";startDate="2026-01-01";endDate="2026-03-28";format="CSV"}
$reportId = Invoke-Api "POST /api/reports (generate)" POST "$BASE/api/reports" $reportBody $adminToken 202 $null -ExtraHeaders @{"X-User-ID"="00000000-0000-0000-0000-000000000001"}
if ($reportId) {
    Invoke-Api "GET /api/reports/$reportId/export" GET "$BASE/api/reports/$reportId/export" $null $adminToken 200 $null | Out-Null
}

# ── NOTIFICATION SERVICE ──────────────────────────────────────
Write-Host "`n=== NOTIFICATION SERVICE ===" -ForegroundColor Cyan

# GET /notifications/me requires X-User-ID header
if ($farmerId) {
    Invoke-Api "GET /api/notifications/me (farmer)" GET "$BASE/api/notifications/me" $null $farmerToken 200 $null -ExtraHeaders @{"X-User-ID"=$farmerId} | Out-Null
}
if ($traderId) {
    Invoke-Api "GET /api/notifications/me (trader)" GET "$BASE/api/notifications/me" $null $traderToken 200 $null -ExtraHeaders @{"X-User-ID"=$traderId} | Out-Null
}

# ── AUTH LOGOUT ───────────────────────────────────────────────
Write-Host "`n=== AUTH LOGOUT ===" -ForegroundColor Cyan

# Get a fresh token to test logout
$freshToken = Invoke-Api "POST /api/auth/login (fresh for logout test)" POST "$BASE/api/auth/login" @{username="farmer_e2e3";password="Farmer@1234"} $null 200 "token"
Invoke-Api "POST /api/auth/logout" POST "$BASE/api/auth/logout" $null $freshToken 204 $null | Out-Null
Invoke-Api "POST /api/auth/refresh (after logout → 401)" POST "$BASE/api/auth/refresh" $null $freshToken 401 $null | Out-Null

# ── SUMMARY ───────────────────────────────────────────────────
Write-Host "`n========================================" -ForegroundColor Cyan
$total = $PASS + $FAIL
Write-Host "RESULTS: $PASS/$total passed, $FAIL failed" -ForegroundColor $(if ($FAIL -eq 0) { "Green" } else { "Yellow" })
if ($ERRORS.Count -gt 0) {
    Write-Host "`nFailed:" -ForegroundColor Red
    $ERRORS | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
}
