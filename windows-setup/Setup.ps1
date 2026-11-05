$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$script:Busy = $false
$script:Devices = @()
$script:Adb = Join-Path $PSScriptRoot 'platform-tools\adb.exe'
$script:Apk = Join-Path $PSScriptRoot 'ScreenWake_v1.apk'

$form = New-Object System.Windows.Forms.Form
$form.Text = 'Screen Wake - Initial Setup'
$form.ClientSize = New-Object System.Drawing.Size(740, 660)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'FixedDialog'
$form.MaximizeBox = $false
$form.BackColor = [System.Drawing.Color]::FromArgb(245,245,247)
$form.Font = New-Object System.Drawing.Font('Microsoft JhengHei',10)

$heading = New-Object System.Windows.Forms.Label
$heading.Text = 'Screen Wake'
$heading.Font = New-Object System.Drawing.Font('Segoe UI',28,[System.Drawing.FontStyle]::Bold)
$heading.Location = New-Object System.Drawing.Point(28,20)
$heading.Size = New-Object System.Drawing.Size(680,55)
$form.Controls.Add($heading)

$note = New-Object System.Windows.Forms.Label
$note.Text = "初始設定 · Windows + USB`r`n先開啟平板 USB 偵錯、連接 USB 線，並在平板允許此電腦的偵錯授權。`r`n請只為你獲授權管理的展示平板執行；已有公司 MDM 時請交由 IT 處理。"
$note.Location = New-Object System.Drawing.Point(30,85)
$note.Size = New-Object System.Drawing.Size(680,85)
$form.Controls.Add($note)

$picker = New-Object System.Windows.Forms.ComboBox
$picker.DropDownStyle = 'DropDownList'
$picker.Location = New-Object System.Drawing.Point(30,178)
$picker.Size = New-Object System.Drawing.Size(480,30)
$form.Controls.Add($picker)

$refresh = New-Object System.Windows.Forms.Button
$refresh.Text = '重新偵測平板'
$refresh.Location = New-Object System.Drawing.Point(525,174)
$refresh.Size = New-Object System.Drawing.Size(180,38)
$refresh.FlatStyle = 'Flat'
$refresh.BackColor = [System.Drawing.Color]::White
$form.Controls.Add($refresh)

$start = New-Object System.Windows.Forms.Button
$start.Text = '開始初始設定'
$start.Location = New-Object System.Drawing.Point(30,232)
$start.Size = New-Object System.Drawing.Size(675,48)
$start.FlatStyle = 'Flat'
$start.FlatAppearance.BorderSize = 0
$start.BackColor = [System.Drawing.Color]::FromArgb(0,122,255)
$start.ForeColor = [System.Drawing.Color]::White
$start.Font = New-Object System.Drawing.Font('Microsoft JhengHei',12,[System.Drawing.FontStyle]::Bold)
$start.Enabled = $false
$form.Controls.Add($start)

$log = New-Object System.Windows.Forms.TextBox
$log.Multiline = $true
$log.ReadOnly = $true
$log.ScrollBars = 'Vertical'
$log.BackColor = [System.Drawing.Color]::White
$log.Location = New-Object System.Drawing.Point(30,300)
$log.Size = New-Object System.Drawing.Size(675,290)
$log.Font = New-Object System.Drawing.Font('Consolas',10)
$form.Controls.Add($log)

$footer = New-Object System.Windows.Forms.Label
$footer.Text = '不會清除帳戶、不會恢復原廠設定。設定成功仍需在 App 設定前線密碼。'
$footer.Location = New-Object System.Drawing.Point(30,610)
$footer.Size = New-Object System.Drawing.Size(680,30)
$form.Controls.Add($footer)

function Write-Log([string] $Text) {
    $log.AppendText($Text + "`r`n")
    [System.Windows.Forms.Application]::DoEvents()
}

function Invoke-Adb([string[]] $AdbArgs) {
    # Return stdout/stderr separately from process success. dpm can report failure with exit code 0.
    $oldPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& $script:Adb @AdbArgs 2>&1)
        $exitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $oldPreference }
    return [PSCustomObject]@{ Code = $exitCode; Text = ($output | ForEach-Object { $_.ToString() }) -join "`r`n" }
}

