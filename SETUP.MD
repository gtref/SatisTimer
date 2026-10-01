# 📘 **SatisBrickTimer — Technical Architecture & API Guide**  
*A complete overview of the system, components, APIs, and behaviors.*

---

## 🧩 **1. Overview**

This document describes the architecture of a parental‑control Android application that:

- Lets parents **select apps** to control  
- Allows setting **timers** for each app or all apps  
- “Bricks” (blocks) apps when the timer expires  
- Keeps apps bricked for **daily**, **2‑day**, **weekly**, or custom cycles  
- Requires a **secret parental password** to override timers or access blocked apps  
- Prevents uninstalling or disabling the app without the parental password  
- Uses **Material 3 UI** for modern Android design  
- Works entirely in **Java** (no Kotlin required)

The app behaves similarly to a built‑in system feature using Android’s **Device Admin**, **AccessibilityService**, and **UsageStats** APIs.

---

## 🧱 **2. Core Features**

### ✔️ App selection  
Parents choose which apps to control using a Material 3 UI list.

### ✔️ Timers  
Each app can have a countdown timer (e.g., 1 hour, 3 hours, daily limit).

### ✔️ Bricking  
When the timer expires, the app becomes inaccessible until the next cycle.

### ✔️ Cycles  
Apps can remain bricked for:

- 1 day  
- 2 days  
- 1 week  
- Custom durations  

### ✔️ Parental password  
A secret password unlocks:

- Bricked apps  
- Timer changes  
- App removal  
- Device Admin removal  
- App uninstall

### ✔️ Uninstall protection  
The app cannot be removed without the parental password.

### ✔️ Material 3 UI  
All screens use modern Material 3 components:

- Buttons  
- Pickers  
- Dialogs  
- Snackbars  
- Cards  
- Switches  
- Checkboxes  

---

## 🧩 **3. System Components**

Your app consists of these major modules:

```
AppSelectorActivity
TimerActivity
ParentalControlActivity
BlockedScreenActivity
MainService (WorkManager / Foreground)
BlockService (AccessibilityService)
DeviceAdminReceiver
Database (Room or SharedPreferences)
Material 3 UI Layer
```

---

## 🔧 **4. Required Android APIs**

### **4.1 PackageManager**
Used to list installed apps:

```java
PackageManager pm = getPackageManager();
List<ApplicationInfo> apps = pm.getInstalledApplications(0);
```

### **4.2 UsageStatsManager**
Detects app launches and usage:

```java
UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
```

### **4.3 AccessibilityService**
Intercepts app launches and blocks apps:

```java
@Override
public void onAccessibilityEvent(AccessibilityEvent event) {
    if (isBricked(event.getPackageName())) {
        showBlockedScreen();
    }
}
```

### **4.4 Device Admin API**
Prevents uninstalling or disabling the app:

```java
DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
```

### **4.5 WorkManager**
Runs background tasks for timers and cycles:

```java
WorkManager.getInstance(context).enqueue(periodicWork);
```

### **4.6 AlarmManager (optional)**
Schedules daily resets.

### **4.7 SharedPreferences / Room**
Stores:

- Timers  
- Cycles  
- Password hash  
- App list  
- Bricked state  

### **4.8 Material 3 Components**
UI elements:

- `MaterialButton`  
- `MaterialDatePicker`  
- `MaterialTimePicker`  
- `MaterialAlertDialogBuilder`  
- `MaterialCheckBox`  
- `MaterialSwitch`  
- `MaterialCardView`  

---

## 🔒 **5. Uninstall Protection**

### ✔️ Device Admin prevents uninstall  
Once activated, the app cannot be:

- Uninstalled  
- Disabled  
- Force‑stopped  
- Cleared  
- Removed from Device Admin  

### ✔️ AccessibilityService blocks uninstall attempts  
Detects when the child opens Settings → Apps → Uninstall.

### ✔️ Parental password required  
To disable Device Admin:

```java
dpm.removeActiveAdmin(adminComponent);
```

Only allowed after password verification.

---

## 🧱 **6. App Bricking System**

### **6.1 Timer storage**

```java
long endTime = System.currentTimeMillis() + durationMs;
saveTimer(packageName, endTime);
```

### **6.2 Timer checking**

WorkManager checks:

```java
if (System.currentTimeMillis() > endTime) {
    brickApp(packageName);
}
```

### **6.3 Bricking behavior**

When bricked:

- AccessibilityService blocks app launch  
- BlockedScreenActivity is shown  
- Icon can be greyed out  
- Notification badge can be applied  
- App hidden from launcher (optional)

### **6.4 Cycle reset**

Daily / 2‑day / weekly cycles:

```java
long nextUnlock = today + cycleLengthMs;
```

WorkManager resets bricked apps at cycle boundaries.

---

## 🔐 **7. Parental Password System**

### ✔️ Password stored as a hash  
Never store plain text.

### ✔️ Password required for:

- Changing timers  
- Changing cycles  
- Removing apps from control  
- Accessing bricked apps  
- Disabling Device Admin  
- Uninstalling the app  

### ✔️ Material 3 password dialog

```java
new MaterialAlertDialogBuilder(this)
    .setTitle("Parent Access")
    .setView(R.layout.dialog_password)
    .setPositiveButton("Unlock", (d, w) -> verifyPassword())
    .show();
```

---

## 🎨 **8. Material 3 UI Integration**

### Screens using Material 3:

- App selection  
- Timer picker  
- Cycle picker  
- Password dialog  
- Blocked screen  
- Settings  
- Parent dashboard  

### Components used:

- `MaterialButton`  
- `MaterialDatePicker`  
- `MaterialTimePicker`  
- `MaterialAlertDialogBuilder`  
- `MaterialCheckBox`  
- `MaterialSwitch`  
- `MaterialCardView`  
- `Snackbar`  

---

## 📁 **9. Project Structure**

```
app/
 ├─ java/com/example/parentalcontrol/
 │   ├─ AppSelectorActivity.java
 │   ├─ TimerActivity.java
 │   ├─ ParentalControlActivity.java
 │   ├─ BlockedScreenActivity.java
 │   ├─ BlockService.java
 │   ├─ MainService.java
 │   ├─ DeviceAdminReceiver.java
 │   ├─ data/
 │   │   ├─ AppDatabase.java
 │   │   ├─ TimerDao.java
 │   │   ├─ AppDao.java
 │   │   └─ PasswordStore.java
 ├─ res/
 │   ├─ layout/
 │   ├─ values/
 │   ├─ xml/
 │   │   └─ device_admin_receiver.xml
 └─ AndroidManifest.xml
```

---

## 🧪 **10. Permissions Required**

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
```

---

## 🎯 **11. Summary**

Your app will behave like a built‑in parental control system:

- Cannot be uninstalled  
- Cannot be force‑stopped  
- Blocks apps when timers expire  
- Enforces daily/weekly cycles  
- Requires parental password for overrides  
- Uses Material 3 for modern UI  
- Works fully in Java  
- Uses official Android APIs (no root required)
