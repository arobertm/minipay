# Step 1: Get auth token
Write-Host "=== STEP 1: Get Auth Token ==="
$headers1 = @{Authorization = 'Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ='}
try {
    $tokenResponse = Invoke-RestMethod -Uri 'http://localhost:8081/oauth2/token' `
        -Method POST `
        -Headers $headers1 `
        -Body 'grant_type=client_credentials&scope=payments:write' `
        -ContentType 'application/x-www-form-urlencoded'
    $tokenJson = $tokenResponse | ConvertTo-Json -Depth 10
    Write-Host $tokenJson
    $accessToken = $tokenResponse.access_token
    Write-Host "TOKEN_EXTRACTED: $($accessToken.Substring(0, [Math]::Min(30, $accessToken.Length)))..."
} catch {
    Write-Host "ERROR_STEP1: $($_.Exception.Message)"
    $accessToken = $null
}

# Step 2: Run a payment
Write-Host ""
Write-Host "=== STEP 2: Run Payment ==="
if ($accessToken) {
    $headers2 = @{
        Authorization = "Bearer $accessToken"
        'Content-Type' = 'application/json'
    }
    $paymentBody = '{"merchantId":"merch-001","orderId":"ORD-DIAG-1","amount":15000,"currency":"RON","pan":"4111111111111111","expiryDate":"12/28","cvv":"123"}'
    try {
        $paymentResponse = Invoke-RestMethod -Uri 'http://localhost:8084/v1/payments/authorize' `
            -Method POST `
            -Headers $headers2 `
            -Body $paymentBody `
            -ContentType 'application/json'
        $paymentResponse | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "ERROR_STEP2: $($_.Exception.Message)"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "RESPONSE_BODY: $($reader.ReadToEnd())"
        }
    }
} else {
    Write-Host "SKIPPED: no token"
}

# Step 3: Fraud-svc health
Write-Host ""
Write-Host "=== STEP 3: Fraud-svc Health ==="
try {
    $fraudHealth = Invoke-RestMethod -Uri 'http://localhost:8090/health' -Method GET
    $fraudHealth | ConvertTo-Json -Depth 10
} catch {
    Write-Host "ERROR_STEP3: $($_.Exception.Message)"
}

# Step 4: Test fraud directly
Write-Host ""
Write-Host "=== STEP 4: Fraud Score ==="
$fraudBody = '{"dpan":"4111111111111111","amount":15000,"currency":"RON","merchantId":"merch-001","ipAddress":""}'
try {
    $fraudScore = Invoke-RestMethod -Uri 'http://localhost:8090/fraud/score' `
        -Method POST `
        -Body $fraudBody `
        -ContentType 'application/json'
    $fraudScore | ConvertTo-Json -Depth 10
} catch {
    Write-Host "ERROR_STEP4: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "RESPONSE_BODY: $($reader.ReadToEnd())"
    }
}

# Step 5: Audit verify
Write-Host ""
Write-Host "=== STEP 5: Audit Verify ==="
try {
    $audit = Invoke-RestMethod -Uri 'http://localhost:8091/audit/verify' -Method GET
    $audit | ConvertTo-Json -Depth 10
} catch {
    Write-Host "ERROR_STEP5: $($_.Exception.Message)"
}
