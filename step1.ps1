$r = Invoke-WebRequest -Uri 'http://localhost:8081/oauth2/token' -Method POST -Headers @{Authorization='Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ='} -Body 'grant_type=client_credentials&scope=payments:write' -ContentType 'application/x-www-form-urlencoded' -TimeoutSec 10 -UseBasicParsing
Write-Host "STATUS: $($r.StatusCode)"
Write-Host "BODY: $($r.Content)"
