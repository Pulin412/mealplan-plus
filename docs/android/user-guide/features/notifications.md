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

- Fires from **8:00 AM onwards** on Mondays.
- Suppressed automatically once at least one plan exists for the week.

---

## Adjusting Notification Times

1. Go to **Settings → Notifications**.
2. Tap the time next to the notification slot you want to change.
3. Drag the slider to the desired hour and tap **Set**.

Changes take effect at the next hourly check (within 60 minutes).

---

## Turning Off a Specific Type

Each notification type has its own toggle inside **Settings → Notifications**:
- **Meal Reminders** — turns off all three meal-slot reminders at once.
- **Streak Protection** — turns off the evening streak alert.
- **Weekly Plan Reminder** — turns off the Monday morning nudge.

Toggling off **Enable Notifications** (the master switch) silences all notifications instantly,
regardless of the individual toggle states.

---

## Frequently Asked Questions

**Why didn't I receive a reminder even though it's enabled?**
- Check that the system notification permission is granted: Device Settings → Apps → MealPlan+ →
  Notifications → Allow.
- The workers run hourly. If you enabled notifications after the target hour, the reminder fires
  the next day at the configured time.
- The reminder is skipped if you already logged that meal slot earlier in the day.

**Can I change the reminder to a specific minute, not just the hour?**
Not currently. Notifications fire at the top of the configured hour (± a few minutes for
WorkManager scheduling).

**What happens if my device is in Do Not Disturb mode?**
The notification is posted but Android suppresses its sound/vibration according to your DND
settings. The notification still appears in the notification shade.
