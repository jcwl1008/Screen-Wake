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
  private int 秒=8,page=0,mode=0;
  private boolean playing=false,locked=false;
  private final String[] modes={"滑動翻頁","淡入淡出","模擬立體翻頁"};
  private final Runnable tick=new Runnable(){public void run(){if(playing && !locked && pdf!=null){book.advance();handler.postDelayed(this,seconds*1000L);}}};

  private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
  private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
  private void addSpace(LinearLayout parent,int height){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
  private TextView action(LinearLayout parent,String label,boolean primary,Runnable click){
    TextView t=text(label,16,primary?Color.WHITE:0xff202a3b);
    t.setGravity(Gravity.CENTER);t.setMinHeight(dp(54));t.setBackground(background(primary?0xff2858d7:0xffe9edf4,14));
    parent.addView(t,new LinearLayout.LayoutParams(-1,dp(54)));t.setOnClickListener(v->click.run());return t;
  }
  private void heading(LinearLayout parent,String s){TextView t=text(s,13,0xff637083);t.setLetterSpacing(0.07f);parent.addView(t);}
  private void updateStatus(){status.setText(pdf==null?"尚未選擇 PDF":pdf.getPageCount()+" 頁 · 第 "+(page+1)+" / "+pdf.getPageCount());}
  private void timer(){handler.removeCallbacks(tick);if(playing&&!locked&&pdf!=null)handler.postDelayed(tick,seconds*1000L);}
  private String hash(String pin){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(("ScreenWake-v2:"+pin).getBytes("UTF-8"));StringBuilder sb=new StringBuilder();for(byte v:b)sb.append(String.format("%02x",v&255));return sb.toString();}catch(Exception e){return "";}}
  private String savedPin(){return getPreferences(0).getString("pin","");}
  private void lockNow(){if(savedPin().isEmpty()){setupPin();return;}locked=true;playing=false;playButton.setText("開始自動翻頁");timer();controls.setVisibility(View.GONE);book.setVisibility(View.GONE);showPinPrompt(true);}
  private void setupPin(){
    final EditText input=new EditText(this);input.setInputType(18);input.setHint("4–8 位數字密碼");input.setSingleLine(true);
    new AlertDialog.Builder(this).setTitle(savedPin().isEmpty()?"設定密碼":"更改密碼").setMessage("Use a 4–8 位數字密碼 to protect the reader. Keep it safe; there is no PIN recovery.").setView(input).setNegativeButton("取消",null).setPositiveButton("儲存",(d,w)->{
      String pin=input.getText().toString();if(!pin.matches("[0-9]{4,8}")){toast("密碼必須為 4 至 8 位數字");return;}
      getPreferences(0).edit().putString("pin",hash(pin)).apply();lockButton.setText("鎖定閱讀器");toast("密碼已儲存");
    }).show();
  }
  private void showPinPrompt(boolean can離開){
    final EditText input=new EditText(this);input.setInputType(18);input.setHint("輸入密碼");
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("閱讀器已鎖定").setMessage("輸入密碼以解鎖 Screen Wake。").setView(input).set取消able(false)
      .setPositiveButton("解鎖",null).setNegativeButton("離開",(d,w)->finish()).create();
    dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
      if(hash(input.getText().toString()).equals(savedPin())){locked=false;controls.setVisibility(View.VISIBLE);book.setVisibility(View.VISIBLE);dialog.dismiss();}
      else{input.setText("");input.setError("密碼錯誤");}
    }));dialog.show();
  }
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mode=getPreferences(0).getInt("mode",0);seconds=getPreferences(0).getInt("interval",8);
    scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff8f9fc);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(20));scroll.addView(root);screen=new FrameLayout(this);screen.addView(scroll,new FrameLayout.LayoutParams(-1,-1));setContentView(screen);
    controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);root.addView(controls);
    TextView title=text("Screen Wake",29,0xff152238);title.setTypeface(null,1);controls.addView(title);
    TextView sub=text("PDF 閱讀器 · v3",14,0xff6a7587);controls.addView(sub);addSpace(controls,26);
    heading(controls,"PDF 文件");addSpace(controls,10);
    action(controls,"選擇 PDF",true,()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);});
    addSpace(controls,10);status=text("尚未選擇 PDF",14,0xff647084);controls.addView(status);addSpace(controls,24);
    heading(controls,"自動翻頁");addSpace(controls,12);
    intervalText=text("每隔 "+seconds+" 秒",17,0xff1b2940);intervalText.setTypeface(null,1);controls.addView(intervalText);
    intervalBar=new SeekBar(this);intervalBar.setMax(298);intervalBar.setProgress(seconds-2);controls.addView(intervalBar);
    intervalBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar b,int value,boolean user){seconds=value+2;intervalText.setText("每隔 "+seconds+" 秒");if(user){getPreferences(0).edit().putInt("interval",seconds).apply();timer();}}
      public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
    });
    TextView hint=text("每頁間隔可設定 2 至 300 秒",12,0xff788395);controls.addView(hint);addSpace(controls,22);
    heading(controls,"翻頁效果");addSpace(controls,10);
    modeButton=action(controls,modes[mode]+" · 選擇效果",false,()->{
      new AlertDialog.Builder(this).setTitle("選擇翻頁效果").setSingleChoiceItems(modes,mode,(d,which)->{
        mode=which;getPreferences(0).edit().putInt("mode",mode).apply();modeButton.setText(modes[mode]+" · 選擇效果");d.dismiss();
      }).setNegativeButton("取消",null).show();
    });
    addSpace(controls,24);
    playButton=action(controls,"開始自動翻頁",true,()->{
      if(pdf==null){toast("Choose a PDF first");return;}playing=!playing;playButton.setText(playing?"暫停自動翻頁":"開始自動翻頁");timer();
    });
    addSpace(controls,10);
    action(controls,"全螢幕閱讀",false,()->{
      if(pdf==null){toast("Choose a PDF first");return;}
      enterFullscreen();
    });
    addSpace(controls,26);heading(controls,"密碼保護");addSpace(controls,10);
    lockButton=action(controls,savedPin().isEmpty()?"設定密碼":"鎖定閱讀器",false,()->lockNow());
    TextView privacy=text("以數字密碼保護閱讀器",12,0xff788395);privacy.setPadding(0,dp(8),0,0);controls.addView(privacy);
    if(!savedPin().isEmpty()){addSpace(controls,10);action(controls,"更改密碼",false,()->{
      final EditText old=new EditText(this);old.setInputType(18);old.setHint("目前密碼");
      new AlertDialog.Builder(this).setTitle("驗證目前密碼").setView(old).setNegativeButton("取消",null).setPositiveButton("繼續",(d,w)->{
        if(hash(old.getText().toString()).equals(savedPin()))setupPin();else toast("密碼錯誤");
      }).show();
    });}
    addSpace(root,24);
    book=new BookView(this);book.setBackground(background(0xffe8ecf3,16));
    root.addView(book,new LinearLayout.LayoutParams(-1,dp(280)));
    book.setOnClickListener(v->{if(pdf!=null&&!locked)book.advance();});
    if(!savedPin().isEmpty()){locked=true;controls.setVisibility(View.GONE);book.setVisibility(View.GONE);showPinPrompt(true);}
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null)return;
    try{
      Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
      if(pdf!=null)pdf.close();if(fd!=null)fd.close();
      fd=getContentResolver().openFileDescriptor(uri,"r");pdf=new PdfRenderer(fd);page=0;playing=false;playButton.setText("開始自動翻頁");book.clear();book.loadPage();updateStatus();timer();
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
  @Override public void onBackPressed(){
    if(fullscreen&&!locked){exitFullscreen();}
    else super.onBackPressed();
  }
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(book!=null)book.clear();if(pdf!=null)pdf.close();try{if(fd!=null)fd.close();}catch(Exception ignored){}super.onDestroy();}
  private class BookView extends View {
    private Bitmap current,next;private boolean animating=false;private long started;
    private final Paint paint=new Paint(3);
    BookView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
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
      super.onDraw(c);c.drawColor(fullscreen?Color.BLACK:0xffe8ecf3);
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
