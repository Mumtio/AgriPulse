# Build and Test Guide
**AgriPulse - Quick Reference for APK Building and Testing**

---

## 🚀 Quick Start

### Prerequisites Checklist
- [ ] Android Studio installed (Arctic Fox or later)
- [ ] Android device with USB debugging enabled
- [ ] USB cable for device connection
- [ ] Project opened in Android Studio

---

## 📱 Option 1: Test on Physical Device (Recommended)

### Step 1: Enable Developer Mode on Phone
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. You'll see "You are now a developer!"

### Step 2: Enable USB Debugging
1. Go to **Settings** → **Developer Options**
2. Enable **USB Debugging**
3. Enable **Install via USB** (if available)

### Step 3: Connect and Run
1. Connect phone to computer via USB
2. On phone, allow USB debugging (tap "Always allow")
3. In Android Studio, click green **Run** button (▶)
4. Select your device from the list
5. Wait for build and installation
6. App launches automatically

**Time Required:** 5-10 minutes

---

## 💻 Option 2: Test on Android Emulator

### Step 1: Create Emulator (if needed)
1. In Android Studio, click **Device Manager** (phone icon)
2. Click **Create Device**
3. Select **Pixel 4** or similar
4. Select **API 30** (Android 11)
5. Click **Finish**

### Step 2: Run on Emulator
1. Start emulator from Device Manager
2. Wait for emulator to boot (2-3 minutes)
3. Click green **Run** button (▶)
4. Select emulator from list
5. Wait for build and installation
6. App launches automatically

**Time Required:** 10-15 minutes (first time)

---

## 📦 Option 3: Build Release APK

### Method A: Using Android Studio GUI

#### Step 1: Generate Signed Bundle/APK
1. Click **Build** → **Generate Signed Bundle / APK**
2. Select **APK**
3. Click **Next**

#### Step 2: Create Keystore (First Time Only)
1. Click **Create new...**
2. Fill in details:
   - **Key store path:** Choose location (e.g., `agripulse-keystore.jks`)
   - **Password:** Create strong password
   - **Alias:** agripulse-key
   - **Validity:** 25 years
   - **First and Last Name:** Your name
   - **Organization:** Bob The Builders
3. Click **OK**

#### Step 3: Sign APK
1. Enter keystore password
2. Enter key password
3. Click **Next**
4. Select **release** build variant
5. Check **V1** and **V2** signature versions
6. Click **Finish**

#### Step 4: Locate APK
APK will be at:
```
AgriPulse-master/app/build/outputs/apk/release/app-release.apk
```

**Time Required:** 5-10 minutes

---

### Method B: Using Command Line (Faster)

#### For Debug APK (Testing)
```bash
cd AgriPulse-master
./gradlew assembleDebug
```

APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### For Release APK (Submission)
```bash
cd AgriPulse-master
./gradlew assembleRelease
```

APK location:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

**Note:** Release APK needs signing for submission

**Time Required:** 3-5 minutes

---

## 🧪 Testing Checklist

### Quick Test (5 minutes)
- [ ] App installs successfully
- [ ] App launches without crash
- [ ] Landing screen displays
- [ ] Can navigate to Scan screen
- [ ] Can perform one scan
- [ ] Scan completes successfully

### Full Test (30 minutes)
Follow **TESTING_GUIDE.md** for comprehensive testing:
- [ ] All 10 test cases
- [ ] All features verified
- [ ] All screens tested
- [ ] Export functionality tested
- [ ] Navigation flow tested

---

## 🐛 Troubleshooting

### Problem: Gradle Sync Failed
**Solution:**
```
File → Invalidate Caches → Invalidate and Restart
```

### Problem: Device Not Detected
**Solution:**
1. Reconnect USB cable
2. Check USB debugging is enabled
3. Try: Tools → Troubleshoot Device Connections

### Problem: Build Failed
**Solution:**
```bash
cd AgriPulse-master
./gradlew clean
./gradlew build
```

### Problem: App Crashes on Launch
**Solution:**
1. Check Logcat for errors
2. Verify Android version (API 24+)
3. Clear app data and reinstall

---

## 📊 Build Verification

### After Building APK

#### Check File Size
```bash
ls -lh app/build/outputs/apk/release/app-release.apk
```
**Expected:** 10-30 MB

#### Install on Device
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

#### Launch App
```bash
adb shell am start -n com.flir.atlassdk.acecamerasample/.MainActivity
```

#### Check Logs
```bash
adb logcat | grep AgriPulse
```

---

## 🎯 Pre-Submission Checklist

