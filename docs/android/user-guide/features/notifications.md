# Notifications

MealPlan+ sends three types of local push notifications to help you stay on track with your meal
logging and weekly planning.

---

## Enabling Notifications

1. Open the app and tap the **bell icon** in the top-right corner of the Home screen.
2. The Settings screen opens directly to the **Notifications** section.
3. Toggle **Enable Notifications** on.
   - On Android 13+, the system permission dialog appears. Tap **Allow**.
4. Individual notification types are enabled by default once the master toggle is on.

---

## Notification Types

### Meal Reminders
Sends a reminder at your configured time if you have not yet logged a meal for that slot.

| Slot      | Default time |
|-----------|-------------|
| Breakfast | 8:00 AM     |
| Lunch     | 1:00 PM     |
| Dinner    | 7:00 PM     |

You can adjust each time from Settings → Notifications.

### Streak Protection
Warns you in the evening if you have an active logging streak but have not logged anything today.

- Default alert time: **9:00 PM**
- Only fires if your streak is ≥ 1 day and today has zero calories logged.

### Weekly Plan Reminder
Reminds you every Monday morning to plan your week if you have no meal plans yet for the current
week.

- Fires at **8:00 AM** on Mondays.
- Suppressed automatically once at least one plan exists for the week.

---

## Adjusting Notification Times

1. Go to **Settings → Notifications**.
2. Tap the time displayed next to the notification slot you want to change.
3. Drag the **Hour** slider to your desired hour (12 AM – 11 PM).
4. Drag the **Minute** slider to your desired minute (:00 – :59).
5. Tap **Set**.

The alarm is rescheduled immediately — your next notification will fire at exactly the time
you selected.

---

## Turning Off a Specific Type

Each notification type has its own toggle inside **Settings → Notifications**:
- **Meal Reminders** — turns off all three meal-slot reminders at once.
- **Streak Protection** — turns off the evening streak alert.
- **Weekly Plan Reminder** — turns off the Monday morning nudge.

Toggling off **Enable Notifications** (the master switch) silences all notifications instantly,
regardless of the individual toggle states. All pending alarms are cancelled immediately.

---

## Frequently Asked Questions

**Why didn't I receive a reminder even though it's enabled?**
- Check that the system notification permission is granted: Device Settings → Apps → MealPlan+ →
  Notifications → Allow.
- On Android 12+ check **Settings → Apps → Special app access → Alarms & reminders** and ensure
  MealPlan+ is allowed. If not granted, notifications will fire but may be a few minutes late.
- The reminder is skipped if you already logged that meal slot earlier in the day.

**Can I set reminders to a specific minute?**
Yes. The time picker has separate hour and minute sliders, letting you set any time such as
"7:30 AM" or "10:15 PM". Notifications fire at exactly the configured time.

**Will I still get notifications after rebooting my device?**
Yes. MealPlan+ listens for `BOOT_COMPLETED` and automatically re-schedules all your alarms
after the device restarts.

**What happens if my device is in Do Not Disturb mode?**
The notification is posted but Android suppresses its sound/vibration according to your DND
settings. The notification still appears in the notification shade.
