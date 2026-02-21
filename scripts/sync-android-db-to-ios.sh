#!/bin/bash
# Sync Android SQLite DB to iOS simulator
# Usage: ./scripts/sync-android-db-to-ios.sh
#
# Requires: adb in PATH, iOS simulator running with app installed

set -e
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"

ANDROID_PKG="com.mealplanplus"
DB_NAME="mealplan_database"
TMP_DB="/tmp/android_mealplan.db"
IOS_DEVICE="A016C2C2-4A34-489A-A4CB-391E5503115B"  # iPhone 17 Pro simulator

echo "=== MealPlan+ Android→iOS DB Sync ==="

# 1. Check adb device
echo "Checking Android device..."
adb devices | grep -v "List of" | grep "device" || { echo "ERROR: No Android device found"; exit 1; }

# 2. Pull Android DB (checkpoint WAL first to get all data)
echo "Pulling Android DB..."
adb shell "run-as $ANDROID_PKG cat /data/data/$ANDROID_PKG/databases/$DB_NAME" > "$TMP_DB"
adb shell "run-as $ANDROID_PKG cat /data/data/$ANDROID_PKG/databases/${DB_NAME}-wal" > "${TMP_DB}-wal" 2>/dev/null || true
sqlite3 "$TMP_DB" "PRAGMA wal_checkpoint(FULL);" 2>/dev/null || true

# 3. Fix schema version: Room uses 13, SQLDelight iOS expects 1
echo "Patching schema version (13→1)..."
sqlite3 "$TMP_DB" "PRAGMA user_version = 1;"

# 4. Find iOS app container
echo "Finding iOS app container..."
IOS_APP_DIR=$(find "$HOME/Library/Developer/CoreSimulator/Devices/$IOS_DEVICE/data/Containers/Data/Application" \
  -name "mealplan.db" 2>/dev/null | head -1 | xargs dirname 2>/dev/null)

if [ -z "$IOS_APP_DIR" ]; then
  echo "ERROR: iOS app DB not found. Launch the app on the simulator first."
  exit 1
fi
echo "Found iOS DB at: $IOS_APP_DIR"

# 5. Copy DB
echo "Copying DB to iOS simulator..."
cp "$TMP_DB" "$IOS_APP_DIR/mealplan.db"
rm -f "$IOS_APP_DIR/mealplan.db-wal" "$IOS_APP_DIR/mealplan.db-shm" 2>/dev/null

# 6. Set UserDefaults: user_id=1, is_logged_in=true
IOS_PLIST=$(find "$HOME/Library/Developer/CoreSimulator/Devices/$IOS_DEVICE/data/Containers/Data/Application" \
  -name "com.mealplanplus.ios.plist" 2>/dev/null | head -1)

if [ -n "$IOS_PLIST" ]; then
  echo "Updating UserDefaults (user_id=1, is_logged_in=true)..."
  plutil -replace user_id -integer 1 "$IOS_PLIST"
  plutil -replace is_logged_in -bool true "$IOS_PLIST"
fi

# 7. Summary
echo ""
echo "=== Sync complete ==="
sqlite3 "$TMP_DB" "SELECT 'Users: ' || count(*) FROM users; SELECT 'Diets: ' || count(*) FROM diets; SELECT 'Meals: ' || count(*) FROM meals; SELECT 'Plans: ' || count(*) FROM plans;"
echo ""
echo "Restart the iOS simulator app to see the synced data."
