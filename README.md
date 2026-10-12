# M-Pesa Analytics — Android

Privacy-first, offline-capable Android starter application written in Kotlin.

## What is included

- Kotlin + Jetpack Compose
- Room / SQLite local database
- M-Pesa SMS parser
- Historical SMS import
- New SMS BroadcastReceiver
- Basic transaction model
- Deduplication by transaction code
- Transaction list UI
- Hilt dependency injection
- WorkManager dependency
- Android SMS permissions

## IMPORTANT

This is an engineering starter, not a production-ready financial application.

The parser contains generic M-Pesa patterns and MUST be tested against real SMS examples before trusting the financial totals.

Do not paste your M-Pesa PIN into the app or source code.

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- A physical Android phone is strongly recommended for SMS testing.

## Build

1. Open this folder in Android Studio.
2. Allow Gradle to sync.
3. Run the app on a physical Android device.
4. Grant SMS permissions.
5. Tap "Import M-Pesa SMS".
6. The app will scan the device SMS database and parse messages that look like M-Pesa notifications.

## Testing live SMS

After permissions are granted:

1. Keep the application installed.
2. Make a legitimate small M-Pesa transaction or receive an ordinary M-Pesa notification.
3. Android should deliver the SMS broadcast to the receiver if the device/OS allows the app to receive it.
4. The receiver parses it and stores the transaction locally.
5. Open the app to see the transaction.

## Important Android / Google Play limitation

SMS permissions are restricted by Android and Google Play policy. A side-loaded/development build can be tested on a personal device, but Play Store publication requires checking the current SMS permission and default-handler/core-functionality requirements.

For a private personal application, development/sideloading is the simplest path.

## Next engineering steps

1. Expand the parser using a real anonymised SMS test corpus.
2. Add exact transaction-type parsers.
3. Add parser confidence and review queue.
4. Add proper duplicate fingerprinting.
5. Add daily/weekly/monthly/yearly SQL analytics.
6. Add charts.
7. Add category management.
8. Add encrypted database storage.
9. Add export/delete-all controls.
10. Add comprehensive unit tests.
