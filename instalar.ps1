<#
.SYNOPSIS
    Instalador interactivo de TV Pro para Android TV y Móvil Android.
#>
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "Instalador TV Pro - Android TV & Mobile"

function Buscar-Adb {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) { return $adbCmd.Source }

    $rutasPosibles = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "C:\Program Files\Android\Android Studio\platform-tools\adb.exe",
        "C:\Android\sdk\platform-tools\adb.exe"
    )
    foreach ($r in $rutasPosibles) {
        if (Test-Path $r) { return $r }
    }
    return $null
}

$adb = Buscar-Adb
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Rutas de APKs
$apkTV = Join-Path $scriptDir "TVPro-TV.apk"
if (-not (Test-Path $apkTV)) { $apkTV = Join-Path $scriptDir "instaladores\TVPro-TV.apk" }
if (-not (Test-Path $apkTV)) { $apkTV = Join-Path $scriptDir "TVPro.apk" }
if (-not (Test-Path $apkTV)) { $apkTV = Join-Path $scriptDir "..\TVPro-TV.apk" }

$apkMobile = Join-Path $scriptDir "TVPro-Mobile.apk"
if (-not (Test-Path $apkMobile)) { $apkMobile = Join-Path $scriptDir "instaladores\TVPro-Mobile.apk" }
if (-not (Test-Path $apkMobile)) { $apkMobile = Join-Path $scriptDir "..\TVPro-Mobile.apk" }

