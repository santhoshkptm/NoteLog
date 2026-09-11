package com.example.simplejournal;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    private static final String LOG_PLACEHOLDER="[YYYY-MM-DD HH:MM] - ";
    private EditText editor; private String type,name; private Uri uri; private boolean changing,dirty;
    private final ArrayList<String> undo=new ArrayList<>(), redo=new ArrayList<>(); private boolean historyReady;
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);} private boolean dark(){return (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;} private int fg(){return dark()?Color.WHITE:Color.rgb(30,30,30);} private int bg(){return dark()?Color.rgb(30,31,36):Color.rgb(250,250,250);}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);uri=Uri.parse(getIntent().getStringExtra("uri"));name=getIntent().getStringExtra("name");type=getIntent().getStringExtra("type");if(type==null)type="Plain";Storage.setType(this,name,type);build();}
    private void insets(View v){v.setOnApplyWindowInsetsListener((view,i)->{int top=0,bottom=0;if(Build.VERSION.SDK_INT>=30){top=i.getInsets(WindowInsets.Type.statusBars()).top;bottom=i.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=i.getSystemWindowInsetTop();bottom=i.getSystemWindowInsetBottom();}view.setPadding(dp(12),dp(8)+top,dp(12),dp(8)+bottom);return i;});v.requestApplyInsets();}
    private TextView label(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextColor(fg());v.setTextSize(z);return v;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());setContentView(root);insets(root);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("‹");back.setTextSize(26);back.setContentDescription("Back");bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView title=label(name,18);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(0,dp(48),1);tl.leftMargin=dp(8);bar.addView(title,tl);
        Button save=new Button(this);save.setText("Save");bar.addView(save,new LinearLayout.LayoutParams(dp(78),dp(48)));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(52)));
        back.setOnClickListener(v->{save();finish();});save.setOnClickListener(v->save());
        editor=new EditText(this);editor.setTextColor(fg());editor.setHintTextColor(dark()?0xFF909098:0xFF777777);editor.setBackgroundColor(bg());editor.setGravity(Gravity.TOP|Gravity.START);editor.setTextSize(17);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);editor.setPadding(dp(8),dp(10),dp(8),dp(10));
        String loaded=Storage.read(this,uri);if(type.equals("Log")){if(loaded.isEmpty())loaded=LOG_PLACEHOLDER;else if(!loaded.endsWith("\n")&&!loaded.endsWith(LOG_PLACEHOLDER))loaded+="\n"+LOG_PLACEHOLDER;else if(loaded.endsWith("\n")&&!loaded.endsWith(LOG_PLACEHOLDER+"\n"))loaded+=LOG_PLACEHOLDER;}
        // Plain mode is intentionally untouched: no bullets, prefixes, or other formatting are injected.
        editor.setText(loaded);editor.setSelection(editor.length());root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);TextView mode=label(type,13);mode.setPadding(dp(8),0,dp(8),0);tools.addView(mode,new LinearLayout.LayoutParams(0,dp(48),1));Button u=new Button(this);u.setText("Undo");tools.addView(u,new LinearLayout.LayoutParams(dp(68),dp(48)));Button r=new Button(this);r.setText("Redo");tools.addView(r,new LinearLayout.LayoutParams(dp(68),dp(48)));Button f=new Button(this);f.setText("Find");tools.addView(f,new LinearLayout.LayoutParams(dp(68),dp(48)));root.addView(tools,new LinearLayout.LayoutParams(-1,dp(56)));u.setOnClickListener(v->undo());r.setOnClickListener(v->redo());f.setOnClickListener(v->findText());
        undo.add(editor.getText().toString());historyReady=true;
        editor.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){if(changing)return;dirty=true;editor.post(()->applyAutomaticFormatting());editor.post(EditorActivity.this::recordHistory);}public void afterTextChanged(Editable e){}});
        if(type.equals("Check-Box"))editor.setOnTouchListener((v,event)->{if(event.getAction()!=MotionEvent.ACTION_UP||editor.getLayout()==null)return false;int off=editor.getOffsetForPosition(event.getX(),event.getY());int line=editor.getLayout().getLineForOffset(Math.max(0,Math.min(off,editor.length())));int ls=editor.getLayout().getLineStart(line);if(ls+1<editor.length()){char ch=editor.getText().charAt(ls);if((ch=='☐'||ch=='☑')&&off<=ls+2){changing=true;editor.getText().replace(ls,ls+1,String.valueOf(ch=='☐'?'☑':'☐'));changing=false;dirty=true;recordHistory();return true;}}return false;});
        editor.requestFocus();editor.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT),180);
    }
    private void applyAutomaticFormatting(){if(changing)return;Editable e=editor.getText();if(type.equals("Plain"))return;if(type.equals("Log")){formatLog(e);return;}String prefix=type.equals("Check-Box")?"☐ ":(type.equals("Notes")||type.equals("Bullets"))?"• ":null;if(prefix!=null)formatBullets(e,prefix);}
    private int lineStart(Editable e,int p){p=Math.max(0,Math.min(p,e.length()));int n=e.toString().lastIndexOf('\n',Math.max(0,p-1));return n<0?0:n+1;}
    private void formatBullets(Editable e,String prefix){
        String all=e.toString();int cursor=editor.getSelectionStart();int pos=0;
        while(pos<all.length()){int nl=all.indexOf('\n',pos);if(nl<0)nl=all.length();if(nl==pos){pos=nl+1;continue;}pos=nl+1;if(pos>=all.length())break;int ls=pos;int end=all.indexOf('\n',ls);if(end<0)end=all.length();String line=all.substring(ls,end);if(!line.startsWith(prefix)&&!line.trim().isEmpty()){changing=true;e.insert(ls,prefix);changing=false;if(cursor>=ls)cursor+=prefix.length();all=e.toString();}}
        if(e.length()==0||e.toString().endsWith("\n")){changing=true;e.append(prefix);changing=false;cursor=e.length();}
        // Empty bullet followed by Enter exits bullet mode rather than producing endless bullets.
        String s=e.toString();if(s.endsWith("\n"+prefix+"\n")){changing=true;e.delete(e.length()-prefix.length()-1,e.length());changing=false;cursor=e.length();}
        editor.setSelection(Math.max(0,Math.min(cursor,e.length())));
    }
    private void formatLog(Editable e){
        String s=e.toString();int cursor=editor.getSelectionStart();
        // Stamp a placeholder only after the user has typed something into it. The typed text remains intact.
        int searchFrom=0;while(true){int ls=s.indexOf(LOG_PLACEHOLDER,searchFrom);if(ls<0)break;int contentStart=ls+LOG_PLACEHOLDER.length();int nextNl=s.indexOf('\n',contentStart);if(nextNl<0)nextNl=s.length();if(contentStart<nextNl){String now=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());String replacement=now+" - ";changing=true;e.replace(ls,contentStart,replacement);changing=false;int delta=replacement.length()-LOG_PLACEHOLDER.length();if(cursor>=contentStart)cursor+=delta;s=e.toString();}searchFrom=contentStart+1;}
        // Enter on the placeholder itself must not create another placeholder.
        if(s.endsWith(LOG_PLACEHOLDER+"\n")){
            changing=true; e.delete(e.length()-1,e.length()); changing=false; cursor=e.length(); s=e.toString();
        } else if(!s.endsWith(LOG_PLACEHOLDER) && s.endsWith("\n")){
            changing=true; e.append(LOG_PLACEHOLDER); changing=false; cursor=e.length();
        }
        editor.setSelection(Math.max(0,Math.min(cursor,e.length())));
    }
    private void recordHistory(){if(!historyReady||changing)return;String s=editor.getText().toString();if(undo.isEmpty()||!undo.get(undo.size()-1).equals(s)){undo.add(s);if(undo.size()>100)undo.remove(0);redo.clear();}}
    private void undo(){if(undo.size()<=1)return;String cur=undo.remove(undo.size()-1);redo.add(cur);changing=true;String prev=undo.get(undo.size()-1);editor.setText(prev);editor.setSelection(prev.length());changing=false;dirty=true;}
    private void redo(){if(redo.isEmpty())return;String n=redo.remove(redo.size()-1);undo.add(n);changing=true;editor.setText(n);editor.setSelection(n.length());changing=false;dirty=true;}
    private void findText(){final EditText q=new EditText(this);q.setHint("Find in this note");q.setSingleLine(true);new AlertDialog.Builder(this).setTitle("Find").setView(q).setNegativeButton("Close",null).setPositiveButton("Find next",(d,w)->{String needle=q.getText().toString();if(needle.isEmpty())return;String all=editor.getText().toString().toLowerCase(Locale.ROOT),target=needle.toLowerCase(Locale.ROOT);int from=Math.max(0,editor.getSelectionStart());int p=all.indexOf(target,from);if(p<0)p=all.indexOf(target);if(p>=0){editor.requestFocus();editor.setSelection(p,p+needle.length());}else Toast.makeText(this,"Not found",Toast.LENGTH_SHORT).show();}).show();}
    private String textForSave(){String s=editor.getText().toString();if(type.equals("Log")){while(s.endsWith(LOG_PLACEHOLDER))s=s.substring(0,s.length()-LOG_PLACEHOLDER.length());while(s.endsWith("\n"))s=s.substring(0,s.length()-1);}return s;}
    private void save(){try{String out=textForSave();String existing=Storage.read(this,uri);if(!out.equals(existing))Storage.write(this,uri,out);dirty=false;}catch(Exception e){Toast.makeText(this,"Save failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    @Override protected void onPause(){super.onPause();if(dirty)save();}
}
