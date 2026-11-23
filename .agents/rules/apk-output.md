---
description: Directiva de generación y distribución de instaladores APK de TV Pro
trigger: always_on
---

# Regla de Distribución de APKs

Siempre que se compilen, generen o creen los APKs de TV Pro (para TV o Celular / Mobile):
1. Los APKs deben quedar guardados en:
   `C:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-TVpro\instaladores`
2. Los nombres oficiales de los instaladores son:
   - `TVPro-TV.apk` (para la versión de TV)
   - `TVPro-Mobile.apk` (para la versión de Celular / Mobile)
3. Esto es compatible tanto con el instalador interactivo (`instaladores/instalar.bat` / `instaladores/instalar.ps1`) como con la tarea automática de Gradle `copyApkToInstallers`.
