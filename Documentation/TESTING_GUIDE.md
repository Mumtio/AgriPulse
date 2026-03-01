# AgriPulse - Testing Guide
**Complete Testing Instructions for Android Studio & Physical Device**

---

## 📱 Testing Environment

### Requirements
- **Android Studio:** Arctic Fox (2020.3.1) or later
- **Android Device:** API 24+ (Android 7.0+) OR Android Emulator
- **USB Cable:** For physical device connection
- **Computer:** Windows/Mac/Linux with Android Studio installed

---

## 🚀 Setup Instructions

### Step 1: Open Project in Android Studio

1. **Launch Android Studio**
   ```
   Start Android Studio from your applications
   ```

2. **Open Project**
   - Click "Open" or "Open an Existing Project"
   - Navigate to: `C:\Users\Shawki\Desktop\flir\AgriPulse-master`
   - Click "OK"

3. **Wait for Gradle Sync**
   - Android Studio will automatically sync Gradle
   - Wait for "Gradle sync finished" message
   - This may take 2-5 minutes on first open

4. **Verify SDK**
   - Check bottom right for any SDK download prompts
   - Install any missing SDK components if prompted

---

### Step 2: Connect Physical Android Device

1. **Enable Developer Options on Phone**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - You'll see "You are now a developer!"

2. **Enable USB Debugging**
   - Go to Settings → Developer Options
   - Enable "USB Debugging"
   - Enable "Install via USB" (if available)

3. **Connect Phone to Computer**
   - Use USB cable
   - On phone, allow USB debugging when prompted
   - Select "Always allow from this computer"

4. **Verify Connection in Android Studio**
   - Look at top toolbar
   - Device dropdown should show your phone model
   - If not visible, click refresh button

---

### Step 3: Build and Run

1. **Select Run Configuration**
   - Top toolbar: Select "app" from dropdown
   - Next dropdown: Select your device

2. **Click Run Button**
   - Green play button (▶) in toolbar
   - OR press Shift+F10 (Windows/Linux) or Control+R (Mac)

3. **Wait for Build**
   - Gradle will build the APK
   - Progress shown in bottom panel
   - First build takes 2-5 minutes

4. **App Installs and Launches**
   - APK installs on your device
   - App launches automatically
   - You should see the Landing screen

---

## 🧪 Complete Feature Testing

### Test 1: Landing Screen
**Duration:** 1 minute

**Steps:**
1. App opens to Landing screen
2. Verify UI elements:
   - AgriPulse logo/title
   - "Start Scanning" button
   - "View History" button
   - "Analytics" button
   - "Settings" button

**Expected Results:**
- ✅ All buttons visible
- ✅ Clean, modern design
- ✅ No crashes or errors

**Pass/Fail:** ___________

---

### Test 2: Scan Screen - Basic Navigation
**Duration:** 2 minutes

**Steps:**
1. Click "Start Scanning" from Landing
2. Scan screen opens
3. Verify UI elements:
   - Back button (top left)
   - "Align animal in frame" text
   - Temperature card (shows "--.- °C")
   - Scan overlay box (center)
   - "SCAN" button (bottom)
   - Status text area

**Expected Results:**
- ✅ Scan screen loads
- ✅ Thermal view visible (black background)
- ✅ All UI elements present
- ✅ Back button works

**Pass/Fail:** ___________

---

### Test 3: Thermal Scanning - Normal Result
**Duration:** 3 minutes

**Steps:**
1. On Scan screen
2. Click "SCAN" button
3. Observe:
   - Button changes to "SCANNING..."
   - Button becomes disabled
   - Status shows "Analyzing thermal data..."
4. Wait 1-2 seconds
5. Observe results:
   - Temperature displays (e.g., "38.2 °C")
   - Status shows "✓ Normal temperature" in GREEN
   - Keypoints appear on screen (white dots)
   - ROI boxes appear (green rounded rectangles)
   - Labels show body part names and temperatures
   - Button re-enables with "SCAN" text

**Expected Results:**
- ✅ Scan completes successfully
- ✅ Temperature displayed
- ✅ Status is green
- ✅ Keypoints visible (6-8 points)
- ✅ ROI boxes visible with labels
- ✅ No share button (normal status)

