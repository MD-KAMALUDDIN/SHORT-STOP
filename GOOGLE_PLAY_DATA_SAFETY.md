# Google Play — Data Safety & Submission Guide

---

## Data Safety Form

### Does your app collect or share any of the required user data types?
**YES** — App Activity is accessed (but processed locally only, never transmitted)

> Google's definition: "collect" means accessing data on-device, even if never sent to a server.
> Because ShortStop uses `UsageStatsManager`, you must declare App Activity.

### Data Type: App Activity
- **Collected**: Yes
- **Shared with third parties**: No
- **Processed ephemerally (in memory only)**: Yes
- **Required or optional**: Required (core app functionality)
- **Purpose**: App functionality — detecting foreground app to trigger intervention overlay

### Is data encrypted in transit?
**Not applicable** — data is never transmitted off the device

### Do you provide a way for users to request data deletion?
**YES** — Settings → Reset All Data permanently deletes all on-device data

### Does your app use the INTERNET permission?
**NO** — not declared in `AndroidManifest.xml`

---

## Usage Stats Permission Declaration

**Why does your app use `PACKAGE_USAGE_STATS`?**

```
ShortStop uses the PACKAGE_USAGE_STATS permission to detect which app is
currently in the foreground. This is required to:

1. Detect when the user opens a blocked app
2. Display a 30-second intervention overlay to help manage screen time
3. Track usage statistics locally on the device for the rank/points system

The permission is used ONLY for app usage monitoring. We do NOT:
- Access content within apps
- Read text, passwords, or sensitive information
- Transmit any data off the device
- Use it for any purpose other than digital wellness intervention

All data processing happens locally and in real-time on the user's device.
No data is stored permanently beyond what the user can see and delete in the app.
```

---

## Foreground Service Special Use Declaration

The manifest includes:
```xml
<service
    android:name=".ShortStopService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="ShortStop monitors app usage via UsageStatsManager to display
        digital wellness intervention overlays. No data is collected or transmitted." />
</service>
```

**Why `specialUse`?** The service monitors foreground app usage for digital wellness intervention. This does not fit `dataSync`, `mediaPlayback`, or other standard types. `specialUse` is the correct type and avoids the 6-hour timeout.

---

## Privacy Policy

Host `PRIVACY_POLICY.md` at a public URL and enter it in the Play Console.

Suggested URL: `https://github.com/MD-KAMALUDDIN/SHORT-STOP/blob/main/PRIVACY_POLICY.md`

---

## Pre-Submission Checklist

- [ ] `AndroidManifest.xml` has NO `android.permission.INTERNET`
- [ ] Data Safety form: "NO" for data collection
- [ ] Privacy Policy URL provided and accessible
- [ ] Usage Stats permission explanation submitted
- [ ] `foregroundServiceType="specialUse"` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
- [ ] App description mentions "100% offline" and "zero data collection"
- [ ] Screenshots show the app working (no network required)
- [ ] `versionCode = 1`, `versionName = "1.0"`

---

## If Google Asks for Clarification

**About Usage Stats permission:**
```
ShortStop uses PACKAGE_USAGE_STATS exclusively to detect the foreground app
package name in real-time. This triggers a 30-second intervention overlay
when the user opens a blocked app. All processing is local. No data is
stored beyond what the user sees in the app's statistics screen, and the
user can delete all data at any time via Settings → Reset All Data.
```

**About the foreground service:**
```
ShortStop runs a foreground service to continuously monitor app usage via
UsageStatsManager. This is required for the core digital wellness feature —
detecting when a blocked app is opened and showing an intervention overlay.
The service type is specialUse as it does not fit standard categories.
No data is transmitted. The service is declared with exported=false.
```

**About data collection:**
```
Zero data collection. The app has no INTERNET permission and cannot make
network calls. All user data (blocked apps, points, streaks) is stored
locally in an AES-256 encrypted SQLCipher database on the user's device.
The user can export or delete all data at any time.
```
