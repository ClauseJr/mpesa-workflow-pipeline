# Connecting M-Pesa SMS to the app

## 1. Use a physical Android phone

SMS functionality should be tested on a real Android device.

USB debugging:

Android Settings → About phone → Build number → tap 7 times.

Then:

Developer options → USB debugging → ON.

Connect the phone to Android Studio.

## 2. Install the application

Run the `app` configuration from Android Studio.

On first launch Android will request:

- Read SMS
- Receive SMS

Grant both for development/testing.

## 3. Import your existing M-Pesa messages

Open:

M-Pesa Analytics → Import M-Pesa SMS

The app queries the Android SMS provider and sends each message through the parser.

Only messages identified as likely M-Pesa messages are converted into transactions.

## 4. Receive new M-Pesa messages

The manifest registers:

`android.provider.Telephony.SMS_RECEIVED`

The receiver is:

`MpesaSmsReceiver`

When an SMS arrives:

SMS → BroadcastReceiver → Parser → Room → UI

## 5. If live messages do not appear

Check:

- READ_SMS permission is granted.
- RECEIVE_SMS permission is granted.
- The phone actually received the SMS.
- The sender/body matches the parser's M-Pesa detection rules.
- Battery optimisation is not interfering.
- Test on the physical phone rather than an emulator.

## 6. Do NOT use real financial totals until the parser is validated

Before trusting the application:

- compare imported transaction count with your M-Pesa SMS history
- manually verify at least 30–50 transactions
- test sends, receives, paybill, till, withdrawals, airtime and reversals
- test duplicate messages
- test malformed messages
- test messages with different date formats

## 7. Privacy

The starter app is designed around local processing.

No M-Pesa PIN is required.

No M-Pesa login is required.

No cloud account is required.

No transaction should be sent to a server by the code in this starter.

Before production release, add database encryption and an explicit data deletion/export flow.
