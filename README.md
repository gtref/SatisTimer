# SatisTimer Android starter

This is a Java-based Android application scaffold based on `SETUP.MD`. It includes the Gradle build, Android manifest and service declarations, Material 3, WorkManager, Room, and placeholder classes for the documented app components. Feature behavior and screens have not been implemented.

## Build requirements

- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.9.2 (downloaded by Gradle on the first build)

The Gradle wrapper downloads Gradle 8.11.1 automatically. On Windows, build with:

```powershell
./gradlew.bat assembleDebug
```

`QUERY_ALL_PACKAGES` is included for the documented installed-app selector. Distribution through Google Play may require declaring an eligible core use in Play Console.