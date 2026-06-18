# Home Loan Calculator (Android)

This is a comprehensive Home Loan planner and EMI calculator designed specifically for Android. It simulates bank-level amortization schedules, incorporating prepayments, interest rate revisions, and repayment strategies.

## Features Preserved and Ported
- **Home loan / EMI calculation**: Calculate standard monthly payments based on principal, rate, and tenure.
- **Bank-grade amortization logic**: Accurately computes month-by-month principal and interest.
- **Interest rate change simulation**: Revise interest rates at given months and recalculate the remainder.
- **Extra / lump-sum payment simulation**: Supports both recurring monthly extra payments and lump-sum events.
- **Comparison of strategies**: Toggle between "reduce EMI" or "reduce tenure" when making prepayments or taking rate changes.
- **Full amortization schedule**: A scrollable data table breaking down all payments, balances, and savings.
- **Balance Chart**: A visual graphical representation of the loan balance over time.
- **Export to CSV**: Download the schedule as a CSV file to Android's Downloads folder to view in Excel.

## Architectural Notes
- The logic is decoupled into a pure Kotlin object (`LoanMath.kt`), fulfilling the same role as a pure Dart service, making it highly testable and framework-independent.
- Implemented entirely in **Kotlin** and **Jetpack Compose**, as this is a native Android cloud build environment.
- The UI mimics clean banking-style material cards with expandable/scrollable data sections.

## How to Build and Run
- Built with Gradle (`gradle assembleDebug`) in this AI Studio workspace.
- The compiled APK is available and automatically run on the attached Android emulator view.
