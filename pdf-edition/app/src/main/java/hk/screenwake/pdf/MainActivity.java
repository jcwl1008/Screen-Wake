package hk.screenwake.pdf;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import java.security.MessageDigest;

public class MainActivity extends Activity {
  private static final int PICK=77;
  private final Handler handler=new Handler();
  private PdfRenderer pdf;
  private ParcelFileDescriptor fd;
  private BookView book;
  private LinearLayout controls, root;
  private FrameLayout screen;
  private ScrollView scroll;
  private boolean fullscreen=false;
  private TextView status, intervalText, playButton, modeButton, lockButton;
  private SeekBar intervalBar;
  private TextView[] modeChoices=new TextView[3];
  private int seconds=8,page=0,mode=0;
  private boolean playing=false,locked=false,userReading=false;
  private static final long READING_IDLE_MS=60000L;
  private final Runnable resumeAfterReading=()->{if(playing && !locked && fullscreen && pdf!=null){userReading=false;timer();toast("已恢復自動翻頁");}};
  private final String[] modes={"滑動翻頁","淡入淡出","模擬立體翻頁"};
  private final Runnable tick=new Runnable(){public void run(){if(playing && !locked && !userReading && pdf!=null && fullscreen){book.advance();handler.postDelayed(this,seconds*1000L);}}};

