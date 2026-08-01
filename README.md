# PaisaTrack

A personal Android app that reads your bank transaction SMS (Axis, HDFC, Union Bank,
Kotak, Indian Bank), sums up your spending, and shows it on a home-screen widget.
Credit cards are tracked by their billing cycle; accounts by calendar month.
You can also add cash entries by typing plain English ("spent 70 at tea shop").

## Get the app onto your phone (no Android Studio needed)

1. Push this project to a GitHub repo (private is fine).
2. GitHub Actions builds the APK automatically — watch the **Actions** tab.
3. When the run finishes (green check), open it → **Artifacts** → download **PaisaTrack-apk**.
4. Unzip → you get `app-debug.apk`. Put it on your phone.
5. Tap the APK → allow "install unknown apps" → Install.
6. Open the app → grant SMS + notification permission.
7. Long-press home screen → Widgets → PaisaTrack → drag the widget out.

## Changing things later
Edit code → push → Actions rebuilds → download the new APK → reinstall.
