# ShortStop — User Guide

---

## 📱 Getting Started

### First-Time Setup

1. Install ShortStop and open it
2. Complete the 4-page onboarding
3. Grant **Display over other apps** permission — required to show the pause screen
4. Agree to the usage monitoring disclosure
5. Grant **Usage access** permission — required to detect which app is in the foreground
6. You land on the main screen — the service starts automatically

### Quick Start

- Toggle ON the apps that distract you (Instagram, TikTok, YouTube, etc.)
- Use your phone normally
- Open a blocked app → after **7 seconds** a full-screen overlay appears
- Wait 30 seconds or tap Emergency Exit (costs 50 pts)

---

## 🎯 Core Features

### App Blocking

- Toggle any app ON to block it
- After **7 continuous seconds** on a blocked app, the overlay appears
- The overlay fades over **30 seconds** showing a motivational message
- **Cooldown**: if you re-open the same blocked app within **3 minutes** of leaving, the overlay fires instantly (no 7s wait)
- Toggling an app OFF cancels any pending reward for it

### Emergency Exit

- Tap **🚨 Emergency Exit** to dismiss the overlay immediately
- Costs **50 points**
- Requires a balance of at least 50 pts — button is **disabled** if you cannot afford it
- After emergency exit, the 7-second countdown restarts immediately
- Each emergency exit reduces your rank score by 20 points

### Study Mode

- Tap **Study Mode 25min (50 pts)** on any blocked app
- Costs **50 points** to activate
- Grants **25 minutes** of uninterrupted access — no overlay fires
- When 25 minutes expire, the overlay fires immediately
- Completing the session awards **+5 pts** directly to your balance

---

## 💎 Points & Rewards

### Earning Points

| Action | Points | When |
|---|---|---|
| Clean exit (stay away 10 min) | +10 pts | After 10 min away from the app |
| Daily streak bonus | +10 pts | First intervention of each day |
| Complete study session | +5 pts | When 25-min session ends |

### Spending Points

| Action | Cost | Requirement |
|---|---|---|
| Emergency Exit | −50 pts | Balance ≥ 50 |
| Activate Study Mode | −50 pts | Balance ≥ 50 |

### Pending Rewards

- Clean exit rewards and streak bonuses go to **Pending Rewards** first
- Tap **Claim** on the home screen to move them to your spendable balance
- Study session rewards go directly to your balance (no claiming needed)

### Clean Exit — Exact Rules

- Leave a blocked app → 10-minute countdown starts
- If you re-open the blocked app before 10 minutes → deadline resets, no reward
- Stay away the full 10 minutes → +10 pts added to pending rewards
- Unblocking the app cancels the pending reward

---

## 📊 Rank System

Your rank score is calculated from:

```
score = (streak × 25) + (study sessions × 15) + (minutes saved ÷ 5) − (emergency exits × 20)
```

| Score | Rank | Description |
|---|---|---|
| < 100 | 🌱 Sprout | You are still a slave to the notification. |
| 100–299 | 🔨 Apprentice | You are starting to fight back. |
| 300–749 | 🎯 Focused | You have reclaimed hours of your life. |
| 750–1499 | 🧘 Monk | Distractions have lost their power over you. |
| 1500–2999 | ⚔️ Sentinel | You are the master of your digital domain. |
| 3000+ | 👑 Sovereign | Total digital autonomy. |

Emergency exits **reduce** your rank score — use them sparingly.

---

## ⚙️ Settings

### Export Data
- Exports your stats and blocked apps as a JSON file
- Save to Google Drive or share it

### Reset All Data
- Permanently deletes all progress, points, blocked apps, and statistics
- Cannot be undone
- App restarts to onboarding after reset

---

## 🔧 Troubleshooting

### Overlay Not Appearing

1. Go to **Android Settings → Apps → Special app access → Usage access**
2. Enable **Permit usage access** for ShortStop
3. Go to **Android Settings → Apps → ShortStop → Display over other apps**
4. Enable it
5. Disable battery optimization for ShortStop (Settings → Battery → ShortStop)
6. Restart your device

### Service Keeps Stopping (Samsung / OEM devices)

Samsung and other OEM devices aggressively kill background services. Fix:
1. Open ShortStop
2. When the battery optimization dialog appears, tap **Open Settings**
3. Find ShortStop and set to **Unrestricted** or **Don't optimize**

### Points Not Updating

- Clean exit rewards only appear after **10 full minutes** away from the app
- Pending rewards must be **claimed** from the home screen before they appear in your balance
- Study session rewards (+5 pts) go directly to balance — no claiming needed

### Streak Not Increasing

- Streak increases when you have at least one intervention on a new day
- The streak date is based on your device's local date
- Make sure your device date/time is correct

---

## 🔒 Privacy

ShortStop collects **zero data**. Everything stays on your device.

- No internet permission — cannot make network calls
- No analytics, no crash reporting, no tracking
- All motivational quotes are stored locally in the app
- Database is encrypted with AES-256 (SQLCipher)
- You can export or delete all your data at any time from Settings

---

## 📞 Support

- **GitHub Issues**: [github.com/MD-KAMALUDDIN/SHORT-STOP/issues](https://github.com/MD-KAMALUDDIN/SHORT-STOP/issues)
- **Email**: mdkamaluddin7339@gmail.com