  private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
  private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private Typeface englishFont(boolean bold){
    // Samsung Sharp Sans is proprietary. Load only if the licensed font assets are supplied.
    try{return Typeface.createFromAsset(getAssets(),bold?"fonts/SamsungSharpSans-Bold.ttf":"fonts/SamsungSharpSans-Regular.ttf");}
    catch(Exception missing){return Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL);}
  }
  private TextView text(String s,int size,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    if(s.matches(".*[A-Za-z].*"))t.setTypeface(englishFont(false));
    return t;
  }
  private void showChangelog(){
    new AlertDialog.Builder(this).setTitle("Screen Wake · 版本更新")
      .setMessage("v4.2.0｜介面及品牌更新\n• 採用 Screen Wake 風格應用程式圖示\n• 新增版本號碼及更新紀錄\n• 英文標題及字型支援 Samsung Sharp Sans（需提供授權字型）\n\nv4.1.0｜顧客閱讀模式\n• 觸碰螢幕暫停自動翻頁\n• 60 秒沒有操作後恢復\n\nv4.0.0｜Unslop UI\n• 繁體中文、簡潔設定介面\n• 三款翻頁動畫及全螢幕閱讀")
      .setPositiveButton("關閉",null).show();
  }
  private void addSpace(LinearLayout parent,int height){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
  private TextView action(LinearLayout parent,String label,boolean primary,Runnable click){
    TextView t=text(label,16,primary?Color.WHITE:0xff29313B);
    t.setGravity(Gravity.CENTER);t.setMinHeight(dp(52));t.setBackground(background(primary?0xff4263A3:0xffECEDEB,12));
    parent.addView(t,new LinearLayout.LayoutParams(-1,dp(54)));t.setOnClickListener(v->click.run());return t;
  }
  private void heading(LinearLayout parent,String s){TextView t=text(s,13,0xff637083);t.setTypeface(null,1);parent.addView(t);}
  private void updateStatus(){status.setText(pdf==null?"尚未選擇 PDF":pdf.getPageCount()+" 頁 · 第 "+(page+1)+" / "+pdf.getPageCount());}
  private void timer(){handler.removeCallbacks(tick);if(playing&&!locked&&!userReading&&pdf!=null&&fullscreen)handler.postDelayed(tick,seconds*1000L);}
  private void stopReadingPause(){userReading=false;handler.removeCallbacks(resumeAfterReading);}
  private void registerReaderTouch(){
    if(!fullscreen || !playing || locked || pdf==null)return;
    userReading=true;handler.removeCallbacks(tick);handler.removeCallbacks(resumeAfterReading);
    handler.postDelayed(resumeAfterReading,READING_IDLE_MS);
  }
  private String hash(String pin){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(("ScreenWake-v2:"+pin).getBytes("UTF-8"));StringBuilder sb=new StringBuilder();for(byte v:b)sb.append(String.format("%02x",v&255));return sb.toString();}catch(Exception e){return "";}}
  private String savedPin(){return getPreferences(0).getString("pin","");}
  private void lockNow(){if(savedPin().isEmpty()){setupPin();return;}locked=true;playing=false;stopReadingPause();playButton.setText("開始展示");timer();controls.setVisibility(View.GONE);book.setVisibility(View.GONE);showPinPrompt(true);}
  private void setupPin(){
    final EditText input=new EditText(this);input.setInputType(18);input.setHint("4–8 位數字密碼");input.setSingleLine(true);
    new AlertDialog.Builder(this).setTitle(savedPin().isEmpty()?"設定密碼":"更改密碼").setMessage("請設定 4 至 8 位數字密碼。請妥善保管，密碼無法復原。").setView(input).setNegativeButton("取消",null).setPositiveButton("儲存",(d,w)->{
      String pin=input.getText().toString();if(!pin.matches("[0-9]{4,8}")){toast("密碼必須為 4 至 8 位數字");return;}
      getPreferences(0).edit().putString("pin",hash(pin)).apply();lockButton.setText("鎖定閱讀器");toast("密碼已儲存");
    }).show();
  }
  private void showPinPrompt(boolean can離開){
    final EditText input=new EditText(this);input.setInputType(18);input.setHint("輸入密碼");
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("閱讀器已鎖定").setMessage("輸入密碼以解鎖 Screen Wake。").setView(input).setCancelable(false)
      .setPositiveButton("解鎖",null).setNegativeButton("離開",(d,w)->finish()).create();
    dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
      if(hash(input.getText().toString()).equals(savedPin())){locked=false;controls.setVisibility(View.VISIBLE);book.setVisibility(View.VISIBLE);dialog.dismiss();}
      else{input.setText("");input.setError("密碼錯誤");}
    }));dialog.show();
  }
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mode=Math.max(0,Math.min(2,getPreferences(0).getInt("mode",0)));seconds=getPreferences(0).getInt("interval",8);
    scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xffF7F7F5);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(28),dp(24),dp(28));scroll.addView(root);screen=new FrameLayout(this);screen.addView(scroll,new FrameLayout.LayoutParams(-1,-1));setContentView(screen);
    controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);root.addView(controls);
    TextView title=text("Screen Wake",28,0xff252B32);title.setTypeface(englishFont(true));controls.addView(title);
    TextView sub=text("PDF 自動翻頁",14,0xff68717A);controls.addView(sub);
    TextView version=text("版本 v4.2.0  ·  查看更新紀錄",12,0xff4263A3);
    version.setPadding(0,dp(10),0,dp(8));version.setContentDescription("版本 4.2.0，按此查看更新紀錄");
    version.setOnClickListener(v->showChangelog());controls.addView(version);addSpace(controls,12);
    heading(controls,"目前文件");addSpace(controls,12);
    action(controls,"選擇 PDF 文件",false,()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);});
    addSpace(controls,10);status=text("尚未選擇 PDF",14,0xff68717A);controls.addView(status);addSpace(controls,20);
    heading(controls,"翻頁間隔");addSpace(controls,12);
    intervalText=text("每隔 "+seconds+" 秒",18,0xff252B32);intervalText.setTypeface(null,1);controls.addView(intervalText);
    intervalBar=new SeekBar(this);intervalBar.setMax(298);intervalBar.setProgress(seconds-2);controls.addView(intervalBar);
    intervalBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar b,int value,boolean user){seconds=value+2;intervalText.setText("每隔 "+seconds+" 秒");if(user){getPreferences(0).edit().putInt("interval",seconds).apply();timer();}}
      public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
    });
    TextView hint=text("2 至 300 秒 · 最後一頁自動循環",12,0xff68717A);controls.addView(hint);addSpace(controls,24);
    heading(controls,"翻頁效果");addSpace(controls,12);
    LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.HORIZONTAL);controls.addView(choices);
    for(int i=0;i<3;i++){
      final int choice=i;
      TextView t=text(modes[i],13,0xff29313B);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);
      if(i>0)p.leftMargin=dp(8);
      choices.addView(t,p);modeChoices[i]=t;
      t.setOnClickListener(v->{mode=choice;getPreferences(0).edit().putInt("mode",mode).apply();updateModes();});
    }
    updateModes();
    addSpace(controls,24);
    playButton=action(controls,"開始展示",true,()->{
      if(pdf==null){toast("請先選擇 PDF 文件");return;}playing=!playing;stopReadingPause();playButton.setText(playing?"暫停展示":"開始展示");timer();
    });
    addSpace(controls,10);
    action(controls,"全螢幕預覽",false,()->{
      if(pdf==null){toast("請先選擇 PDF 文件");return;}
      enterFullscreen();
    });
    addSpace(controls,28);heading(controls,"安全設定");addSpace(controls,10);
    lockButton=action(controls,savedPin().isEmpty()?"設定密碼":"鎖定閱讀器",false,()->lockNow());
    TextView privacy=text("設定 4 至 8 位數字 PIN，防止其他人操作",12,0xff68717A);privacy.setPadding(0,dp(8),0,0);controls.addView(privacy);
    if(!savedPin().isEmpty()){addSpace(controls,10);action(controls,"更改密碼",false,()->{
      final EditText old=new EditText(this);old.setInputType(18);old.setHint("目前密碼");
      new AlertDialog.Builder(this).setTitle("驗證目前密碼").setView(old).setNegativeButton("取消",null).setPositiveButton("繼續",(d,w)->{
        if(hash(old.getText().toString()).equals(savedPin()))setupPin();else toast("密碼錯誤");
      }).show();
    });}
    book=new BookView(this);book.setBackground(background(0xffE9EAE7,12));
    LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(-1,dp(216));previewParams.bottomMargin=dp(24);
    root.addView(book,1,previewParams);
    book.setOnClickListener(v->{if(pdf!=null&&!locked&&!playing)book.advance();});
    if(!savedPin().isEmpty()){locked=true;controls.setVisibility(View.GONE);book.setVisibility(View.GONE);showPinPrompt(true);}
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null)return;
    try{
      Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
      if(pdf!=null)pdf.close();if(fd!=null)fd.close();
      fd=getContentResolver().openFileDescriptor(uri,"r");pdf=new PdfRenderer(fd);page=0;playing=false;stopReadingPause();playButton.setText("開始展示");book.clear();book.loadPage();updateStatus();timer();
    }catch(Exception e){new AlertDialog.Builder(this).setMessage("無法開啟 PDF："+e.getMessage()).setPositiveButton("OK",null).show();}
  }
  private Bitmap render(int n){
    if(pdf==null)return null;
    try(PdfRenderer.Page p=pdf.openPage(n)){
      int w=p.getWidth(),h=p.getHeight();float scale=Math.min(2f,1800f/Math.max(w,h));
      Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),Bitmap.Config.ARGB_8888);
      b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);return b;
    }catch(Exception e){return null;}
  }
  private void updateModes(){
    for(int i=0;i<modeChoices.length;i++){
      if(modeChoices[i]==null)continue;
      boolean selected=i==mode;
      modeChoices[i].setTextColor(selected?Color.WHITE:0xff29313B);
      modeChoices[i].setBackground(background(selected?0xff4263A3:0xffECEDEB,10));
    }
  }
  private void enterFullscreen(){
    if(fullscreen||pdf==null)return;
    fullscreen=true;root.removeView(book);scroll.setVisibility(View.GONE);
    book.setBackgroundColor(Color.BLACK);
    screen.addView(book,new FrameLayout.LayoutParams(-1,-1));
    getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
    book.invalidate();timer();
  }
  private void exitFullscreen(){
    if(!fullscreen)return;
    fullscreen=false;stopReadingPause();timer();screen.removeView(book);root.addView(book,1,new LinearLayout.LayoutParams(-1,dp(216)));
    book.setBackground(background(0xffE9EAE7,12));scroll.setVisibility(View.VISIBLE);
    getWindow().getDecorView().setSystemUiVisibility(0);book.invalidate();
  }
  @Override public void onBackPressed(){
    if(fullscreen&&!locked){exitFullscreen();}
    else super.onBackPressed();
  }
  @Override protected void onPause(){super.onPause();handler.removeCallbacks(tick);handler.removeCallbacks(resumeAfterReading);}
  @Override protected void onResume(){super.onResume();if(userReading){handler.removeCallbacks(resumeAfterReading);handler.postDelayed(resumeAfterReading,READING_IDLE_MS);}else timer();}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(book!=null)book.clear();if(pdf!=null)pdf.close();try{if(fd!=null)fd.close();}catch(Exception ignored){}super.onDestroy();}
  private class BookView extends View {
    private Bitmap current,next;private boolean animating=false;private long started;
    private final Paint paint=new Paint(3);
    BookView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
      if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){registerReaderTouch();}
      return super.onTouchEvent(event);
    }
    void clear(){if(current!=null&&!current.isRecycled())current.recycle();if(next!=null&&!next.isRecycled())next.recycle();current=null;next=null;animating=false;invalidate();}
    void loadPage(){if(current!=null&&!current.isRecycled())current.recycle();current=render(page);invalidate();}
    void advance(){
      if(pdf==null||animating)return;
      if(current==null)current=render(page);
      next=render((page+1)%pdf.getPageCount());if(next==null)return;
      animating=true;started=SystemClock.uptimeMillis();invalidate();
    }
    private void bitmap(Canvas canvas,Bitmap b,float x,float y,float w,float h){if(b!=null&&!b.isRecycled())canvas.drawBitmap(b,null,new RectF(x,y,x+w,y+h),paint);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);c.drawColor(fullscreen?Color.BLACK:0xffE9EAE7);
      if(current==null){paint.setColor(0xff8995a7);paint.setTextSize(dp(16));c.drawText("PDF 預覽",dp(20),getHeight()/2f,paint);return;}
      float scale=Math.min((float)getWidth()/current.getWidth(),(float)getHeight()/current.getHeight());
      float w=current.getWidth()*scale,h=current.getHeight()*scale,x=(getWidth()-w)/2,y=(getHeight()-h)/2;
      paint.setColor(Color.WHITE);paint.setAlpha(255);
      if(!animating){bitmap(c,current,x,y,w,h);return;}
      float t=Math.min(1f,(SystemClock.uptimeMillis()-started)/550f);
      float eased=t*t*(3-2*t);
      if(mode==0){
        bitmap(c,current,x-w*eased,y,w,h);bitmap(c,next,x+w*(1-eased),y,w,h);
      }else if(mode==1){
        bitmap(c,current,x,y,w,h);paint.setAlpha((int)(255*eased));bitmap(c,next,x,y,w,h);paint.setAlpha(255);
      }else{
        bitmap(c,next,x,y,w,h);
        c.save();c.clipRect(x,y,x+w*(1-eased),y+h);
        bitmap(c,current,x,y,w,h);c.restore();
        paint.setColor(0x33000000);c.drawRect(x+w*(1-eased)-dp(5),y,x+w*(1-eased),y+h,paint);paint.setColor(Color.WHITE);
      }
      if(t>=1f){if(current!=null)current.recycle();current=next;next=null;page=(page+1)%pdf.getPageCount();animating=false;updateStatus();}
      else postInvalidateDelayed(16);
    }
  }
}
