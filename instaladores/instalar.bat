@echo off
chcp 65001 >nul
title Instalador TV Pro (TV y Mobile)
cls
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1"
pause
