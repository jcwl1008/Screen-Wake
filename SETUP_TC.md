# Screen Wake v1

為 Android 平板而設的獨立展示瀏覽器。白色與淺灰介面、圓角卡片、藍色按鈕；前線可自行輸入展示網址。Android 9 或以上。

## 安裝及開始

1. 將 ScreenWake_v1.apk 下載至 Tab，點選安裝。按系統提示，只為下載來源暫時允許安裝未知應用程式；安裝後可以關閉該權限。
2. 開啟「Screen Wake」，點「更改展示網址」，貼上 HTTPS 網址並儲存。只輸入網域時會自動補上 https://。
3. 點「設定密碼並開始」，設定至少 6 個字元的前線密碼。沒有通用預設密碼或後門密碼。
4. 未完成裝置管理時，可點「預覽網頁」測試排版、觸控、翻頁及網絡；預覽會保持螢幕常亮，但不能限制系統 Home、多工、通知列。
5. 完成下節裝置管理設定後，點「開始鎖定展示」。App 會確認系統真的進入 Lock Task 才標示「展示已鎖定」。
6. 顧客可瀏覽網頁；預覽模式不顯示 App 頂部工具列，請用系統返回手勢／返回鍵開啟密碼驗證。鎖定展示模式可點右上角「前線管理」，或用返回鍵驗證密碼。輸入密碼後，可重新載入、更改網址、更改密碼或結束展示。

設定密碼後，首頁更改網址同樣需要驗證。網址及密碼驗證資料只儲存在此裝置。密碼以隨機 salt 配合 PBKDF2-HMAC-SHA256、210,000 次運算儲存；連續錯誤 5 次會暫停驗證 30 秒。

## 兩種模式

| 行為 | 一般預覽 | 受管理的展示鎖定 |
| --- | --- | --- |
| App 在前景時避免自動熄屏 | 支援 | 支援 |
| 網頁自適應橫直方向 | 支援 | 支援 |
| 更改網址／App 內退出需要密碼 | 支援 | 支援 |
| 限制 Home、多工、通知列 | 不支援 | 由 Android Lock Task 實施 |
| 阻止實體電源鍵熄屏／強制重啟 | 不支援 | 無法完全封鎖 |
| 裝置重新開機後自動啟動 | 不支援 | 本版未加入自動啟動；需手動開 App |

App 結束展示後會解除自身常亮要求，恢復系統一般螢幕逾時行為。常亮不是「永不關機」，裝置仍可能因電源鍵、斷電、系統故障等原因停止顯示。

## Windows 一鍵初始設定

1. 在 Windows 電腦下載 `ScreenWake_v1_Windows_Setup.zip`，完整解壓，保留所有資料夾。不要直接在 ZIP 內開啟程式。
2. 在平板開啟 USB 偵錯，連接 USB 線，解鎖平板並允許電腦偵錯授權。
3. 雙擊 `START_SETUP.cmd`。工具已附 Google 官方 Android Platform Tools，不需要另行安裝 Android Studio。
4. 如偵測到多部平板，先在清單選擇目標，再按「開始初始設定」。工具會安裝／更新同一簽署的 APK，執行 Device Owner 設定，成功後開啟 App。
5. 返回 App 點「初始設定 → 重新檢查」，顯示就緒後即可開始鎖定展示。App 裏的「複製 ADB 指令」只會複製文字，並不會在平板自行執行 ADB。

工具使用 Windows PowerShell 及 Windows Forms；啟動器的 ExecutionPolicy Bypass 只套用於該次 PowerShell 程序，不會更改電腦永久設定。若公司端點政策阻擋腳本，請交由 IT 執行，勿嘗試停用公司保護。若找不到裝置，請由 IT 安裝 Samsung Android USB Driver。

工具不會清除帳戶、清除 App 資料、移除其他管理員或恢復原廠設定。Device Owner 設定若被系統拒絕，會顯示原因並停止；如已設定過，請直接在 App 重新檢查狀態。正式使用前須在實際 Windows 電腦及 Tab 上驗收。

## IT：啟用真正的展示鎖定

### 方案 A：公司已有 EMM／MDM／Knox 管理

請由 IT 在既有管理平台部署 APK，並允許套件 `hk.jimmy.screenwake` 使用 Android Lock Task／單一 App 展示模式。若平台需要以自身 kiosk launcher 啟動，可由 IT 配置其等效鎖定政策。不要嘗試用下方 Device Owner 指令取代公司現有管理。

App 能辨識由外部 DPC 設定的 Lock Task 允許清單；Home、多工、通知等實際限制由公司 DPC 政策控制，需要 IT 驗收。

