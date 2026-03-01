# Quick Reference Guide - Responsive Design

## Screen Size Configurations

```
┌─────────────────────────────────────────────────────────────┐
│                    FLIR ACE CAMERA (5")                     │
│                  PRIMARY TARGET DEVICE                       │
├─────────────────────────────────────────────────────────────┤
│ Configuration: values-w480dp-h800dp/                        │
│ Touch Targets: 56-80dp (glove-friendly)                    │
│ Scan Overlay:  280dp                                        │
│ Button Height: 64dp                                         │
│ Text Range:    13-38sp                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    DEFAULT (PHONES)                          │
├─────────────────────────────────────────────────────────────┤
│ Configuration: values/                                       │
│ Touch Targets: 48-72dp                                      │
│ Scan Overlay:  260dp                                        │
│ Button Height: 56dp                                         │
│ Text Range:    12-36sp                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  TABLETS (7-10")                            │
├─────────────────────────────────────────────────────────────┤
│ Configuration: values-sw600dp/                              │
│ Touch Targets: 48-72dp                                      │
│ Scan Overlay:  320dp                                        │
│ Button Height: 56dp                                         │
│ Text Range:    16-42sp                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                LARGE TABLETS (10"+)                         │
├─────────────────────────────────────────────────────────────┤
│ Configuration: values-sw720dp/                              │
│ Touch Targets: 48-72dp                                      │
│ Scan Overlay:  380dp                                        │
│ Button Height: 56dp                                         │
│ Text Range:    18-48sp                                      │
└─────────────────────────────────────────────────────────────┘
```

## ACE Design System - Spacing Scale

```
Base Unit: 4px (multiplies by 4)

spacing_none    = 0dp
spacing_xxs     = 4dp   ▌
spacing_xs      = 8dp   ▌▌
spacing_sm      = 16dp  ▌▌▌▌
spacing_md      = 24dp  ▌▌▌▌▌▌
spacing_lg      = 32dp  ▌▌▌▌▌▌▌▌
spacing_xl      = 48dp  ▌▌▌▌▌▌▌▌▌▌▌▌
spacing_xxl     = 64dp  ▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌
```

## Touch Target Sizes

```
┌──────────────────────────────────────────────────────────┐
│                    TOUCH TARGETS                         │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Minimum (48dp)         ┌────────────┐                  │
│  Android Standard       │            │                  │
│                         │            │                  │
│                         └────────────┘                  │
│                                                          │
│  ACE Camera (56dp)      ┌──────────────┐                │
│  Glove-Friendly         │              │                │
│                         │              │                │
│                         └──────────────┘                │
│                                                          │
│  Comfortable (64dp)     ┌────────────────┐              │
│  Standard Button        │                │              │
│                         │                │              │
│                         └────────────────┘              │
│                                                          │
│  Large (80dp)           ┌──────────────────┐            │
│  Menu Cards             │                  │            │
│                         │                  │            │
│                         └──────────────────┘            │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## Focus States

```
┌──────────────────────────────────────────────────────────┐
│                    FOCUS INDICATOR                       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Unfocused State:                                        │
│  ┌────────────────┐                                      │
│  │    Button      │  No outline                          │
│  └────────────────┘                                      │
│                                                          │
│  Focused State (4dp green):                              │
│  ╔════════════════╗                                      │
│  ║    Button      ║  Bold green outline                  │
│  ╚════════════════╝                                      │
│                                                          │
│  Color: #4CAF50 (Material Green)                         │
│  Width: 4dp                                              │
│  Radius: 14dp                                            │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## Hardware Button Navigation

```
┌──────────────────────────────────────────────────────────┐
│              FLIR ACE HARDWARE BUTTONS                   │
├──────────────────────────────────────────────────────────┤
│                                                          │
│                      [On/Off]                            │
│                                                          │
│                        [Up]                              │
│                         ▲                                │
│                         │                                │
│              [Left] ◄───┼───► [Right]                    │
│                         │                                │
│                         ▼                                │
│                       [Down]                             │
│                                                          │
│                      [Select]                            │
│                       (OK)                               │
│                                                          │
│                       [Back]                             │
│                                                          │
├──────────────────────────────────────────────────────────┤
│  Up/Down/Left/Right: Navigate between elements           │
│  Select: Activate focused element                        │
│  Back: Return to previous screen                         │
│  On/Off: Power management                                │
└──────────────────────────────────────────────────────────┘
```

## Dimension Resource Usage

