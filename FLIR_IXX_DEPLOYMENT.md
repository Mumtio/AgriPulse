# AgriPulse Deployment Guide - FLIR iXX Series

## Compatible Devices

### FLIR iXX Series (ACE Platform)
✅ **FLIR i34** - Entry-level thermal camera
✅ **FLIR i35** - With LTE connectivity  
✅ **FLIR i64** - High-resolution thermal imaging

All iXX series cameras share:
- 5-inch touchscreen display
- Android-based ACE platform
- Physical hardware buttons
- Rugged, glove-friendly design
- IP54 rating (dust/water resistant)

## Pre-Deployment Checklist

### 1. Verify Device Specifications
- [ ] Device is FLIR i34, i35, or i64
- [ ] Running Android 13+ (API 33+)
- [ ] Has FLIR Atlas SDK installed
- [ ] Camera is powered on and functional

### 2. Development Environment
- [ ] Android Studio installed
- [ ] FLIR SDK libraries in `app/libs/`:
  - `androidsdk-release.aar`
  - `thermalsdk-release.aar`
- [ ] USB debugging enabled on iXX camera
- [ ] ADB drivers installed on development machine

### 3. App Configuration
- [ ] Set `CommunicationInterface.ACE` in MainActivity.java
- [ ] Verify minSdk = 33, targetSdk = 36
- [ ] Check ABI is set to arm64-v8a

## Step-by-Step Deployment

### Step 1: Configure for iXX Series

In `MainActivity.java` (line 88), ensure:
```java
private static final CommunicationInterface aceRealCameraInterface = CommunicationInterface.ACE;
```

**NOT** `CommunicationInterface.EMULATOR`

### Step 2: Build the APK

#### Option A: Release Build (Recommended)
```bash
# Windows
.\gradlew assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

#### Option B: Debug Build (For Testing)
```bash
# Windows
.\gradlew assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Connect iXX Camera

#### Via USB Cable
1. Connect iXX camera to computer via USB
2. On camera: Enable USB debugging
   - Settings → Developer Options → USB Debugging
3. Verify connection:
```bash
adb devices
```

You should see your device listed.

#### Via Network (If LTE/WiFi available on i35)
```bash
# Find camera IP address (on camera: Settings → About → IP Address)
adb connect <camera-ip-address>:5555
```

### Step 4: Install the App

