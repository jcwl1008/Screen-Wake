package hk.jimmy.screenwake;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.text.InputType;
import android.text.TextUtils;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    static final String BOOK = "https://heyzine.com/flip-book/597e09ddc4.html";
    private static final int INK = 0xFF202124;
    private static final int MUTED = 0xFF565C63;
    private static final int ERROR = 0xFF9B2424;
    private SharedPreferences prefs;
    private PasswordStore passwords;
    private DevicePolicyManager policy;
    private ComponentName admin;
    private boolean showing, locked, authOpen;
    private WebView web;
    private LinearLayout page;
    private TextView loadStatus;
    private Button retryLoad;
    private String displayUrl;
    private final Handler flipHandler = new Handler(Looper.getMainLooper());
    private final Runnable flipTick = this::runAutoFlip;
    private boolean resumed, pageLoaded, readerReady;
    private int flipGeneration;
    private static final int DEFAULT_FLIP_SECONDS = 8;
    private final ExecutorService crypto = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.paper));
        getWindow().setNavigationBarColor(getColor(R.color.paper));
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
    private Button button(String label, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : INK);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        int fill = primary ? getColor(R.color.accent) : Color.WHITE;
        int ripple = primary ? 0x33FFFFFF : 0x17202124;
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple), rounded(fill, 12), null));
        button.setStateListAnimator(null);
        button.setMinHeight(dp(52));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setOnClickListener(view -> action.run());
        return button;
    }
    private LinearLayout settingRow(int icon, String title, String summary, boolean shorten, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPaddingRelative(dp(16), dp(12), dp(16), dp(12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x17202124), null, rounded(Color.WHITE, 0)));
        row.setFocusable(true);
        row.setOnClickListener(view -> action.run());

        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(image, new LinearLayout.LayoutParams(dp(18), dp(18)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMarginStart(dp(12));
        row.addView(labels, params);
        add(labels, text(title, 16, INK, false), 0);
        TextView detail = text(summary, 14, MUTED, false);
        if (shorten) {
            detail.setMaxLines(1);
            detail.setEllipsize(TextUtils.TruncateAt.END);
        }
        add(labels, detail, 4);
        row.setContentDescription(title + "，" + summary);
        labels.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        return row;
    }
    private void add(LinearLayout parent, View v, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(top); parent.addView(v,lp);
    }
    private boolean owner() { return policy.isDeviceOwnerApp(getPackageName()); }
    private boolean permitted() { return owner() || policy.isLockTaskPermitted(getPackageName()); }
    private void dashboard() {
        showing = false;
        locked = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.paper));

        LinearLayout frame = new LinearLayout(this);
        frame.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        frame.setPadding(dp(20), dp(24), dp(20), dp(24));
        LinearLayout content = new LinearLayout(this) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int width = Math.min(View.MeasureSpec.getSize(widthSpec), dp(520));
                super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), heightSpec);
            }
        };
        content.setOrientation(LinearLayout.VERTICAL);
        frame.addView(content, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(frame);

        TextView title = text("Screen Wake", 24, INK, false);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setAccessibilityHeading(true);
        add(content, title, 0);
        add(content, text("全螢幕網頁展示", 14, MUTED, false), 4);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setBackground(rounded(Color.WHITE, 12));
        settings.setClipToOutline(true);
        String address = displayUrl.startsWith("https://") ? displayUrl.substring(8) : displayUrl;
        add(settings, settingRow(R.drawable.ic_link, "展示網址", address, true,
            () -> editSetting(() -> editUrl(this::dashboard))), 0);
        View divider = new View(this);
        divider.setBackgroundColor(0x18202124);
        LinearLayout.LayoutParams rule = new LinearLayout.LayoutParams(-1, dp(1));
        rule.setMarginStart(dp(46));
        settings.addView(divider, rule);
        add(settings, settingRow(R.drawable.ic_repeat, "自動翻頁", autoFlipSummary(), false,
            () -> editSetting(this::editAutoFlip)), 0);
        add(content, settings, 24);

        add(content, button("開始全螢幕展示", true, this::preparePreview), 24);
        add(content, text("展示期間保持螢幕常亮。用返回手勢開啟前線管理。", 14, MUTED, false), 8);
        if (!passwords.exists()) add(content, text("首次展示需要設定前線密碼。", 14, MUTED, false), 8);
        if (passwords.exists()) {
            add(content, button("前線管理", false, () -> authenticate(this::settingsMenu)), 20);
        }
        setContentView(scroll);
    }
    private void editSetting(Runnable action) {
        if (passwords.exists()) authenticate(action); else action.run();
    }
    private String autoFlipSummary() {
        if (!isHeyzine(displayUrl)) return "目前網址不支援自動翻頁";
        if (!prefs.getBoolean("auto_flip", false)) return "已關閉";
        return "每 " + flipSeconds() + " 秒翻頁 · 循環播放";
    }
    private void startPreview() {
        if (prefs.getBoolean("unslop_preview_notice_seen", false)) {
            startDisplay(false);
            return;
        }
        new AlertDialog.Builder(this).setTitle("全螢幕展示")
            .setMessage("展示期間會保持螢幕常亮。你仍可使用 Android 主畫面、多工及通知列。")
            .setNegativeButton("取消", null).setPositiveButton("開始展示", (dialog, which) -> {
                prefs.edit().putBoolean("unslop_preview_notice_seen", true).apply();
                startDisplay(false);
            }).show();
    }
    private void preparePreview() {
        if (passwords.exists()) startPreview();
        else newPassword(() -> {
            dashboard();
            startPreview();
        });
    }
    private void editUrl(Runnable next) {
        LinearLayout box=dialogContent();
        EditText input=new EditText(this); input.setSingleLine(true); input.setText(displayUrl);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSelectAllOnFocus(true);
        addField(box, "HTTPS 網址", input, 0);
        add(box, text("展示期間只允許開啟同一網站嘅頁面。", 14, MUTED, false), 8);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("展示網址").setView(scrollForm(box))
            .setNegativeButton("取消",null).setPositiveButton("儲存",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            String value=input.getText().toString().trim();
            try {
                String normalized = SitePolicy.normalize(value);
                if (!prefs.edit().putString("url", normalized).commit()) {
                    input.setError("未能儲存網址，請重試");
                    return;
                }
                displayUrl = normalized;
                dialog.dismiss();
                next.run();
            } catch (Exception ex) {
                input.setError("請輸入有效嘅 HTTPS 網址");
            }
        })); dialog.show();
    }
    private void addField(LinearLayout box, String label, EditText field, int top) {
        TextView name = text(label, 14, INK, false);
        field.setId(View.generateViewId());
        name.setLabelFor(field.getId());
        field.setTextSize(16);
        field.setMinHeight(dp(48));
        add(box, name, top);
        add(box, field, 4);
    }
    private ScrollView scrollForm(LinearLayout box) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        return scroll;
    }
    private EditText passwordInput() {
        EditText e = new EditText(this); e.setSingleLine(true);
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
        LinearLayout box = dialogContent();
        EditText first = passwordInput(), second = passwordInput();
        addField(box, "新密碼（最少 6 個字元）", first, 0);
        addField(box, "再次輸入密碼", second, 12);
        add(box, text("密碼只儲存在呢部裝置。", 14, MUTED, false), 8);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("設定前線密碼").setView(scrollForm(box))
            .setNegativeButton("取消",null).setPositiveButton("儲存",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String p=first.getText().toString();
            if (p.length() < 6) { first.setError("請輸入最少 6 個字元"); return; }
            if (!p.equals(second.getText().toString())) { second.setError("兩次輸入嘅密碼唔一致"); return; }
            char[] chars=p.toCharArray(); dialog.getButton(-1).setEnabled(false); dialog.setCancelable(false); dialog.getButton(-2).setEnabled(false);
            crypto.execute(() -> {
                boolean ok; try { passwords.save(chars); ok=true; } catch(Exception ex) { ok=false; } finally { Arrays.fill(chars,'\0'); }
                final boolean saved=ok; runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if(saved) { first.setText(""); second.setText(""); dialog.dismiss(); next.run(); }
                    else { second.setError("未能儲存密碼，請重試"); dialog.getButton(-1).setEnabled(true); dialog.getButton(-2).setEnabled(true); dialog.setCancelable(true); }
                });
            });
        })); dialog.show();
    }
    private void authenticate(Runnable next) {
        if(authOpen) return; authOpen=true;
        LinearLayout box = dialogContent();
        EditText input = passwordInput();
        addField(box, "前線密碼", input, 0);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("前線管理").setView(scrollForm(box))
            .setNegativeButton("取消",null).setPositiveButton("解鎖",null).create();
        dialog.setOnDismissListener(d -> { authOpen=false; input.setText(""); immersive(); });
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            long wait=passwords.waitSeconds(); if(wait>0) { input.setError("請等 " + wait + " 秒再試"); return; }
            char[] chars=input.getText().toString().toCharArray(); input.setText("");
            dialog.getButton(-1).setEnabled(false);
            crypto.execute(() -> {
                boolean result; try { result=passwords.check(chars); } catch(Exception ex) { result=false; } finally { Arrays.fill(chars,'\0'); }
                final boolean ok=result;
                runOnUiThread(() -> { if(isFinishing() || isDestroyed()) return;
                    dialog.getButton(-1).setEnabled(true);
                    if(ok) { dialog.dismiss(); next.run(); }
                    else input.setError(passwords.waitSeconds()>0 ? "請等 30 秒再試" : "密碼不正確");
                });
            });
        })); dialog.show();
    }
    private void startDisplay(boolean strict) {
        if(strict) {
            try {
                if(owner()) { policy.setLockTaskPackages(admin,new String[]{getPackageName()}); policy.setLockTaskFeatures(admin,DevicePolicyManager.LOCK_TASK_FEATURE_NONE); }
                if(!policy.isLockTaskPermitted(getPackageName())) {
                    new AlertDialog.Builder(this).setTitle("展示鎖定未就緒")
                        .setMessage("請聯絡 IT 檢查裝置管理設定，或使用預覽網頁。")
                        .setPositiveButton("知道了",null).show();
                    return;
                }
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
        loadStatus = text("正在載入網頁…", 14, MUTED, false);
        loadStatus.setPadding(dp(16), dp(8), dp(16), dp(8));
        loadStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        page.addView(loadStatus);
        retryLoad = button("重新載入", false, () -> web.loadUrl(displayUrl));
        retryLoad.setVisibility(View.GONE);
        page.addView(retryLoad);
        web=new WebView(this); configureWeb(); page.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page); immersive(); web.loadUrl(displayUrl);
    }
    private boolean allowed(Uri uri) {
        return SitePolicy.allows(displayUrl,uri.toString());
    }
    private boolean isHeyzine(String url) {
        if(url==null) return false;
        String host=Uri.parse(url).getHost();
        return host!=null && (host.equalsIgnoreCase("heyzine.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".heyzine.com"));
    }
    private int flipSeconds() { return prefs.getInt("auto_flip_seconds",DEFAULT_FLIP_SECONDS); }
    private void editAutoFlip() {
        LinearLayout box=dialogContent();
        Switch enabled=new Switch(this);
        enabled.setText("開啟自動翻頁");
        enabled.setChecked(prefs.getBoolean("auto_flip",false));
        enabled.setTextSize(16);
        enabled.setMinHeight(dp(48));
        boolean supported = isHeyzine(displayUrl);
        enabled.setEnabled(supported);
        add(box, enabled, 0);
        EditText seconds = new EditText(this);
        seconds.setSingleLine(true);
        seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        seconds.setText(Integer.toString(flipSeconds()));
        seconds.setSelectAllOnFocus(true);
        seconds.setEnabled(supported && enabled.isChecked());
        enabled.setOnCheckedChangeListener((view, checked) -> {
            seconds.setEnabled(checked);
            if (!checked) seconds.setError(null);
        });
        addField(box, "翻頁間隔（秒）", seconds, 12);
        String hint = supported ? "可設定 2–300 秒。到尾頁後會返回第一頁。"
            : "目前網址不支援自動翻頁。請先更改為 Heyzine 電子書網址。";
        add(box, text(hint, 14, MUTED, false), 8);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("自動翻頁")
            .setView(scrollForm(box)).setNegativeButton(supported ? "取消" : "返回", null);
        if (!supported) {
            builder.show();
            return;
        }
        AlertDialog dialog = builder.setPositiveButton("儲存", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            SharedPreferences.Editor settings = prefs.edit().putBoolean("auto_flip", enabled.isChecked());
            if (enabled.isChecked()) {
                int interval;
                try { interval = Integer.parseInt(seconds.getText().toString().trim()); }
                catch (NumberFormatException ex) { interval = 0; }
                if (interval < 2 || interval > 300) {
                    seconds.setError("請輸入 2–300 秒");
                    return;
                }
                settings.putInt("auto_flip_seconds", interval);
            }
            settings.apply();
            dialog.dismiss();
            if(showing) resetAutoFlip(); else dashboard();
        }));
        dialog.show();
    }
    private boolean canAutoFlip() {
        return resumed && showing && pageLoaded && hasWindowFocus() && web!=null
            && prefs.getBoolean("auto_flip",false) && isHeyzine(web.getUrl());
    }
    private void resetAutoFlip() {
        flipHandler.removeCallbacks(flipTick);
        flipGeneration++;
        readerReady=false;
        if(canAutoFlip()) flipHandler.post(flipTick);
    }
    private void runAutoFlip() {
        if(!canAutoFlip()) return;
        int generation=flipGeneration;
        web.evaluateJavascript(HeyzineAutoFlip.script(readerReady), result->{
            if(generation!=flipGeneration || !canAutoFlip()) return;
            readerReady="true".equals(result);
            flipHandler.postDelayed(flipTick,readerReady ? flipSeconds()*1000L : 1000L);
        });
    }
    private void cleanDisplayControls(WebView view,String url) {
        if(!isHeyzine(url)) return;
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
                pageLoaded=false;
                resetAutoFlip();
                if(!allowed(Uri.parse(url))) { v.stopLoading(); v.loadUrl(displayUrl); return; }
                retryLoad.setVisibility(View.GONE);
                loadStatus.setTextColor(MUTED);
                loadStatus.setText("正在載入網頁…");
                loadStatus.setVisibility(View.VISIBLE);
            }
            @Override public void onPageCommitVisible(WebView v,String url) { cleanDisplayControls(v,url); }
            @Override public void onPageFinished(WebView v,String url) {
                cleanDisplayControls(v,url);
                if(loadStatus.getText().toString().equals("正在載入網頁…")) {
                    loadStatus.setVisibility(View.GONE);
                    retryLoad.setVisibility(View.GONE);
                    pageLoaded=true;
                    resetAutoFlip();
                }
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
        pageLoaded=false;
        resetAutoFlip();
        loadStatus.setText("未能載入網頁，請檢查網絡。");
        loadStatus.setTextColor(ERROR);
        loadStatus.setVisibility(View.VISIBLE);
        retryLoad.setVisibility(View.VISIBLE);
    }
    private void settingsMenu() {
        String[] options = owner() ? new String[]{"更改前線密碼", "移除裝置管理"}
            : new String[]{"更改前線密碼"};
        new AlertDialog.Builder(this).setTitle("前線管理").setItems(options, (dialog, choice) -> {
            if (choice == 0) newPassword(this::dashboard);
            else confirmRemoveManagement();
        }).setNegativeButton("返回", null).show();
    }
    private void confirmRemoveManagement() {
        new AlertDialog.Builder(this).setTitle("移除裝置管理？")
            .setMessage("如要再次啟用展示鎖定，請由 IT 重新設定。")
            .setNegativeButton("取消", null).setPositiveButton("移除", (dialog, choice) -> {
                try {
                    policy.setLockTaskPackages(admin, new String[]{});
                    policy.clearDeviceOwnerApp(getPackageName());
                    dashboard();
                } catch (Exception ex) {
                    Toast.makeText(this, "未能移除，請聯絡 IT。", Toast.LENGTH_LONG).show();
                }
            }).show();
    }
    private void staffMenu() {
        LinearLayout box = dialogContent();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("前線管理")
            .setView(scrollForm(box)).setNegativeButton("返回展示", null).create();
        addMenuAction(box, dialog, "重新載入網頁", () -> web.loadUrl(displayUrl));
        addMenuAction(box, dialog, "更改展示網址", () -> editUrl(() -> {
            web.clearHistory();
            web.loadUrl(displayUrl);
        }));
        addMenuAction(box, dialog, "更改前線密碼", () -> newPassword(() ->
            Toast.makeText(this, "密碼已更新", Toast.LENGTH_SHORT).show()));
        addMenuAction(box, dialog, "自動翻頁", this::editAutoFlip);
        View divider = new View(this);
        divider.setBackgroundColor(0x18202124);
        LinearLayout.LayoutParams rule = new LinearLayout.LayoutParams(-1, dp(1));
        rule.topMargin = dp(8);
        box.addView(divider, rule);
        Button finish = addMenuAction(box, dialog, "結束展示", this::endDisplay);
        finish.setTextColor(ERROR);
        dialog.show();
    }
    private Button addMenuAction(LinearLayout box, AlertDialog dialog, String label, Runnable action) {
        Button item = button(label, false, () -> {
            dialog.dismiss();
            action.run();
        });
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        add(box, item, 0);
        return item;
    }
    private void endDisplay() {
        try { if(locked) stopLockTask(); }
        catch(Exception e) { Toast.makeText(this,"未能退出展示，請由 IT 檢查政策。",Toast.LENGTH_LONG).show(); return; }
        prefs.edit().putBoolean("session",false).commit();
        pageLoaded=false;
        resetAutoFlip();
        if(web!=null) { web.stopLoading(); web.destroy(); web=null; }
        dashboard();
    }
    private void immersive() {
        if(showing) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            |View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    @Override public void onBackPressed() { if(showing) authenticate(this::staffMenu); else super.onBackPressed(); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if(focus) immersive(); resetAutoFlip(); }
    @Override protected void onResume() { super.onResume(); resumed=true; if(web!=null) web.onResume(); immersive(); resetAutoFlip(); }
    @Override protected void onPause() { resumed=false; resetAutoFlip(); if(web!=null) web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { resumed=false; resetAutoFlip(); if(web!=null) web.destroy(); crypto.shutdown(); super.onDestroy(); }
}