function Mostrar-Banner {
    Clear-Host
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "            TV PRO - INSTALADOR DE APK (TV & MOBILE)            " -ForegroundColor Yellow -BackgroundColor Black
    Write-Host "================================================================" -ForegroundColor Cyan
    if ($adb) {
        Write-Host "  ADB detectado : " -NoNewline -ForegroundColor Gray
        Write-Host $adb -ForegroundColor Green
    } else {
        Write-Host "  [!] ADB no encontrado en el sistema." -ForegroundColor Red
        Write-Host "      Instala Android SDK Platform-Tools o añade adb al PATH." -ForegroundColor Yellow
    }
    Write-Host "----------------------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "  APK TV     : " -NoNewline -ForegroundColor Gray
    if (Test-Path $apkTV) { Write-Host "$apkTV ($( [math]::Round((Get-Item $apkTV).Length / 1MB, 2) ) MB)" -ForegroundColor Green }
    else { Write-Host "No encontrado (Usa opcion 5 para compilar)" -ForegroundColor Red }

    Write-Host "  APK Mobile : " -NoNewline -ForegroundColor Gray
    if (Test-Path $apkMobile) { Write-Host "$apkMobile ($( [math]::Round((Get-Item $apkMobile).Length / 1MB, 2) ) MB)" -ForegroundColor Green }
    else { Write-Host "No encontrado (Usa opcion 5 para compilar)" -ForegroundColor Red }
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Obtener-Dispositivos {
    if (-not $adb) { return @() }
    $salida = & $adb devices -l
    $dispositivos = @()
    foreach ($linea in $salida) {
        $linea = $linea.Trim()
        if ([string]::IsNullOrWhiteSpace($linea) -or $linea.StartsWith("List of devices")) { continue }
        if ($linea -match '^([^\s]+)\s+device(.*)$') {
            $id = $Matches[1]
            $extra = $Matches[2].Trim()
            $dispositivos += [PSCustomObject]@{
                Id = $id
                Detalles = $extra
            }
        }
    }
    return $dispositivos
}

function Seleccionar-Dispositivo {
    $devs = Obtener-Dispositivos
    if ($devs.Count -eq 0) {
        Write-Host "No se encontraron dispositivos conectados por ADB." -ForegroundColor Yellow
        Write-Host "Conecta tu Android TV o móvil por cable USB o por Wi-Fi (Opción 4)." -ForegroundColor Gray
        Write-Host ""
        $conectar = Read-Host "¿Deseas conectar a un dispositivo por Wi-Fi ahora? (s/n)"
        if ($conectar -eq "s" -or $conectar -eq "S") {
            Conectar-Wifi
            $devs = Obtener-Dispositivos
        }
        if ($devs.Count -eq 0) { return $null }
    }

    if ($devs.Count -eq 1) {
        Write-Host "Dispositivo detectado: $($devs[0].Id) ($($devs[0].Detalles))" -ForegroundColor Cyan
        return $devs[0].Id
    }

    Write-Host "Múltiples dispositivos detectados:" -ForegroundColor Cyan
    for ($i = 0; $i -lt $devs.Count; $i++) {
        Write-Host "  [$($i+1)] $($devs[$i].Id) - $($devs[$i].Detalles)" -ForegroundColor Yellow
    }
    $sel = Read-Host "Selecciona el número de dispositivo [1-$($devs.Count)]"
    $idx = [int]$sel - 1
    if ($idx -ge 0 -and $idx -lt $devs.Count) {
        return $devs[$idx].Id
    }
    Write-Host "Selección inválida." -ForegroundColor Red
    return $null
}

function Instalar-Paquete ($deviceId, $apkPath, $packageName, $activityName, $nombreApp) {
    if (-not (Test-Path $apkPath)) {
        Write-Host "[ERROR] El archivo $apkPath no existe. Compílalo primero." -ForegroundColor Red
        return
    }

    Write-Host ""
    Write-Host ">>> Instalando $nombreApp en el dispositivo [$deviceId]..." -ForegroundColor Cyan
    Write-Host "    Archivo: $apkPath" -ForegroundColor Gray
    
    $installArgs = @()
    if ($deviceId) { $installArgs += "-s", $deviceId }
    $installArgs += "install", "-r", "-d", $apkPath

    $resultado = & $adb $installArgs 2>&1
    Write-Host ($resultado -join "`n")

    if ($resultado -match "Success") {
        Write-Host "[OK] $nombreApp se instaló exitosamente." -ForegroundColor Green
        $abrir = Read-Host "¿Deseas abrir la aplicación ahora en el dispositivo? (s/n)"
        if ($abrir -eq "s" -or $abrir -eq "S") {
            Write-Host "Iniciando $packageName..." -ForegroundColor Gray
            & $adb -s $deviceId shell am start -n "$packageName/$activityName"
        }
    } else {
        Write-Host "[ERROR] Falló la instalación de $nombreApp." -ForegroundColor Red
    }
}

function Conectar-Wifi {
    Write-Host ""
    Write-Host "--- Conexión a Android TV / Móvil por Wi-Fi ---" -ForegroundColor Cyan
    Write-Host "Asegúrate de que el dispositivo esté en la misma red Wi-Fi y tenga" -ForegroundColor Gray
    Write-Host "'Depuración por USB / Depuración inalámbrica' activada en Opciones de Desarrollador." -ForegroundColor Gray
    Write-Host ""
    $ip = Read-Host "Ingresa la IP del dispositivo (ej: 192.168.1.50 o 192.168.1.50:5555)"
    if ([string]::IsNullOrWhiteSpace($ip)) { return }
    if (-not ($ip -match ":")) { $ip = "$ip`:5555" }
    
    Write-Host "Conectando a $ip..." -ForegroundColor Yellow
    $res = & $adb connect $ip 2>&1
    Write-Host ($res -join "`n")
    Start-Sleep -Seconds 2
}

function Compilar-Apks {
    Write-Host ""
    Write-Host ">>> Compilando APKs de TV y Mobile con Gradle..." -ForegroundColor Cyan
    $projectRoot = $scriptDir
    if (Test-Path (Join-Path $projectRoot "android-app")) {
        $gradleDir = Join-Path $projectRoot "android-app"
    } elseif (Test-Path (Join-Path $projectRoot "..\android-app")) {
        $gradleDir = (Resolve-Path (Join-Path $projectRoot "..\android-app")).Path
    } else {
        Write-Host "No se encontró el directorio android-app." -ForegroundColor Red
        return
    }

    Push-Location $gradleDir
    try {
        & .\gradlew.bat assembleRelease
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Compilación exitosa." -ForegroundColor Green
            $tvRelease = Join-Path $gradleDir "app\build\outputs\apk\release\app-release.apk"
            $mobRelease = Join-Path $gradleDir "app-mobile\build\outputs\apk\release\app-mobile-release.apk"
            
            $distDir = Join-Path $projectRoot "instaladores"
            if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir -Force | Out-Null }
            
            if (Test-Path $tvRelease) {
                Copy-Item $tvRelease (Join-Path $projectRoot "TVPro-TV.apk") -Force
                Copy-Item $tvRelease (Join-Path $projectRoot "TVPro.apk") -Force
                Copy-Item $tvRelease (Join-Path $distDir "TVPro-TV.apk") -Force
                Write-Host "Copiado: TVPro-TV.apk" -ForegroundColor Green
            }
            if (Test-Path $mobRelease) {
                Copy-Item $mobRelease (Join-Path $projectRoot "TVPro-Mobile.apk") -Force
                Copy-Item $mobRelease (Join-Path $distDir "TVPro-Mobile.apk") -Force
                Write-Host "Copiado: TVPro-Mobile.apk" -ForegroundColor Green
            }
        } else {
            Write-Host "[ERROR] La compilación falló con código $LASTEXITCODE." -ForegroundColor Red
        }
    } finally {
        Pop-Location
    }
    Write-Host ""
    Read-Host "Presiona Enter para continuar..."
}