**Pass/Fail:** ___________

---

### Test 4: Thermal Scanning - Fever Detection
**Duration:** 3 minutes

**Steps:**
1. Click "SCAN" button multiple times
2. Eventually you'll get a fever detection (random)
3. Observe results:
   - Temperature displays high (e.g., "39.8 °C")
   - Status shows "🚨 Udder temperature elevated" in RED
   - Explanation shows "Why?\n• Udder temperature elevated"
   - ROI box on udder is RED
   - "Share with Vet" button appears

**Expected Results:**
- ✅ High temperature detected
- ✅ Status is red
- ✅ Specific reason shown
- ✅ Red ROI box on affected area
- ✅ Share button visible

**Pass/Fail:** ___________

---

### Test 5: Export and Share
**Duration:** 3 minutes

**Steps:**
1. After fever detection, click "Share with Vet"
2. Dialog appears with 3 options:
   - "Share via Apps"
   - "Export CSV"
   - "Export Text"
3. Click "Share via Apps"
4. Share menu opens
5. Select an app (e.g., Gmail, WhatsApp)
6. Verify:
   - Message includes alert text
   - File attachment present
7. Go back and try "Export CSV"
8. Toast message shows filename
9. Try "Export Text"
10. Toast message shows filename

**Expected Results:**
- ✅ Dialog appears with 3 options
- ✅ Share menu opens
- ✅ Message and attachment present
- ✅ CSV export shows success toast
- ✅ Text export shows success toast

**Pass/Fail:** ___________

---

### Test 6: History Screen
**Duration:** 3 minutes

**Steps:**
1. Go back to Landing screen
2. Click "View History"
3. Observe:
   - List of scanned animals
   - Each shows: Animal ID, Species, Temperature, Status, Time
4. Click on an animal
5. Animal History screen opens
6. Observe:
   - Animal ID and species at top
   - List of all scans for this animal
   - Each scan shows temperature, status, time
7. Click on a scan
8. Scan details appear

**Expected Results:**
- ✅ History list displays
- ✅ All scanned animals shown
- ✅ Correct data displayed
- ✅ Animal history works
- ✅ Scan details accessible

**Pass/Fail:** ___________

---

### Test 7: Analytics Dashboard
**Duration:** 2 minutes

**Steps:**
1. Go back to Landing screen
2. Click "Analytics"
3. Observe:
   - Pie chart showing Normal/Monitor/High Risk
   - Herd statistics (percentages)
   - Temperature trend
   - Hot zones list
4. Verify data matches scanned animals

**Expected Results:**
- ✅ Pie chart displays
- ✅ Statistics accurate
- ✅ Trend calculated
- ✅ Hot zones listed (if any fever detected)

**Pass/Fail:** ___________

---

### Test 8: Multiple Scans
**Duration:** 5 minutes

**Steps:**
1. Go to Scan screen
2. Perform 5 scans in a row
3. Observe:
   - Each scan generates new animal ID (COW001, COW002, etc.)
   - Each scan has unique data
   - Overlay updates each time
4. Go to History
5. Verify all 5 animals appear
6. Go to Analytics
7. Verify statistics updated

**Expected Results:**
- ✅ All 5 scans successful
- ✅ Unique IDs generated
- ✅ All appear in history
- ✅ Analytics updated

**Pass/Fail:** ___________

---

### Test 9: Navigation Flow
**Duration:** 3 minutes

**Steps:**
1. Landing → Scan → Back → Landing ✓
2. Landing → History → Animal → Back → History → Back → Landing ✓
3. Landing → Analytics → Back → Landing ✓
4. Landing → Settings → Back → Landing ✓
5. Scan → History (via back) → Scan ✓

**Expected Results:**
- ✅ All navigation works
- ✅ Back button functions correctly
- ✅ No crashes
- ✅ Smooth transitions

**Pass/Fail:** ___________

---

### Test 10: Visual Quality
**Duration:** 2 minutes

**Steps:**
1. Review all screens for:
   - Rounded corners on buttons and cards
   - Shadows and elevation
   - Color consistency
   - Text readability
   - Icon clarity
   - Animation smoothness
