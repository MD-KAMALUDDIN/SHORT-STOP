# ShortStop — Play Store Listing

---

## SHORT DESCRIPTION (80 characters max)

```
Break phone addiction with smart 30-second interventions. 100% offline.
```

---

## FULL DESCRIPTION

```
🛑 TAKE BACK CONTROL OF YOUR TIME

ShortStop helps you break free from endless scrolling and phone addiction. When you open a distracting app, ShortStop gives you a 30-second pause to reflect before you fall into the scroll trap.

📱 HOW IT WORKS

1. Toggle ON the apps that distract you (TikTok, Instagram, YouTube, etc.)
2. Use your phone normally
3. Open a blocked app → after 7 seconds, a full-screen motivational overlay appears
4. The overlay fades over 30 seconds — you cannot skip it without a penalty
5. Stay away for 10 minutes → earn reward points
6. Build streaks, climb ranks, reclaim your time

✨ KEY FEATURES

🎯 SMART INTERVENTIONS
• 7-second trigger catches mindless scrolling before it starts
• 30-second pause creates a real pattern interrupt
• Cooldown system — re-opening within 3 minutes triggers instant overlay
• Emergency Exit available only if you have 50+ points (costs 50 pts)

📚 STUDY MODE
• Need a blocked app for work? Activate 25-minute focused sessions
• Costs 50 points — encourages intentional use
• Completing a session awards 5 points directly to your balance

🏆 MEANINGFUL GAMIFICATION
• Earn points: +10 for clean exits, +10 daily streak bonus, +5 study sessions
• Spend points: Emergency Exit (−50), Study Mode (−50)
• Emergency exits REDUCE your rank score — real consequences
• 6 ranks: 🌱 Sprout → 🔨 Apprentice → 🎯 Focused → 🧘 Monk → ⚔️ Sentinel → 👑 Sovereign

📊 RANK SCORE FORMULA
score = (streak × 25) + (study sessions × 15) + (minutes saved ÷ 5) − (emergency exits × 20)

🔒 PRIVACY FIRST — ZERO DATA COLLECTION
• No internet permission — physically cannot make network calls
• No tracking, no analytics, no crash reporting
• All data encrypted with AES-256 (SQLCipher + AndroidKeyStore)
• 100% offline — all quotes stored locally
• Export or delete your data anytime

⚙️ RELIABLE BY DESIGN
• Foreground service with specialUse type — no 6-hour timeout
• Reward timers stored as DB timestamps — survive device reboots
• Boot receiver restarts service automatically after reboot
• Battery optimization prompt on first launch

💎 100% FREE — NO CATCHES
• No ads, ever
• No in-app purchases
• No premium tiers
• No data selling
• All features from day one

🛠️ TECHNICAL
• Minimum Android 8.0 (API 26)
• Uses Usage Stats permission for app monitoring
• Requires overlay permission for intervention screens
• All processing happens locally on your device
• Lightweight: ~15 MB installed
```

---

## WHAT'S NEW — Version 1.0

```
🎉 ShortStop v1.0

• Smart 7-second intervention trigger
• 30-second motivational overlay
• Emergency Exit disabled when balance < 50 pts
• 25-minute Study Mode (Pomodoro)
• Clean exit rewards (10 min away = +10 pts)
• Daily streak bonus
• 6-rank progression system (Sprout → Sovereign)
• AES-256 encrypted database
• 100% offline, zero data collection
```

---

## CATEGORY
**Productivity**

---

## CONTENT RATING
**Everyone**

---

## KEYWORDS
```
digital wellness, phone addiction, app blocker, screen time, productivity, focus, self control, mindfulness, social media detox, doomscrolling, distraction blocker, study mode, pomodoro, habit tracker
```

---

## NOTES FOR SUBMISSION

1. Service type is `specialUse` — include the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification already in the manifest
2. No data collection — select "This app doesn't collect or share any user data" in Data Safety
3. No internet permission — confirm in Data Safety form
4. Usage Stats permission explanation: used only to detect foreground app for intervention overlays, all processing local and real-time
5. Privacy Policy URL: link to `PRIVACY_POLICY.md` in the repository
