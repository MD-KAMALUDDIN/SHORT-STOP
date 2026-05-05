# Play Console — Quick Reference

## Data Safety Form

**Does your app collect or share any of the required user data types?**
```
YES — App Activity is accessed (processed locally only, never transmitted)
```

> Google's definition of "collect" includes accessing data on-device even if never sent to a server.
> Because ShortStop uses UsageStatsManager, you must declare App Activity.

**Data type: App Activity**
- Collected: Yes (UsageStatsManager reads foreground app package name)
- Shared with third parties: No
- Processed ephemerally (in memory only): Yes — real-time, not stored permanently
- Required or optional: Required (core app functionality)
- Purpose: App functionality — detecting foreground app to trigger intervention overlay

**Is data encrypted in transit?**
```
Not applicable — data is never transmitted off the device
```

**Do you provide a way for users to request data deletion?**
```
YES — Settings → Reset All Data permanently deletes all on-device data
```

**Does your app use the INTERNET permission?**
```
NO
```

---

## Usage Stats Permission

**Does your app use a permission that requires additional disclosure?**
```
YES — PACKAGE_USAGE_STATS
```

**Explanation to provide:**
```
ShortStop uses the PACKAGE_USAGE_STATS permission to detect which app is
currently in the foreground. This is the only purpose. When the user opens
a blocked app, ShortStop displays a 30-second intervention overlay to help
manage screen time. All processing is local and real-time on the device.
No data is transmitted off the device.
```

---

## Foreground Service Special Use

**Why does your app use a foreground service with type specialUse?**
```
ShortStop runs a foreground service to continuously monitor app usage via
UsageStatsManager (polling every 3 seconds). This is required to detect
when the user opens a blocked app and display an intervention overlay.
The service type is specialUse as it does not fit standard categories
(dataSync, mediaPlayback, etc.). No data is transmitted. The service is
declared with exported=false and includes the PROPERTY_SPECIAL_USE_FGS_SUBTYPE
property with a full description.
```

---

## Privacy Policy URL

```
https://github.com/MD-KAMALUDDIN/SHORT-STOP/blob/main/PRIVACY_POLICY.md
```

---

## App Description Key Lines

```
🔒 100% offline • AES-256 encrypted database • Zero data transmitted
```

---

## Prominent Disclosure (Required)

Google requires a prominent disclosure inside the app before the system permission dialogs appear.
ShortStop's onboarding screens already cover this:

- **Overlay permission screen**: explains the 30-second pause screen
- **Usage Stats disclosure screen**: explains that UsageStatsManager is used to detect foreground apps

Take screenshots of these screens for your Play Console submission.

---

## Content Rating (IARC)

- **Ads**: No
- **App Access**: All functionality available without special access (no login required)
- **Violence / Language / Substances**: None
- **Expected rating**: Everyone (3+)

---

## Checklist Before Submitting

- [ ] Data Safety: App Activity declared, not shared, processed locally
- [ ] Privacy Policy URL added
- [ ] Usage Stats permission explanation provided
- [ ] Special Use foreground service explanation provided
- [ ] Prominent disclosure screenshots uploaded
- [ ] IARC questionnaire completed (Everyone rating)
- [ ] App description mentions "100% offline"
- [ ] Screenshots show home screen and overlay
- [ ] Feature graphic (1024×500px) uploaded
- [ ] App icon (512×512px) uploaded
- [ ] `versionCode = 1`, `versionName = "1.0"`

---

## Expected Timeline

- Upload: 5 minutes
- Review: 1–3 days
- Live: within 24 hours after approval