### Before Submitting APK

#### Technical Verification
- [ ] APK builds successfully
- [ ] APK installs on device
- [ ] App launches without crash
- [ ] No critical errors in Logcat
- [ ] All features functional

#### Testing Verification
- [ ] Completed all 10 test cases
- [ ] All tests passed
- [ ] No crashes during testing
- [ ] Performance acceptable

#### File Verification
- [ ] APK file exists
- [ ] File size reasonable (<50MB)
- [ ] APK is signed (for release)
- [ ] File name is clear (e.g., agripulse-v1.0-beta.apk)

---

## 📤 Submission Process

### Step 1: Prepare APK
1. Build release APK (signed)
2. Rename to: `agripulse-v1.0-beta.apk`
3. Test on physical device
4. Verify all features work

### Step 2: ACE App Review
1. Go to FLIR ACE Developer Hub
2. Navigate to App Review section
3. Fill out submission form:
   - **App Name:** AgriPulse
   - **Version:** 1.0.0 (Beta)
   - **Category:** Wildcard
   - **Description:** Thermal livestock wellness monitor
4. Upload APK
5. Submit for review

### Step 3: Box Upload
1. Wait for ACE App Review approval
2. Receive Box upload link via email
3. Upload approved APK to Box
4. Verify upload successful

### Step 4: Email Confirmation
Send to: info@kreativdistrikt.com

**Subject:** Deliverable 2 Submission - AgriPulse - Bob The Builders

**Body:**
```
Dear FLIR App Challenge Team,

We are pleased to submit our Deliverable 2 (Mock-up Beta Version).

Team Name: Bob The Builders
Idea Name: AgriPulse - Livestock Wellness Monitor
Category: Wildcard

Submission Details:
- APK submitted via ACE App Review
- APK uploaded to Box
- Documentation included

Best regards,
[Your Name]
Bob The Builders Team
```

---

## ⏰ Time Estimates

### Quick Path (Testing Only)
- Setup device: 5 minutes
- Run from Android Studio: 5 minutes
- Quick test: 5 minutes
**Total: 15 minutes**

### Standard Path (Build + Test)
- Setup device: 5 minutes
- Build APK: 5 minutes
- Install and test: 10 minutes
- Full testing: 30 minutes
**Total: 50 minutes**

### Complete Path (Build + Full Test + Submit)
- Setup: 5 minutes
- Build release APK: 10 minutes
- Full testing: 30 minutes
- ACE App Review: 10 minutes
- Box upload: 5 minutes
- Email confirmation: 5 minutes
**Total: 65 minutes**

---

## 🎯 Recommended Workflow

### For Today (Submission Day)

**Morning (Now - 12:00 PM):**
1. ✅ Test on physical device (15 minutes)
2. ✅ Run quick test (5 minutes)
3. ✅ Fix any critical issues

**Afternoon (12:00 PM - 3:00 PM):**
1. ✅ Build release APK (10 minutes)
2. ✅ Full testing (30 minutes)
3. ✅ Document test results

**Evening (3:00 PM - 5:30 PM):**
1. ✅ Submit via ACE App Review (10 minutes)
2. ✅ Upload to Box (5 minutes)
3. ✅ Send confirmation email (5 minutes)

**Buffer (5:30 PM - 5:59 PM):**
- Emergency fixes if needed

---

## 📞 Quick Help

### Common Commands

**Clean build:**
```bash
./gradlew clean
```

**Build debug APK:**
```bash
./gradlew assembleDebug
```

**Build release APK:**
```bash
./gradlew assembleRelease
```

**Install APK:**
```bash
adb install path/to/app.apk
```

**Uninstall app:**
```bash
adb uninstall com.flir.atlassdk.acecamerasample
```

**View logs:**
```bash
adb logcat
```

---

## ✅ Success Indicators

### Build Success
- ✅ "BUILD SUCCESSFUL" message
- ✅ APK file created
- ✅ No error messages

### Installation Success
- ✅ "Success" message from adb
- ✅ App icon appears on device
- ✅ App launches when tapped

### Testing Success
- ✅ All features work
- ✅ No crashes
- ✅ Performance smooth
- ✅ UI displays correctly

---

## 🎉 You're Ready!

**Everything you need to build, test, and submit AgriPulse!**

**Next Steps:**
1. Choose your testing method (physical device recommended)
2. Follow the steps above
3. Complete testing checklist
4. Build release APK
5. Submit!

**Good luck! 🚀**

---

**Version:** 1.0
**Date:** February 9, 2026
**Status:** Ready to Use