function Refresh-Devices {
    $picker.Items.Clear()
    $script:Devices = @()
    $start.Enabled = $false
    if (-not (Test-Path -LiteralPath $script:Adb)) { throw '找不到 adb.exe。請完整解壓 ZIP，保留 platform-tools 資料夾。' }
    if (-not (Test-Path -LiteralPath $script:Apk)) { throw '找不到 ScreenWake_v1.apk。請重新解壓完整設定套件。' }
    $r = Invoke-Adb -AdbArgs @('devices','-l')
    Write-Log $r.Text
    if ($r.Code -ne 0) { throw 'ADB 偵測失敗。請檢查 USB 驅動程式。' }
    foreach ($line in ($r.Text -split "`r?`n")) {
        if ($line -match '^([^\s]+)\s+device(?:\s|$)') {
            $serial = $Matches[1]
            if ($serial -notmatch '^emulator-' -and $serial -notmatch '[:.]') {
                $script:Devices += $serial
                [void]$picker.Items.Add($line.Trim())
            }
        }
    }
    if ($script:Devices.Count -eq 1) { $picker.SelectedIndex = 0 }
    if ($script:Devices.Count -eq 0) {
        Write-Log '未找到已授權的 USB 平板。請解鎖平板並允許 USB 偵錯。若一直無法偵測，請安裝 Samsung Android USB Driver。'
    } elseif ($script:Devices.Count -gt 1) {
        Write-Log '偵測到多部裝置。請在上方清單明確選擇要設定的平板。'
    }
}

$picker.Add_SelectedIndexChanged({ $start.Enabled = (-not $script:Busy -and $picker.SelectedIndex -ge 0) })
$refresh.Add_Click({
    try { Refresh-Devices } catch { Write-Log ('錯誤：' + $_.Exception.Message) }
})
$start.Add_Click({
    if ($script:Busy -or $picker.SelectedIndex -lt 0) { return }
    $script:Busy = $true
    $start.Enabled = $false
    $refresh.Enabled = $false
    $picker.Enabled = $false
    try {
        $serial = $script:Devices[$picker.SelectedIndex]
        $r = Invoke-Adb -AdbArgs @('-s',$serial,'get-state')
        if ($r.Code -ne 0 -or $r.Text.Trim() -ne 'device') { throw '所選平板已斷線或未授權。請重新偵測。' }
        Write-Log '1/3 安裝或更新 ScreenWake_v1.apk…'
        $r = Invoke-Adb -AdbArgs @('-s',$serial,'install','-r',$script:Apk)
        Write-Log $r.Text
        if ($r.Code -ne 0 -or $r.Text -notmatch '(?m)^Success\s*$') { throw '安裝未成功，已停止。未更改裝置擁有者。' }
        Write-Log '2/3 設定 Device Owner…'
        $r = Invoke-Adb -AdbArgs @('-s',$serial,'shell','dpm','set-device-owner','hk.jimmy.screenwake/.AdminReceiver')
        Write-Log $r.Text
        if ($r.Code -ne 0 -or $r.Text -notmatch 'Success: Device owner set to') {
            # An existing owner is left unchanged. The app can check its own status without guessing from dumpsys output.
            [void](Invoke-Adb -AdbArgs @('-s',$serial,'shell','am','start','-n','hk.jimmy.screenwake/.MainActivity'))
            throw '系統未確認 Device Owner 設定成功。若已設定過，請在 App 按「初始設定 → 重新檢查」。否則請將上方錯誤交予 IT；本工具不會移除帳戶或重設裝置。'
        }
        Write-Log '3/3 開啟 Screen Wake…'
        $r = Invoke-Adb -AdbArgs @('-s',$serial,'shell','am','start','-n','hk.jimmy.screenwake/.MainActivity')
        Write-Log $r.Text
        if ($r.Code -ne 0 -or $r.Text -match '(?im)^Error') { throw 'Device Owner 已設定，但未能自動開啟 App。請在平板手動開啟 Screen Wake。' }
        Write-Log '初始設定成功。平板按「初始設定 → 重新檢查」，再設定網址及前線密碼，開始鎖定展示。'
        Write-Log '店頭驗收後，請關閉 USB 偵錯並撤銷偵錯授權。'
        [void][System.Windows.Forms.MessageBox]::Show($form,'初始設定成功。請在平板設定前線密碼及網址，再開始展示。','Screen Wake')
    } catch {
        Write-Log ('已停止：' + $_.Exception.Message)
    } finally {
        $script:Busy = $false
        $refresh.Enabled = $true
        $picker.Enabled = $true
        $start.Enabled = ($picker.SelectedIndex -ge 0)
    }
})
$form.Add_FormClosing({ if ($script:Busy) { $_.Cancel = $true } })
$form.Add_Shown({ try { Refresh-Devices } catch { Write-Log ('錯誤：' + $_.Exception.Message) } })
[void]$form.ShowDialog()
