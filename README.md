# MedTracker App

Una aplicación Android para el seguimiento de exámenes médicos con procesamiento local de PDFs.

## Descripción

MedTracker permite a los usuarios cargar, procesar y visualizar los resultados de sus exámenes médicos directamente en el dispositivo. La aplicación extrae datos de PDFs de laboratorios médicos, permitiendo seguir la evolución de los parámetros a lo largo del tiempo.

## Características Principales

- Procesamiento local de PDFs de exámenes médicos
- Extracción automática de parámetros usando OCR cuando es necesario
- Visualización de tendencias en gráficos
- Resumen de salud con indicadores visuales
- Almacenamiento seguro en el dispositivo

## Requisitos Técnicos

- Android SDK 34 o superior
- Java 17
- Kotlin 1.9.0
- Gradle 8.4.2 o superior

## Configuración del Proyecto

1. Asegúrate de tener configurado Java 17 correctamente en tu entorno
2. Clona el repositorio:
   ```
   git clone https://github.com/idacal/MedTracker.git
   ```
3. Abre el proyecto en Android Studio
4. Sincroniza el proyecto con los archivos Gradle

## Solución de Problemas

### Problema con versiones de dependencias
Si encuentras errores relacionados con versiones de dependencias incompatibles:
1. Verifica que la versión de androidx.core:core-ktx en `gradle/libs.versions.toml` sea 1.12.0
2. Asegúrate de que compileSdk es 34 en el archivo `app/build.gradle.kts`

### Problema con JAVA_HOME
Si ves errores relacionados con JAVA_HOME:
1. Asegúrate de que Java 17 está instalado en tu sistema
2. Configura la variable de entorno JAVA_HOME para que apunte a la instalación de Java 17
3. Reinicia Android Studio

## Estructura del Proyecto

El proyecto sigue una arquitectura limpia (Clean Architecture) con las siguientes capas:

- **domain**: Contiene los modelos, interfaces de repositorios y casos de uso
- **data**: Implementación de repositorios, fuentes de datos locales
- **pdfprocessor**: Módulos para el procesamiento de PDFs
- **ui**: Composables y ViewModels para las distintas pantallas

## Licencia

Este proyecto está bajo la licencia MIT. Ver el archivo LICENSE para más detalles. 