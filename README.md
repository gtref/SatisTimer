# SatisTimer 0.0.3

Java-based Android test build. It can select launchable apps, save a timer and lock cycle, and use the optional accessibility service to show a lock screen when a timer expires. The dashboard shows the app version in its lower-right corner.

## Build and Install

Requirements: JDK 17 and Android SDK Platform 35. The Gradle wrapper downloads Gradle 8.11.1 automatically.

```powershell
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Open SatisTimer, choose **Add apps**, select an app, and save. Set its timer to **1 minute** and its lock cycle to **1 minute (test)**. Enable **app blocking**, allow notifications, and turn on SatisTimer in Android's Accessibility settings. The timer starts only while the selected app is in the foreground with the screen on. A small overlay shows the time remaining; a notification is sent once when one minute or less remains. After the timer expires, the app shows the lock screen until the test cycle ends. Real lock cycles of 1 day, 2 days, and 1 week are also available.

## Scope

Timers count down only while a selected app is in the foreground and the display is on; this version does not measure touches or other interaction inside the app. Lock periods begin when an active timer expires. This is a prototype: parental passwords, recurring resets, UsageStats, and background timer checks are not implemented. Parent settings are not password protected. Device Admin is not registered, so the app can be uninstalled normally. Accessibility blocking and notification permission must be enabled explicitly on the device.

Package visibility is limited to apps with launcher activities. Distribution through Google Play may require declaring an eligible core use for app monitoring.