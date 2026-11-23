# Directivas del Proyecto TV Pro

## 📦 Generación y Salida de APKs (TV & Mobile)
- **Directorio de salida obligatorio para instaladores:**
  `C:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-TVpro\instaladores`
- **Nombres de archivo requeridos:**
  - **Android TV:** `TVPro-TV.apk` (desde `:app:assembleRelease`)
  - **Android Mobile:** `TVPro-Mobile.apk` (desde `:app-mobile:assembleRelease`)
- **Automatización de Gradle:**
  Los scripts `build.gradle.kts` de `:app` y `:app-mobile` tienen configurada la tarea `copyApkToInstallers` vinculada a `assembleRelease`, copiando automáticamente los binarios generados a la carpeta `instaladores/`.
- **Regla para el Asistente:**
  Cada vez que el usuario solicite compilar, generar o crear los APKs, SIEMPRE se debe asegurar que los archivos finales queden depositados en `c:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-TVpro\instaladores`.