```xml
<!-- Spacing -->
android:padding="@dimen/spacing_sm"              <!-- 16dp -->
android:layout_margin="@dimen/spacing_md"        <!-- 24dp -->
android:layout_marginBottom="@dimen/spacing_lg"  <!-- 32dp -->

<!-- Touch Targets -->
android:layout_width="@dimen/touch_target_min"   <!-- 48dp / 56dp on ACE -->
android:minHeight="@dimen/button_height_standard" <!-- 56dp / 64dp on ACE -->

<!-- Text Sizes -->
android:textSize="@dimen/text_size_body"         <!-- 14sp / 15sp on ACE -->
android:textSize="@dimen/text_size_heading"      <!-- 18sp / 19sp on ACE -->
android:textSize="@dimen/text_size_display"      <!-- 36sp / 38sp on ACE -->

<!-- Corners & Elevation -->
app:cardCornerRadius="@dimen/card_corner_radius" <!-- 16dp -->
app:cardElevation="@dimen/card_elevation"        <!-- 4dp -->

<!-- Focus State -->
android:focusable="true"
android:foreground="@drawable/focus_highlight"
```

## Style Usage

```xml
<!-- ACE Button -->
<Button
    style="@style/Widget.ACE.Button"
    android:layout_width="match_parent"
    android:layout_height="@dimen/button_height_standard"
    android:text="SCAN"
    android:focusable="true"
    android:foreground="@drawable/focus_highlight"/>

<!-- ACE Menu Card -->
<androidx.cardview.widget.CardView
    style="@style/Widget.ACE.MenuCard"
    android:layout_width="match_parent"
    android:layout_height="@dimen/card_menu_height"
    android:focusable="true"
    android:foreground="@drawable/focus_highlight">
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="SCAN"/>
</androidx.cardview.widget.CardView>

<!-- ACE Icon Button -->
<ImageButton
    style="@style/Widget.ACE.IconButton"
    android:layout_width="@dimen/touch_target_min"
    android:layout_height="@dimen/touch_target_min"
    android:src="@drawable/ic_back"
    android:contentDescription="@string/back_button"
    android:focusable="true"
    android:foreground="@drawable/focus_highlight"/>
```

## Responsive Layout Pattern

```xml
<!-- Constrained width for large screens -->
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_md"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintWidth_max="600dp">
        
        <!-- Content here -->
        
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

## File Structure

```
app/src/main/res/
├── values/
│   ├── dimens.xml          ← Base dimensions
│   ├── styles.xml          ← ACE styles
│   ├── colors.xml          ← Color palette
│   └── strings.xml         ← Accessibility strings
│
├── values-w480dp-h800dp/   ← FLIR ACE camera (5")
│   └── dimens.xml
│
├── values-sw600dp/         ← Tablets (7-10")
│   └── dimens.xml
│
├── values-sw720dp/         ← Large tablets (10"+)
│   └── dimens.xml
│
├── drawable/
│   └── focus_highlight.xml ← Focus indicator
│
└── layout/
    ├── fragment_landing.xml
    ├── fragment_menu.xml
    ├── fragment_scan.xml
    ├── fragment_history.xml
    ├── fragment_animal_history.xml
    ├── fragment_scan_detail.xml
    └── fragment_analytics.xml
```

## Testing Commands

```bash
# Build APK
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk

# Check current configuration
adb shell dumpsys window displays | grep -i "cur="

# View layout inspector
# Android Studio → Tools → Layout Inspector

# Test with different screen sizes
# Android Studio → Tools → Device Manager → Create Virtual Device
```

## Common Dimension Values

| Resource Name              | Default | ACE (5") | Tablet | Large  |
|---------------------------|---------|----------|--------|--------|
| `spacing_xs`              | 8dp     | 8dp      | 8dp    | 8dp    |
| `spacing_sm`              | 16dp    | 16dp     | 16dp   | 16dp   |
| `spacing_md`              | 24dp    | 24dp     | 32dp   | 40dp   |
| `spacing_lg`              | 32dp    | 32dp     | 40dp   | 48dp   |
| `touch_target_min`        | 48dp    | 56dp     | 48dp   | 48dp   |
| `button_height_standard`  | 56dp    | 64dp     | 56dp   | 56dp   |
| `card_menu_height`        | 72dp    | 80dp     | 80dp   | 88dp   |
| `scan_overlay_size`       | 260dp   | 280dp    | 320dp  | 380dp  |
| `text_size_body`          | 14sp    | 15sp     | 16sp   | 18sp   |
| `text_size_heading`       | 18sp    | 19sp     | 20sp   | 22sp   |
| `text_size_display`       | 36sp    | 38sp     | 42sp   | 48sp   |

---

**Quick Tip**: When in doubt, use dimension resources! They automatically adapt to the screen size.