```bash
# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Or install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Force reinstall (if already installed)
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Step 5: Grant Permissions

On first launch, the app will request:
1. **Camera Permission** - Required for thermal imaging
2. **Internet Permission** - Required for ACE SDK communication

Grant both permissions when prompted.

### Step 6: Verify Installation

1. Launch AgriPulse from app drawer
2. Check status message shows "Connecting to..."
3. Verify thermal camera feed appears
4. Test hardware button navigation

## iXX Series Specific Features

### Hardware Button Mapping

The iXX series has physical buttons optimized for field use:

```
┌─────────────────────────────────┐
│        FLIR iXX BUTTONS         │
├─────────────────────────────────┤
│                                 │
│  [Power]  - Top right           │
│                                 │
│  [D-Pad]  - Right side          │
│    ↑ Up                         │
│  ← Left  Right →                │
│    ↓ Down                       │
│                                 │
│  [Select] - Center of D-pad     │
│  [Back]   - Below D-pad         │
│                                 │
└─────────────────────────────────┘
```

### Navigation Flow in AgriPulse

**Landing Screen:**
- Focus: Start button
- Select: Enter app

**Menu Screen:**
- Up/Down: Navigate cards (Scan → Analytics → History → Exit)
- Select: Open selected screen
- Back: Exit app (with confirmation)

**Scan Screen:**
- Up/Down: Navigate buttons (Back → Scan → Share)
- Select: Activate button
- Back: Return to menu

**History Screen:**
- Up/Down: Navigate list items
- Select: View animal details
- Back: Return to menu

### Display Optimization

The app automatically detects iXX series (5-inch) and applies:
- **Touch targets**: 56-80dp (glove-friendly)
- **Text sizes**: 13-38sp (readable at arm's length)
- **Scan overlay**: 280dp (perfect for 5" screen)
- **Button height**: 64dp (easy to press)
- **Spacing**: 8-48dp (ACE design system)

## Testing on iXX Series

### Functional Testing
- [ ] App launches successfully
- [ ] Thermal camera connects and streams
- [ ] Temperature readings display correctly
- [ ] Scan function works
- [ ] History saves and loads
- [ ] Analytics display correctly
- [ ] Share function works

### Hardware Button Testing
- [ ] D-pad navigation works smoothly
- [ ] Focus indicator (green outline) is visible
- [ ] Select button activates focused element
- [ ] Back button returns to previous screen
- [ ] Navigation is logical and intuitive

### Field Testing (With Gloves)
- [ ] All buttons are easily tappable
- [ ] Text is readable in bright sunlight
- [ ] Touch targets are large enough
- [ ] Camera can be operated one-handed
- [ ] App responds quickly to inputs

### Environmental Testing
- [ ] Works in direct sunlight
- [ ] Works in low light/darkness
- [ ] Works in dusty conditions
- [ ] Works in light rain (IP54)
- [ ] Battery life is acceptable

## Troubleshooting

### Camera Not Connecting

**Symptom**: "Connection error" or "Camera not found"

**Solutions**:
1. Verify `CommunicationInterface.ACE` is set (not EMULATOR)
2. Check camera permission is granted
3. Restart the app
4. Restart the iXX camera
5. Check FLIR SDK libraries are in `app/libs/`

```bash
# Check if libraries exist
ls app/libs/
# Should show:
# androidsdk-release.aar
# thermalsdk-release.aar
```

### UI Elements Too Small

**Symptom**: Buttons/text are hard to tap/read

**Solutions**:
1. Verify device is using correct configuration:
```bash
adb shell dumpsys window displays | grep -i "cur="
```

2. Check configuration is loaded:
   - Should use `values-w480dp-h800dp/` for iXX series

3. Manually increase sizes in `values-w480dp-h800dp/dimens.xml`

### Focus States Not Working

**Symptom**: No green outline when using D-pad

**Solutions**:
1. Check all interactive elements have:
```xml
android:focusable="true"
android:foreground="@drawable/focus_highlight"
```

2. Verify `focus_highlight.xml` exists in `drawable/`

3. Test focus order:
```bash
# Enable focus debugging
adb shell settings put global debug_view_attributes 1
```

### Thermal Stream Not Displaying

**Symptom**: Black screen or frozen image

**Solutions**:
1. Check GLSurfaceView is visible:
   - In scan screen, GLSurfaceView should be visible
   - In other screens, it should be hidden

2. Verify OpenGL is enabled:
   - Check `ThermalSdkAndroid.init()` is called

3. Check camera permissions:
```bash
adb shell dumpsys package com.flir.atlassdk.acecamerasample | grep permission
```

### App Crashes on Launch

**Symptom**: App closes immediately after opening

**Solutions**:
1. Check logcat for errors:
```bash
adb logcat | grep -i "agripulse\|flir\|thermal"
```

2. Verify SDK libraries are compatible:
   - Check SDK version matches target Android version

3. Check for missing permissions in manifest

4. Verify minSdk (33) is supported by device

## Performance Optimization

### Battery Life
- Thermal imaging is power-intensive
- Expected runtime: 4-6 hours continuous use
- Tips:
  - Lower screen brightness when possible
  - Close app when not in use
  - Disable LTE when not needed (i35)

### Storage Management
- App uses SharedPreferences for scan data
- Typical storage: <10MB for 100 scans
- To clear data:
```bash
adb shell pm clear com.flir.atlassdk.acecamerasample
```

### Network Usage (i35 with LTE)
- Minimal data usage (only for sharing reports)
- Typical: <1MB per shared report
- Works fully offline except for sharing

## Maintenance

### Updating the App

1. Build new APK with updated version code
2. Install over existing app:
```bash
adb install -r app-release.apk
```
3. Data is preserved (SharedPreferences)

### Backing Up Data

```bash
# Backup app data
adb backup -f agripulse-backup.ab com.flir.atlassdk.acecamerasample

# Restore app data
adb restore agripulse-backup.ab
```

### Uninstalling

```bash
adb uninstall com.flir.atlassdk.acecamerasample
```

## Production Deployment

### For Multiple iXX Cameras

1. **Build signed release APK**:
   - Generate keystore
   - Sign APK with release key
   - Enable ProGuard for code obfuscation

2. **Mass deployment options**:
   - USB installation (one by one)
   - Network deployment via MDM
   - FLIR device management (if available)

3. **Configuration management**:
   - Use build variants for different farms
   - Configure animal IDs via config file
   - Set up remote logging (optional)

### Security Considerations

- [ ] Sign APK with production keystore
- [ ] Enable ProGuard/R8 obfuscation
- [ ] Remove debug logging
- [ ] Secure SharedPreferences (if needed)
- [ ] Implement data encryption (if required)

## Support Resources

### FLIR Support
- **Developer Portal**: https://developer.flir.com/
- **iXX Series Manual**: Check device documentation
- **SDK Documentation**: In SDK package
- **Technical Support**: Contact FLIR support team

### App Support
- **Documentation**: See `RESPONSIVE_DESIGN.md`
- **Quick Reference**: See `QUICK_REFERENCE.md`
- **Setup Guide**: See `ACE_CAMERA_SETUP.md`

## Quick Command Reference

```bash
# Check connected devices
adb devices

# Install app
adb install -r app-release.apk

# View logs
adb logcat | grep -i agripulse

# Check app info
adb shell dumpsys package com.flir.atlassdk.acecamerasample

# Clear app data
adb shell pm clear com.flir.atlassdk.acecamerasample

# Uninstall app
adb uninstall com.flir.atlassdk.acecamerasample

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen
adb shell screenrecord /sdcard/demo.mp4
```

---

## Summary

Your AgriPulse app is fully optimized for FLIR iXX series cameras:
- ✅ 5-inch touchscreen support
- ✅ Hardware button navigation
- ✅ Glove-friendly touch targets
- ✅ ACE design system compliance
- ✅ Thermal imaging integration
- ✅ Field-ready durability

Ready to deploy on i34, i35, and i64! 🎉
