$body = @{
    email = "barbara@test.cl"
    password = "test123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:3001/auth/login' -Method Post -ContentType 'application/json' -Body $body -ErrorAction Stop
    Write-Output "EXITO:"
    Write-Output ($response | ConvertTo-Json -Depth 5)
} catch {
    Write-Output "ERROR:"
    Write-Output $_.Exception.Message
    if ($_.ErrorDetails) {
        Write-Output $_.ErrorDetails.Message
    }
}
