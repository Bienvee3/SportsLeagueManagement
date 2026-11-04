# run.ps1 — Lanza Sports League Management cargando .env automaticamente
# Uso: .\run.ps1
# Requiere: Docker Desktop corriendo con 'docker compose up -d' previo

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "No se encontro el archivo .env en $envFile"
    exit 1
}

# Cargar variables del .env (ignora comentarios y lineas vacias)
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line.Split("=", 2)
        if ($parts.Length -eq 2) {
            $key   = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  SET $key" -ForegroundColor DarkGray
        }
    }
}

Write-Host ""
Write-Host "Variables cargadas desde .env" -ForegroundColor Green
Write-Host "Lanzando Sports League Management..." -ForegroundColor Cyan
Write-Host "Accede en: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Pulsa Ctrl+C para parar`n" -ForegroundColor Gray

& "$PSScriptRoot\mvnw.cmd" spring-boot:run
