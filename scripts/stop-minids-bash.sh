#!/bin/bash
# Opreste clusterul MiniDS (toate 3 nodurile)

powershell.exe -Command "
  \$pids = (Get-NetTCPConnection -LocalPort 8301,8311,8321 -State Listen -ErrorAction SilentlyContinue).OwningProcess
  if (\$pids) {
    Stop-Process -Id \$pids -Force -ErrorAction SilentlyContinue
    Write-Host 'MiniDS cluster oprit.'
  } else {
    Write-Host 'Niciun nod MiniDS nu ruleaza.'
  }
"
