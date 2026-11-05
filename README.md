# Screen Wake

**繁體中文｜[English](#english)**

為店頭平板而設的 Android 網頁展示 App。前線自行輸入網址，展示期間保持螢幕常亮，並以密碼保護管理功能。介面採用白底、圓角卡片與藍色按鈕。

## AutoFlip Simple 版

| 版本 | 下載 |
| --- | --- |
| **Unslop UI 1.0.5**｜手機設定頁、全螢幕展示快捷入口 | [ScreenWake_v1_AutoFlip_Simple_unslopUI.apk](downloads/ScreenWake_v1_AutoFlip_Simple_unslopUI.apk) |
| **原版 1.0.4**｜原有設定介面 | [ScreenWake_v1_AutoFlip_Simple.apk](downloads/ScreenWake_v1_AutoFlip_Simple.apk) |

兩款 APK 使用同一個 Android 套件名稱，一次可安裝其中一款。同簽署的原版可直接升級到 Unslop UI，沿用網址、前線密碼和翻頁設定。要由 1.0.5 裝回 1.0.4，Android 一般需要先解除安裝，這會清除本機設定。

Simple 版適合手機和平板全螢幕循環展示 Heyzine 電子書。以下按鈕名稱以 Unslop UI 版為準。

1. 按「展示網址」，輸入 Heyzine 電子書的 HTTPS 網址。
2. 按「自動翻頁」，開啟功能，設定 **2–300 秒**的翻頁間隔，再儲存。
3. 按「開始全螢幕展示」。電子書到尾頁會返到開首，然後繼續翻頁。
4. 如要更改設定或結束展示，用系統返回鍵或返回手勢開啟前線管理。已設定前線密碼的裝置會要求驗證。

Simple 版可覆蓋安裝同簽署的舊版，沿用已儲存的網址、前線密碼及自動翻頁設定。預覽模式下，Home 和多工仍由 Android 控制。

## 標準版下載與設定

- [ScreenWake_v1.apk](downloads/ScreenWake_v1.apk) — 標準版安裝檔（內部版本 1.0.2 / versionCode 3），可覆蓋同簽署舊版。
- [Windows 一鍵初始設定腳本](windows-setup/) — 先下載官方 Platform Tools，再執行 `START_SETUP.cmd`。
- [繁體中文設定指南](SETUP_TC.md)
- [驗證記錄及限制](TESTING.md)

1. 在 Android 9 或以上安裝 APK，設定展示網址與前線密碼。
2. 按「預覽網頁」即可以全螢幕瀏覽；App 頂部工具列會隱藏，螢幕在 App 前景時保持常亮。
3. 用系統返回手勢／返回鍵輸入密碼，開啟前線管理。
4. 要封住 Home、多工及通知列，先由 IT 完成 Device Owner 或 EMM/MDM 的 Lock Task 設定，再按「開始鎖定展示」。

## 功能

| 功能 | 行為 |
| --- | --- |
| 展示網址 | 前線自行輸入 HTTPS 網址，儲存在裝置；設定密碼後更改網址需驗證 |
| 常亮 | 使用 Android `FLAG_KEEP_SCREEN_ON`，App 在前景時避免自動熄屏 |
| 全螢幕預覽 | 隱藏 App 頂部工具列；返回鍵開啟密碼驗證 |
| Heyzine 清爽顯示 | 隱藏網頁工具列、聲音及放大圖示，保留翻頁操作 |
| 前線管理 | 重載、更改網址、更改密碼、結束展示 |
| 密碼 | 隨機 salt + PBKDF2-HMAC-SHA256；連續錯誤 5 次暫停 30 秒 |
| 展示鎖定 | 透過 Android Lock Task，需 Device Owner 或外部 DPC 允許 |
| Windows 初始設定 | USB 裝置選擇、安裝 APK、設定 Device Owner、啟動 App；遇錯停止 |

普通安裝的預覽模式不能封鎖 Home 或多工。實體電源鍵、強制重啟、斷電均無法完全封鎖。本版不會自動開機啟動，重新開機後需手動開 App。未在實際 Galaxy Tab S12 / Windows 組合上驗證；請先按指南完成店頭驗收。

## 從原始碼建立

此分支的原始碼會建立 AutoFlip Simple Unslop UI 版。Android Studio 可直接開啟此專案。也可使用 JDK 17、Android SDK Platform 35 與 Build Tools 35.0.0：

```sh
export ANDROID_HOME=/path/to/android-sdk
bash build.sh
```

輸出為 `build/ScreenWake.apk`。原始簽署金鑰、密碼、本機 SDK、快取及私人專案封存均不在此 repository。自行首次重建會產生新的簽署身分，無法直接覆蓋已安裝的原版 APK；持續更新需使用擁有者私下保管的同一把金鑰。

Windows 腳本位於 [`windows-setup/`](windows-setup/)。Git clone 後先執行 `Download-PlatformTools.ps1` 取得 Google 官方工具，再開啟 `START_SETUP.cmd`。已有公司 MDM/Knox 管理時應交由 IT 部署；工具不會移除帳戶或恢復原廠設定。

## English

### AutoFlip Simple edition

| Edition | Download |
| --- | --- |
| **Unslop UI 1.0.5** with phone-friendly settings | [ScreenWake_v1_AutoFlip_Simple_unslopUI.apk](downloads/ScreenWake_v1_AutoFlip_Simple_unslopUI.apk) |
| **Original 1.0.4** with the original settings interface | [ScreenWake_v1_AutoFlip_Simple.apk](downloads/ScreenWake_v1_AutoFlip_Simple.apk) |

Both APKs share the same Android package. Installing Unslop UI over a same-signed earlier version keeps local settings. To return to 1.0.4, Android normally requires uninstalling 1.0.5 first, which clears those settings.

Enter your Heyzine HTTPS flipbook URL, enable **Heyzine Auto Flip**, and set a **2–300 second** interval. Tap **開始全螢幕展示** to start on Unslop UI or **預覽網頁** on the original. The last page returns to the beginning and playback continues. Android Back opens staff controls.

The Simple home screen puts Preview first and removes the Lock Display and Initial Setup buttons. It can update an earlier APK signed with the same key and retain saved settings. The source on this branch builds the Unslop UI edition. The standard APK and Windows setup instructions are linked above.


Screen Wake is a native Android browser for showroom tablets, with an Apple-inspired light interface, a configurable HTTPS URL, an awake screen and password-protected staff controls.

- Fullscreen preview hides the native top bar; Android Back opens staff authentication.
- The Heyzine reader toolbar, sound and magnification buttons are hidden while page navigation stays available.
- Real kiosk lockdown requires Android Device Owner provisioning or an existing EMM/DPC allowing Lock Task. Preview alone does not restrict Home, Recents or notifications.
- The Windows setup helper installs the APK, provisions Device Owner over authorized USB ADB and launches the app. It stops on errors and never deletes accounts or factory-resets a device.
- Signing uses the original private key for the included APK. Private keys and passwords are excluded from this repository. A locally rebuilt APK uses a new key unless the owner supplies the existing signing identity privately.
- Minimum Android 9, target SDK 35. Native Java, no additional application dependencies. Build with Android Studio or `bash build.sh` using JDK 17 and SDK 35.

**Validation:** APK build/signature, URL policy, password logic, Heyzine DOM selectors and mocked Windows setup flows have been checked. Physical tablet UI, Windows Forms, USB provisioning and vendor-specific kiosk behavior still require real-device testing.