# Bucle principal
do {
    Mostrar-Banner
    Write-Host "  [1] Instalar TV Pro en Android TV / Google TV" -ForegroundColor White
    Write-Host "  [2] Instalar TV Pro en Celular / Móvil Android" -ForegroundColor White
    Write-Host "  [3] Instalar AMBAS versiones (TV y Móvil)" -ForegroundColor White
    Write-Host "  [4] Conectar a dispositivo por Wi-Fi (ADB Connect IP)" -ForegroundColor Yellow
    Write-Host "  [5] Recompilar APKs (Release Gradle Build)" -ForegroundColor Cyan
    Write-Host "  [6] Abrir carpeta donde están los APKs" -ForegroundColor Gray
    Write-Host "  [0] Salir" -ForegroundColor DarkGray
    Write-Host ""
    $opc = Read-Host "Selecciona una opción [0-6]"

    switch ($opc) {
        "1" {
            $dev = Seleccionar-Dispositivo
            if ($dev) {
                Instalar-Paquete $dev $apkTV "com.tvpro.app" "com.tvpro.app.MainActivity" "TV Pro (Android TV)"
            }
            Read-Host "`nPresiona Enter para continuar..."
        }
        "2" {
            $dev = Seleccionar-Dispositivo
            if ($dev) {
                Instalar-Paquete $dev $apkMobile "com.tvpro.mobile" "com.tvpro.mobile.MainActivity" "TV Pro (Mobile)"
            }
            Read-Host "`nPresiona Enter para continuar..."
        }
        "3" {
            $dev = Seleccionar-Dispositivo
            if ($dev) {
                Instalar-Paquete $dev $apkTV "com.tvpro.app" "com.tvpro.app.MainActivity" "TV Pro (Android TV)"
                Instalar-Paquete $dev $apkMobile "com.tvpro.mobile" "com.tvpro.mobile.MainActivity" "TV Pro (Mobile)"
            }
            Read-Host "`nPresiona Enter para continuar..."
        }
        "4" {
            Conectar-Wifi
            Read-Host "`nPresiona Enter para continuar..."
        }
        "5" {
            Compilar-Apks
        }
        "6" {
            $folder = if (Test-Path (Join-Path $scriptDir "instaladores")) { Join-Path $scriptDir "instaladores" } else { $scriptDir }
            explorer.exe $folder
        }
        "0" {
            Write-Host "Hasta luego." -ForegroundColor Gray
            break
        }
        Default {
            Write-Host "Opción inválida." -ForegroundColor Red
            Start-Sleep -Seconds 1
        }
    }
} while ($opc -ne "0")