### 方案 B：獨立店頭展示平板，由此 App 擔任 Device Owner

由 IT 使用 Android Platform Tools，經 USB 連接平板並完成 USB 偵錯授權。此方式通常要求尚未配置帳戶的裝置，且沒有其他 Device Owner。普通「啟用裝置管理員」或「固定 App」不等於 Device Owner。

```sh
adb devices
adb install -r ScreenWake_v1.apk
adb shell dpm set-device-owner hk.jimmy.screenwake/.AdminReceiver
```

必須看見指令成功才算完成。重新開啟 Screen Wake，設定密碼及網址，點「開始鎖定展示」。App 會將自身加入 Lock Task 允許清單並將 Lock Task 功能設為 NONE。

如果出現已有帳戶、已有擁有者、裝置已配置等錯誤，請停止並交由 IT 判斷部署方式。不要為了繞過錯誤而直接重設使用中的平板。如 IT 決定需要恢復原廠設定，必須先完成資料備份及公司授權；恢復原廠設定會刪除資料。此套件不包含自動重設裝置腳本。

店頭驗收完成後，由 IT 關閉 USB 偵錯並撤銷電腦偵錯授權，避免顧客透過受信任電腦繞過 App 控制。

### 解除此 App 的 Device Owner

先用前線密碼結束展示。回到首頁，點「移除此 App 的裝置管理」，再次驗證密碼並確認。之後會恢復為預覽功能，若需再次鎖定須由 IT 重新配置。若由公司 EMM 管理，解除管理必須在公司平台進行。

請由公司保管前線密碼。忘記密碼無法透過預設碼解鎖；須聯絡 IT，按管理政策復原。未受管理的預覽安裝可重新安裝或清除資料，但網址及密碼設定會一併刪除。

## 網址及相容性

- 預設已填入 Jimmy 提供的 Heyzine 電子書： https://heyzine.com/flip-book/597e09ddc4.html 。前線可更換，並於下次開啟時保留。
- 僅接受有效的 HTTPS 網址。展示期間主頁面只允許同一個網域名稱及通訊埠的導覽；圖片、腳本及內嵌內容可使用其他來源，讓常見網站及 Heyzine CDN 正常載入。
- 例如 `example.com` 和 `www.example.com` 被視為不同網站。若網站會重新導向到另一個網域，請直接輸入最後的正式網址。
- 新視窗、外部 App 連結、檔案下載／上載、相機／麥克風及定位授權均不開放。網頁本身的互動翻頁仍可使用。
- 此版本使用系統 Android WebView，並非獨立 Chrome。部分需要第三方 Cookie、Google 登入、彈出視窗、DRM 或外部付款流程的網站可能不相容。安裝前線需要的網站應逐一測試。
- 網頁需要網絡，未提供完整離線下載。斷線可點頁面提示重試；若網頁內的內容一直載入中，前線可輸入密碼後重新載入。
- 網頁不能繞過原有登入權限、付費牆或網站使用限制。

## 店頭驗收

1. 在實際 Tab S12、目前系統及 Android System WebView 上，測試所需網站的開啟、翻頁、橫直切換及網絡恢復。
2. 展示超過平板原有自動熄屏時間，確認螢幕仍亮。
3. 啟用鎖定後，測試 Home、返回、多工與通知列。返回應要求密碼；Home／多工及通知列應由管理政策限制。
4. 輸入錯誤密碼，確認不能管理或退出；連續錯誤 5 次會暫停 30 秒。
5. 用正確密碼更改網址、重載及結束展示，確認回到首頁並恢復一般操作。
6. 重新開機後，手動開啟 App；如上次仍是鎖定展示，App 會嘗試恢復該次展示。這不代表開機自動啟動。

## 原始碼及重建

原始碼為 Java 原生 Android，沒有額外 App 依賴。可用 Android Studio 開啟 ScreenWake 資料夾。Gradle 使用 Android Gradle Plugin 8.9.2、compile/target SDK 35、min SDK 28。

也可在 Linux／macOS 使用 JDK 17、Android SDK Platform 35 與 Build Tools 35.0.0 執行：

```sh
export ANDROID_HOME=/path/to/android-sdk
bash build.sh
```

輸出為 `build/ScreenWake.apk`。GitHub repository 不包含私人簽署金鑰或密碼。首次自行重建會產生新的簽署身分，不能覆蓋已安裝的原版 APK；要保留更新相容性，請由擁有者私下提供原有簽署資料，並放在本機 `signing/` 資料夾。不要將金鑰、密碼或含簽署資料的私人封存提交至 Git。

Android 官方參考：

- https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
- https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- https://developer.android.com/work/dpc/dedicated-devices
