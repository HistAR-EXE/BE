$reg = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/register" -ContentType "application/json" `
  -Body (@{ email = "dbg.$([Guid]::NewGuid().ToString('N').Substring(0,8))@t.vn"; password = "Demo@2026"; displayName = "Dbg" } | ConvertTo-Json)
$h = @{ Authorization = "Bearer $($reg.data.token)" }
$c = (Invoke-RestMethod "http://localhost:8080/api/characters/by-location/11111111-1111-1111-1111-111111111111").data[0].id
try {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/chat" -Headers $h -ContentType "application/json" `
    -Body (@{ characterId = $c; message = "Bep Hoang Cam la gi?"; conversationId = $null } | ConvertTo-Json)
} catch {
  $code = $_.Exception.Response.StatusCode.value__
  Write-Host "Status:" $code
  if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
  else {
    try {
      $r = Invoke-WebRequest -Method Post -Uri "http://localhost:8080/api/chat" -Headers $h -ContentType "application/json" `
        -Body (@{ characterId = $c; message = "Bep Hoang Cam la gi?"; conversationId = $null } | ConvertTo-Json) -SkipHttpErrorCheck
      Write-Host $r.Content
    } catch {}
  }
}
