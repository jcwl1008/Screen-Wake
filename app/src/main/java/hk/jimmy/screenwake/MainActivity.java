package hk.jimmy.screenwake;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    static final String BOOK = "https://heyzine.com/flip-book/597e09ddc4.html";
    private static final int BLUE = Color.rgb(0,122,255), INK = Color.rgb(29,29,31), MUTED = Color.rgb(110,110,115), BG = Color.rgb(245,245,247);
    private SharedPreferences prefs;
    private PasswordStore passwords;
    private DevicePolicyManager policy;
    private ComponentName admin;
    private boolean showing, locked, authOpen;
    private WebView web;
    private LinearLayout page;
    private TextView loadStatus;
    private String displayUrl;
    private final ExecutorService crypto = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("screenwake_private", MODE_PRIVATE);
        passwords = new PasswordStore(prefs);
        displayUrl = prefs.getString("url", BOOK);
        policy = (DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);
        if (passwords.exists() && prefs.getBoolean("session", false) && permitted()) startDisplay(true);
        else dashboard();
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLineSpacing(dp(3), 1); return t;
    }
    private Button button(String s, boolean primary, Runnable action) {
        Button b = new Button(this); b.setText(s); b.setTextSize(16); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : BLUE); b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackground(rounded(primary ? BLUE : Color.rgb(235,243,255), 25));
        b.setPadding(dp(22),dp(9),dp(22),dp(9)); b.setMinHeight(dp(50));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void add(LinearLayout parent, View v, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(top); parent.addView(v,lp);
    }
    private boolean owner() { return policy.isDeviceOwnerApp(getPackageName()); }
    private boolean permitted() { return owner() || policy.isLockTaskPermitted(getPackageName()); }
    private void dashboard() {
        showing=false; locked=false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout frame = new LinearLayout(this); frame.setGravity(Gravity.CENTER); frame.setPadding(dp(24),dp(32),dp(24),dp(32));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(660));
        frame.addView(content,new LinearLayout.LayoutParams(width,-2)); scroll.addView(frame);
        add(content,text("SCREEN WAKE",13,BLUE,true),0);
        add(content,text("讓精彩，\n一直亮住。",42,INK,true),14);
        add(content,text("專為店頭而設的電子書展示空間。",17,MUTED,false),14);
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24),dp(24),dp(24),dp(24)); card.setBackground(rounded(Color.WHITE,24));
        add(card,text("展示內容",13,MUTED,false),0);
        add(card,text("你的網頁展示空間",25,INK,true),9);
        add(card,text(displayUrl,14,MUTED,false),6);
        add(card,button("更改展示網址",false,() -> {
            if(passwords.exists()) authenticate(() -> editUrl(this::dashboard)); else editUrl(this::dashboard);
        }),16);
        add(card,text("螢幕常亮  ·  橫直自適應  ·  密碼退出",15,INK,false),24);
        add(content,card,30);
        boolean ready = permitted();
        add(content,text(ready ? "●  展示鎖定已就緒" : "○  展示鎖定尚未設定",15,ready ? Color.rgb(25,130,75) : MUTED,true),24);
        add(content,text(ready ? "開始後會鎖定此 App；前線可用密碼結束展示。" : "預覽可保持常亮。要限制 Home、多工及通知列，請先完成一次性裝置管理設定。",14,MUTED,false),8);
        add(content,button(passwords.exists() ? "開始鎖定展示" : "設定密碼並開始",true,() -> ensurePassword(() -> {
            if (permitted()) startDisplay(true); else setupInfo();
        })),22);
        add(content,button("預覽網頁",false,() -> ensurePassword(() -> new AlertDialog.Builder(this)
            .setTitle("進入預覽模式")
            .setMessage("螢幕會保持常亮，但 Home、多工、通知列及電源鍵仍由系統控制。請勿將預覽當作防退出展示。")
            .setNegativeButton("取消",null).setPositiveButton("開始預覽",(d,w)->startDisplay(false)).show())),12);
        add(content,button("初始設定",false,this::setupInfo),12);
        if (passwords.exists()) add(content,button("更改前線密碼",false,() -> authenticate(() -> newPassword(this::dashboard))),12);
        if (owner()) add(content,button("移除此 App 的裝置管理",false,() -> authenticate(() ->
            new AlertDialog.Builder(this).setTitle("移除裝置管理？")
                .setMessage("移除後只能使用預覽模式。再次啟用展示鎖定需要由 IT 重新設定。")
                .setNegativeButton("取消",null).setPositiveButton("移除",(d,w)->{
                    try { policy.setLockTaskPackages(admin,new String[]{}); policy.clearDeviceOwnerApp(getPackageName()); dashboard(); }
                    catch(Exception e) { Toast.makeText(this,"未能移除，請聯絡 IT。",Toast.LENGTH_LONG).show(); }
                }).show())),12);
        add(content,text("Screen Wake v1  ·  Made for the showroom",12,MUTED,false),25);
        setContentView(scroll);
    }
    private void setupInfo() {
        String status=owner() ? "此 App 已是 Device Owner，可開始鎖定展示。" :
            policy.isLockTaskPermitted(getPackageName()) ? "管理平台已允許 Lock Task，可開始鎖定展示。" : "尚未取得展示鎖定權限。";
        LinearLayout box=dialogContent();
        TextView info=text(status+"\n\n1. 在 Windows 電腦下載並解壓 ScreenWake_v1_Windows_Setup.zip。\n2. 平板啟用 USB 偵錯，以 USB 線連接電腦，並允許偵錯授權。\n3. 電腦開啟 START_SETUP.cmd，選擇平板，再按『開始初始設定』。\n4. 完成後，返回此頁按『重新檢查』。\n\n平板 App 無法自行執行電腦 ADB 指令或授予自己 Device Owner。若已有公司 MDM／Knox 管理，請交由 IT 設定。若因帳戶或裝置狀態而失敗，工具會停止，不會刪除資料。\n\n實體電源鍵及強制重啟無法完全封鎖。",15,INK,false);
        add(box,info,0); ScrollView scroll=new ScrollView(this); scroll.addView(box);
        new AlertDialog.Builder(this).setTitle("初始設定 · Windows + USB").setView(scroll)
            .setNeutralButton("複製 ADB 指令",(d,w)->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Screen Wake setup","adb devices\nadb install -r ScreenWake_v1.apk\nadb shell dpm set-device-owner hk.jimmy.screenwake/.AdminReceiver"));
                Toast.makeText(this,"已複製，請在電腦執行",Toast.LENGTH_LONG).show();
            }).setNegativeButton("返回",null).setPositiveButton("重新檢查",(d,w)->{
                dashboard();
                new AlertDialog.Builder(this).setTitle(permitted() ? "已就緒" : "尚未完成設定")
                    .setMessage(permitted() ? "可按『開始鎖定展示』。" : "請先在電腦完成設定，或聯絡公司 IT。")
                    .setPositiveButton("知道了",null).show();
            }).show();
    }
    private void ensurePassword(Runnable next) { if(passwords.exists()) next.run(); else newPassword(next); }
    private void editUrl(Runnable next) {
        LinearLayout box=dialogContent();
        EditText input=new EditText(this); input.setSingleLine(true); input.setText(displayUrl);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSelectAllOnFocus(true); add(box,input,0);
        TextView error=text("請輸入 HTTPS 網址。展示期間只允許同一網站的頁面。",13,MUTED,false); add(box,error,12);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("展示網址").setView(box)
            .setNegativeButton("取消",null).setPositiveButton("儲存網址",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            String value=input.getText().toString().trim();
            try {
                String normalized=SitePolicy.normalize(value);
                if(!prefs.edit().putString("url",normalized).commit()) throw new Exception();
                displayUrl=normalized; dialog.dismiss(); next.run();
            } catch(Exception ex) { error.setText("請輸入有效的 HTTPS 網址，例如 https://example.com/page"); }
        })); dialog.show();
    }
    private EditText passwordInput(String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});
        e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        return e;
    }
    private LinearLayout dialogContent() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24),dp(8),dp(24),dp(8)); return box;
    }
    private void newPassword(Runnable next) {
        LinearLayout box=dialogContent(); EditText first=passwordInput("新密碼（最少 6 個字元）"), second=passwordInput("再次輸入密碼");
        add(box,first,0); add(box,second,12); TextView error=text("密碼只儲存在此裝置，請妥善保管。",13,MUTED,false); add(box,error,12);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("設定前線密碼").setView(box)
            .setNegativeButton("取消",null).setPositiveButton("儲存",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String p=first.getText().toString();
            if(p.length()<6 || !p.equals(second.getText().toString())) { error.setText("請輸入最少 6 個字元，並確保兩次密碼相同。"); return; }
            char[] chars=p.toCharArray(); dialog.getButton(-1).setEnabled(false); dialog.setCancelable(false); dialog.getButton(-2).setEnabled(false);
            crypto.execute(() -> {
                boolean ok; try { passwords.save(chars); ok=true; } catch(Exception ex) { ok=false; } finally { Arrays.fill(chars,'\0'); }
                final boolean saved=ok; runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if(saved) { first.setText(""); second.setText(""); dialog.dismiss(); next.run(); }
                    else { error.setText("未能儲存密碼，請重試。"); dialog.getButton(-1).setEnabled(true); dialog.getButton(-2).setEnabled(true); dialog.setCancelable(true); }
                });
            });
        })); dialog.show();
    }
    private void authenticate(Runnable next) {
        if(authOpen) return; authOpen=true;
        LinearLayout box=dialogContent(); EditText input=passwordInput("輸入前線密碼"); add(box,input,0);
        TextView error=text("只有前線人員可以管理展示。",13,MUTED,false); add(box,error,12);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("前線管理").setView(box)
            .setNegativeButton("返回展示",null).setPositiveButton("解鎖",null).create();
        dialog.setOnDismissListener(d -> { authOpen=false; input.setText(""); immersive(); });
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            long wait=passwords.waitSeconds(); if(wait>0) { error.setText("請稍候 " + wait + " 秒再試。"); return; }
            char[] chars=input.getText().toString().toCharArray(); input.setText("");
            dialog.getButton(-1).setEnabled(false);
            crypto.execute(() -> {
                boolean result; try { result=passwords.check(chars); } catch(Exception ex) { result=false; } finally { Arrays.fill(chars,'\0'); }
                final boolean ok=result;
                runOnUiThread(() -> { if(isFinishing() || isDestroyed()) return;
                    dialog.getButton(-1).setEnabled(true);
                    if(ok) { dialog.dismiss(); next.run(); }
                    else error.setText(passwords.waitSeconds()>0 ? "連續輸入錯誤，請 30 秒後再試。" : "密碼不正確，請重試。");
                });
            });
        })); dialog.show();
    }
    private void startDisplay(boolean strict) {
        if(strict) {
            try {
                if(owner()) { policy.setLockTaskPackages(admin,new String[]{getPackageName()}); policy.setLockTaskFeatures(admin,DevicePolicyManager.LOCK_TASK_FEATURE_NONE); }
                if(!policy.isLockTaskPermitted(getPackageName())) { setupInfo(); return; }
                startLockTask();
                ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
                if(am.getLockTaskModeState()!=ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    try { stopLockTask(); } catch(Exception ignored) {}
                    new AlertDialog.Builder(this).setTitle("未能啟用展示鎖定").setMessage("系統未進入 Lock Task。請由 IT 確認裝置設定。").setPositiveButton("知道了",null).show(); return;
                }
            } catch(Exception e) { new AlertDialog.Builder(this).setTitle("未能啟用展示鎖定").setMessage("請由 IT 確認 Device Owner 或 EMM 的 Lock Task 設定。").setPositiveButton("知道了",null).show(); return; }
        }
        showing=true; locked=strict; prefs.edit().putBoolean("session",strict).commit();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(Color.WHITE);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(18),dp(6),dp(12),dp(6));
        TextView label=text(strict ? "Screen Wake  ·  展示已鎖定" : "Screen Wake  ·  預覽模式",13,INK,true);
        bar.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button manage=button("前線管理",false,()->authenticate(this::staffMenu));
        manage.setTextSize(13); manage.setMinHeight(dp(42)); bar.addView(manage,new LinearLayout.LayoutParams(-2,dp(42)));
        if(strict) page.addView(bar);
        loadStatus=text("正在載入網頁…",12,MUTED,false); loadStatus.setPadding(dp(18),dp(4),dp(18),dp(4)); page.addView(loadStatus);
        web=new WebView(this); configureWeb(); page.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page); immersive(); web.loadUrl(displayUrl);
    }
    private boolean allowed(Uri uri) {
        return SitePolicy.allows(displayUrl,uri.toString());
    }
    private void cleanDisplayControls(WebView view,String url) {
        String host=Uri.parse(url).getHost();
        if(host==null || !(host.equalsIgnoreCase("heyzine.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".heyzine.com"))) return;
        // The flipbook's controls are part of the website, separate from Android's top bar.
        // Keep page navigation, gestures and page content intact. CSS also covers late-created controls.
        view.evaluateJavascript("(function(){var id='screenwake-clean-display';"
            +"if(document.getElementById(id))return;var s=document.createElement('style');s.id=id;"
            +"s.textContent='#pnlControls,#btnSoundOff,.zoom-icon,#pnlZoomStep,.btnZoomMore,.btnZoomLess{display:none!important;visibility:hidden!important;pointer-events:none!important}';"
            +"(document.head||document.documentElement).appendChild(s);})()",null);
    }
    private void configureWeb() {
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(true); s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSafeBrowsingEnabled(true); s.setMediaPlaybackRequiresUserGesture(true);
        s.setUseWideViewPort(true); s.setLoadWithOverviewMode(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setLongClickable(false); web.setOnLongClickListener(v->true);
        web.setDownloadListener((url,agent,disposition,mime,length)->Toast.makeText(this,"展示期間不開放下載",Toast.LENGTH_SHORT).show());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,Message msg) {
                Toast.makeText(MainActivity.this,"展示期間不開放外部連結",Toast.LENGTH_SHORT).show(); return false;
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params) { callback.onReceiveValue(null); return true; }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r) {
                if(r.isForMainFrame() && !allowed(r.getUrl()))
                    return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),
                        new java.io.ByteArrayInputStream("This website is outside the configured display.".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r) {
                if(!r.isForMainFrame()) return !"https".equalsIgnoreCase(r.getUrl().getScheme());
                if(allowed(r.getUrl())) return false;
                Toast.makeText(MainActivity.this,"展示期間只可瀏覽已設定的網站",Toast.LENGTH_SHORT).show(); return true;
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon) {
                if(!allowed(Uri.parse(url))) { v.stopLoading(); v.loadUrl(displayUrl); return; }
                loadStatus.setText("正在載入網頁…"); loadStatus.setVisibility(View.VISIBLE);
            }
            @Override public void onPageCommitVisible(WebView v,String url) { cleanDisplayControls(v,url); }
            @Override public void onPageFinished(WebView v,String url) {
                cleanDisplayControls(v,url);
                if(loadStatus.getText().toString().equals("正在載入網頁…")) loadStatus.setVisibility(View.GONE);
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e) { if(r.isForMainFrame()) loadError(); }
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e) { if(r.isForMainFrame()) loadError(); }
            @Override public void onReceivedSslError(WebView v,SslErrorHandler handler,SslError error) { handler.cancel(); loadError(); }
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail) {
                page.removeView(v); v.destroy(); web=new WebView(MainActivity.this); configureWeb();
                page.addView(web,new LinearLayout.LayoutParams(-1,0,1)); loadError(); return true;
            }
        });
    }
    private void loadError() {
        loadStatus.setText("未能載入網頁。請確認網絡後，點此重新載入。"); loadStatus.setVisibility(View.VISIBLE);
        loadStatus.setTextColor(BLUE); loadStatus.setMinHeight(dp(48));
        loadStatus.setOnClickListener(v->{ loadStatus.setOnClickListener(null); web.loadUrl(displayUrl); });
    }
    private void staffMenu() {
        new AlertDialog.Builder(this).setTitle("前線管理").setItems(new String[]{"重新載入網頁","更改展示網址","更改密碼","結束展示"},(d,w)->{
            if(w==0) web.loadUrl(displayUrl);
            else if(w==1) editUrl(()->{ web.clearHistory(); web.loadUrl(displayUrl); });
            else if(w==2) newPassword(()->Toast.makeText(this,"密碼已更新",Toast.LENGTH_SHORT).show());
            else endDisplay();
        }).setNegativeButton("返回展示",null).show();
    }
    private void endDisplay() {
        try { if(locked) stopLockTask(); }
        catch(Exception e) { Toast.makeText(this,"未能退出展示，請由 IT 檢查政策。",Toast.LENGTH_LONG).show(); return; }
        prefs.edit().putBoolean("session",false).commit();
        if(web!=null) { web.stopLoading(); web.destroy(); web=null; }
        dashboard();
    }
    private void immersive() {
        if(showing) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            |View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    @Override public void onBackPressed() { if(showing) authenticate(this::staffMenu); else super.onBackPressed(); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if(focus) immersive(); }
    @Override protected void onResume() { super.onResume(); if(web!=null) web.onResume(); immersive(); }
    @Override protected void onPause() { if(web!=null) web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { if(web!=null) web.destroy(); crypto.shutdown(); super.onDestroy(); }
}
