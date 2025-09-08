# AppTrackSolutionsGobea

AppTrackSolutionsGobea es una aplicación Android desarrollada en Kotlin utilizando Android Studio y Gradle. Permite a los usuarios gestionar notas de manera local con Room y sincronizarlas en la nube mediante Firebase Firestore. La autenticación se realiza con Google Sign-In y Firebase Auth, asegurando que cada usuario acceda únicamente a sus propios datos.

## Características principales
- Gestión de notas locales con Room.
- Sincronización automática de notas con Firebase Firestore.
- Autenticación de usuarios con Google y Firebase Auth.
- Interfaz moderna basada en Jetpack Compose.
- Inspección de base de datos local con Database Inspector de Android Studio.

## Estructura del proyecto
- `app/` - Código fuente principal, recursos, esquemas de Room y archivos de configuración.
- `schemas/` - Esquemas exportados de la base de datos Room para facilitar migraciones.
- `build.gradle.kts`, `settings.gradle.kts` - Configuración de Gradle.
- `.gitignore` - Exclusión de archivos generados y sensibles.

## Requisitos
- Android Studio (recomendado última versión).
- macOS (compatible con otros sistemas operativos).
- Cuenta de Firebase configurada con Firestore y Auth.

## Instalación y ejecución
1. Clona el repositorio.
2. Configura `google-services.json` con tu proyecto Firebase.
3. Sincroniza y compila el proyecto en Android Studio.
4. Ejecuta la app en un emulador o dispositivo físico.

## Seguridad
Las reglas de Firestore deben permitir acceso solo a usuarios autenticados:
```plaintext
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /notes/{userId}/user_notes/{noteId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## Licencia
Este proyecto se distribuye bajo la licencia MIT. Ver archivo LICENSE para más detalles.

