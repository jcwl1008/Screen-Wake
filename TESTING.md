# Validation

- Compiled against Android SDK 35 using JDK 17 and Android build tools 35.0.0.
- APK signature verified with apksigner; signed using APK Signature Scheme v3 (min SDK 28).
- 20 URL policy cases passed: valid same-origin URLs, HTTPS normalization, forbidden schemes, credentials, malformed URLs, unrelated hosts and ports.
- Password logic tests passed with a JVM in-memory SharedPreferences double: salt variation, salted hashes, persistence, incorrect-password failures, persisted cooldown, cooldown recovery and successful reset of failure count.
- These unit tests exercise logic; they do not validate Android storage, UI, WebView rendering, hardware keys or device policy behavior.
- The software Android emulator did not boot within the available verification period. No successful runtime installation or UI test is claimed.
- Not tested on an actual Galaxy Tab S12. Follow the deployment guide's acceptance steps on the intended device, WebView version and management policy before customer use.

## v1 update (versionCode 2, versionName 1.0.1)

- Rebuilt and signature-verified using the original signing key.
- Preview omits the native top bar; Android Back remains the staff authentication entry.
- Windows setup PowerShell source parsed without errors. Mocked ADB setup-flow tests passed for success, paths with spaces, installation failure, policy errors returning exit code zero, offline devices and no selection.
- These are syntax and mocked workflow checks, not actual Windows Forms execution or USB provisioning tests. Physical Tab and Windows validation is still required.

## v1 clean display update (versionCode 3, versionName 1.0.2)

- Heyzine website toolbar, sound and zoom controls hidden by injected CSS on page commit and load completion. Other websites are untouched.
- DOM selector checks on the provided Heyzine page confirm sound/zoom are targeted and previous/next controls remain outside the hidden elements.
- APK rebuilt and signed with the original key. Physical-device UI verification remains required.