2. Check overlay:
   - Keypoints visible
   - ROI boxes rounded
   - Labels readable
   - Colors appropriate (green/red)

**Expected Results:**
- ✅ Professional appearance
- ✅ Modern Material Design
- ✅ Consistent styling
- ✅ Smooth animations
- ✅ Clear visual hierarchy

**Pass/Fail:** ___________

---

## 🐛 Common Issues & Solutions

### Issue 1: App Won't Install
**Symptoms:** Build succeeds but app doesn't install

**Solutions:**
1. Check USB debugging is enabled
2. Uninstall old version from phone
3. Try "Clean Project" in Android Studio
4. Rebuild: Build → Clean Project → Build → Rebuild Project

---

### Issue 2: Gradle Sync Failed
**Symptoms:** Red errors in Android Studio

**Solutions:**
1. File → Invalidate Caches → Invalidate and Restart
2. Check internet connection
3. Update Android Studio
4. Sync Project with Gradle Files

---

### Issue 3: Device Not Detected
**Symptoms:** Phone not showing in device dropdown

**Solutions:**
1. Reconnect USB cable
2. Try different USB port
3. Restart ADB: Tools → Troubleshoot Device Connections
4. Check USB debugging is enabled
5. Try different USB cable

---

### Issue 4: App Crashes on Launch
**Symptoms:** App opens then immediately closes

**Solutions:**
1. Check Logcat for error messages
2. Verify Android version (must be API 24+)
3. Clear app data: Settings → Apps → AgriPulse → Clear Data
4. Reinstall app

---

### Issue 5: Thermal View Not Showing
**Symptoms:** Black screen on Scan screen

**Solutions:**
1. This is normal in emulator mode
2. Thermal emulator provides black background
3. Overlay and UI should still work
4. Scan functionality works normally

---

## 📊 Test Results Summary

### Overall Results
- **Total Tests:** 10
- **Passed:** _____
- **Failed:** _____
- **Success Rate:** _____%

### Critical Features
- [ ] Thermal scanning works
- [ ] Fever detection works
- [ ] Export/share works
- [ ] History displays correctly
- [ ] Analytics calculates correctly
- [ ] Navigation smooth
- [ ] No crashes

### Performance
- [ ] App starts in <3 seconds
- [ ] Scans complete in <2 seconds
- [ ] UI is responsive (60fps)
- [ ] No lag or stuttering

### Visual Quality
- [ ] Professional appearance
- [ ] Modern design
- [ ] Consistent styling
- [ ] Clear feedback

---

## 📝 Testing Checklist

### Before Testing
- [ ] Android Studio installed
- [ ] Project opened and synced
- [ ] Device connected or emulator running
- [ ] USB debugging enabled (physical device)

### During Testing
- [ ] Follow test steps exactly
- [ ] Note any issues or bugs
- [ ] Take screenshots of problems
- [ ] Record pass/fail for each test

### After Testing
- [ ] Review all test results
- [ ] Document any bugs found
- [ ] Verify critical features work
- [ ] Confirm app is submission-ready

---

## 🎯 Acceptance Criteria

### Must Pass (Critical)
- ✅ App installs without errors
- ✅ No crashes during normal use
- ✅ Scan functionality works
- ✅ Data persists across sessions
- ✅ Export/share works

### Should Pass (Important)
- ✅ All navigation works
- ✅ Visual quality is professional
- ✅ Performance is smooth
- ✅ Error handling is graceful

### Nice to Have (Optional)
- ✅ Animations are smooth
- ✅ All edge cases handled
- ✅ Help text is clear

---

## 📞 Support

If you encounter issues during testing:

1. **Check Logcat** in Android Studio (bottom panel)
2. **Search error message** online
3. **Review documentation** in this folder
4. **Contact team** for assistance

---

## ✅ Final Verification

Before submission, verify:
- [ ] All 10 tests passed
- [ ] No critical bugs
- [ ] App looks professional
- [ ] Features work as expected
- [ ] Ready for demo video

---

**Version:** 1.0
**Date:** February 9, 2026
**Status:** Ready for Testing
