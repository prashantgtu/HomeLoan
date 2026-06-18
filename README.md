# Home Loan EMI Calculator (v2.1)

A native Android app for home loan planning, built with Kotlin and Jetpack Compose. It models full bank-style amortization schedules and lets you experiment with prepayments, interest rate changes, and repayment strategies before committing to a loan.

## New in Version 2.1
- **Day/Night Theme Toggle**: Switch dynamically between light (day) and dark (night) theme styles from the top bar.
- **Offline PIN Login**: Secure your financial planning scenarios offline with a 4-digit PIN setup and authentication screen (introduced in v2.0).

## Try the App (Download APK)

You can download the pre-built debug APK directly from this repository:
- [HomeLoan.apk](HomeLoan.apk) (Download and install on your Android device/emulator)

## Features

- **Dynamic Theme Mode**: Day/Night style toggle.
- **Offline 4-Digit PIN Security**: Setup and login with a secure 4-digit PIN.
- EMI calculation from principal, interest rate, and tenure
- Month-by-month amortization schedule with a principal-vs-interest breakdown
- Mid-tenure interest rate revisions with automatic recalculation of the remainder
- One-off lump-sum and recurring extra payments
- Choice of "reduce EMI" or "reduce tenure" strategy whenever you prepay or revise the rate
- Scrollable amortization table showing running balances and interest saved
- Loan balance chart over time
- Export the full schedule to CSV, saved to the device's Downloads folder

## Tech Stack

- Kotlin + Jetpack Compose (Material 3) for the UI
- Kotlin Coroutines for asynchronous work
- Room for local persistence
- Retrofit, Moshi, and OkHttp for networking
- Navigation Compose for in-app navigation
- Robolectric + Roborazzi for unit and Compose screenshot tests

## Project Structure

- `app/` — the application module: UI screens, ViewModels, Room database, and networking code
- `app/src/main/java/.../LoanMath.kt` — a pure Kotlin amortization/EMI engine with no Android framework dependencies, so it's fully unit-testable in isolation

## Getting Started

### Prerequisites

- Android Studio Otter 3 Feature Drop (2025.2.3) or newer — required for AGP 9.1.x
- JDK 17
- An Android device or emulator running API 24 or higher

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/prashantgtu/HomeLoan.git
   ```
2. Open the project folder in Android Studio and let Gradle sync.
3. If you're using any API-key-gated features, copy `app/.env.example` to `app/.env` and fill in your own values. `.env` is gitignored and never committed.

### Run

Click **Run** in Android Studio, or from a terminal:

```bash
./gradlew installDebug
```

### Build an APK

- **Debug APK** — Build → Build APK(s) in Android Studio, or run:
  ```bash
  ./gradlew assembleDebug
  ```
  The output will be generated at `app/build/outputs/apk/debug/`.

- **Release APK** — requires a signing keystore. Set the `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables (or place an upload keystore named `my-upload-key.jks` at the project root), then run:
  ```bash
  ./gradlew assembleRelease
  ```

## Testing

```bash
./gradlew test
```

Runs unit tests, including Robolectric-based Compose screenshot tests via Roborazzi.


