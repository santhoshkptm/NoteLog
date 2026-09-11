package com.example.simplejournal;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout content,list,lockedOverlay;
    private EditText search;
    private boolean newest=true, authenticating=false, unlocked=false, waitingForRetry=false;
    private static final int REQ_FOLDER=700, REQ_ENABLE_LOCK=98, REQ_UNLOCK=99;
    private static final String PREFS="prefs", THEME="theme";

    @Override public void onCreate(Bundle b){super.onCreate(b);configureWindow();build();}
    @Override protected void onResume(){super.onResume();boolean lock=getSharedPreferences(PREFS,0).getBoolean("lock",false);if(lock&&!unlocked&&!authenticating&&!waitingForRetry)authenticate();else if(unlocked||!lock)refresh();}
    private void configureWindow(){getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private String themePref(){return getSharedPreferences(PREFS,0).getString(THEME,"system");}
    private boolean dark(){String t=themePref();if(t.equals("dark"))return true;if(t.equals("light"))return false;return (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    private int fg(){return dark()?Color.WHITE:Color.rgb(30,30,30);} private int bg(){return dark()?Color.rgb(30,31,36):Color.rgb(250,250,250);} private int secondary(){return dark()?0xFFB8B8C0:0xFF666666;} private int surface(){return dark()?0xFF3A3B42:0xFFECECEC;}
    private void applyInsets(View v){final int l=dp(16),r=dp(16);v.setOnApplyWindowInsetsListener((view,insets)->{int top=0,bottom=0;if(Build.VERSION.SDK_INT>=30){top=insets.getInsets(WindowInsets.Type.statusBars()).top;bottom=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}view.setPadding(l,top+dp(10),r,bottom+dp(10));return insets;});v.requestApplyInsets();}
    private TextView label(String text,float size){TextView v=new TextView(this);v.setText(text);v.setTextColor(fg());v.setTextSize(size);return v;}
    private Button plainButton(String text,String description){Button b=new Button(this);b.setText(text);b.setContentDescription(description);return b;}

    private void build(){
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(bg());setContentView(frame);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);applyInsets(content);frame.addView(content,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("NoteLog",26);title.setTypeface(null,1);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        Button menu=plainButton("☰","Open settings menu");menu.setTextSize(22);header.addView(menu,new LinearLayout.LayoutParams(dp(52),dp(48)));content.addView(header,new LinearLayout.LayoutParams(-1,dp(52)));

        search=new EditText(this);search.setHint("Search notes…");search.setHintTextColor(secondary());search.setTextColor(fg());search.setSingleLine(true);search.setTextSize(16);search.setPadding(dp(14),0,dp(14),0);search.setBackgroundColor(surface());content.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
        Space gap=new Space(this);content.addView(gap,new LinearLayout.LayoutParams(1,dp(12)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh();}public void afterTextChanged(Editable e){}});

        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.addView(list,new ScrollView.LayoutParams(-1,-2));content.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        Button add=plainButton("+","Create new note");add.setTextSize(30);add.setGravity(Gravity.CENTER);add.setElevation(dp(8));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(68),dp(68),Gravity.BOTTOM|Gravity.END);fp.setMargins(0,0,dp(20),dp(24));frame.addView(add,fp);add.setOnClickListener(v->newNote());
        menu.setOnClickListener(v->settingsMenu());
        refresh();if(Storage.hasFolder(this))Storage.ensureWelcomeFile(this);if(!Storage.hasFolder(this))showFolderRequired();
    }

    private void settingsMenu(){
        final String lock=getSharedPreferences(PREFS,0).getBoolean("lock",false)?"🔓 Unlock/Lock app":"🔒 Lock the app";
        final String theme=themePref().equals("dark")?"☀ Theme: Light":themePref().equals("light")?"🌙 Theme: Dark":"◐ Theme: System";
        final String[] items={"📁 Folder select",lock,"↕ Sort notes",theme,"ⓘ About me"};
        new AlertDialog.Builder(this).setTitle("Menu").setItems(items,(d,w)->{switch(w){case 0:chooseFolder();break;case 1:securityDialog();break;case 2:sortDialog();break;case 3:themeDialog();break;case 4:about();break;}}).show();
    }
    private void themeDialog(){
        String[] options={"System default","Light","Dark"};String current=themePref();int checked=current.equals("light")?1:current.equals("dark")?2:0;
        new AlertDialog.Builder(this).setTitle("Theme").setSingleChoiceItems(options,checked,(d,w)->{getSharedPreferences(PREFS,0).edit().putString(THEME,w==1?"light":w==2?"dark":"system").apply();d.dismiss();recreate();}).show();
    }
    private void about(){
        Intent i=new Intent(this,AboutActivity.class);startActivity(i);
    }

    private void showFolderRequired(){new AlertDialog.Builder(this).setTitle("Choose a storage folder").setMessage("NoteLog stores your notes as ordinary .txt files in a folder you choose. The files remain outside the app and can survive app data clearing or uninstalling.").setNegativeButton("Later",null).setPositiveButton("Choose folder",(d,w)->chooseFolder()).show();}
    private void chooseFolder(){startActivityForResult(Storage.folderIntent(),REQ_FOLDER);}
    private void refresh(){if(list==null)return;list.removeAllViews();String q=search==null?"":search.getText().toString();if(!Storage.hasFolder(this)){TextView e=label("No storage folder selected.\nUse ☰ → Folder select.",16);e.setGravity(Gravity.CENTER);e.setPadding(dp(12),dp(40),dp(12),dp(12));list.addView(e,new LinearLayout.LayoutParams(-1,-2));return;}List<Storage.Item> items=Storage.list(this,q,newest);for(Storage.Item it:items)addRow(it);if(items.isEmpty()){TextView e=label(q.trim().isEmpty()?"No notes yet.\nTap + to create one.":"No matching notes.",16);e.setGravity(Gravity.CENTER);e.setPadding(dp(12),dp(40),dp(12),dp(12));list.addView(e,new LinearLayout.LayoutParams(-1,-2));}}
    private void addRow(Storage.Item it){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(8),dp(8),dp(8));row.setMinimumHeight(dp(60));
        TextView name=label(it.name,16);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);row.addView(name,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView date=label(Storage.date(it.date),13);date.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);date.setSingleLine(true);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(136),dp(44));dl.leftMargin=dp(8);row.addView(date,dl);
        row.setOnClickListener(v->open(it));row.setOnLongClickListener(v->{fileMenu(it);return true;});list.addView(row,new LinearLayout.LayoutParams(-1,-2));View line=new View(this);line.setBackgroundColor(dark()?0xFF55565C:0xFFD0D0D0);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private void newNote(){
        final String[] types={"Notes","Log","Check-Box","Bullets","Plain"};
        new AlertDialog.Builder(this).setTitle("Choose note type").setItems(types,(d,which)->askFilename(types[which])).show();
    }
    private void askFilename(final String type){
        final EditText n=new EditText(this);
        n.setHint("Filename");
        n.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("File name").setView(n).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->createNote(type,n.getText().toString())).show();
    }
    private void createNote(String type,String name){
        if(name.trim().isEmpty()){Toast.makeText(this,"Enter a filename",Toast.LENGTH_SHORT).show();return;}
        String initial=type.equals("Log")?"":type.equals("Check-Box")?"☐ ":(type.equals("Notes")||type.equals("Bullets"))?"• ":"";
        try{
            Uri u=Storage.create(this,name,initial);
            Storage.setType(this,Storage.normalize(name),type);
            Intent i=new Intent(this,EditorActivity.class);
            i.putExtra("uri",u.toString());
            i.putExtra("name",Storage.normalize(name));
            i.putExtra("type",type);
            startActivity(i);
        }catch(Exception e){Toast.makeText(this,"Could not create file: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void open(Storage.Item it){Intent i=new Intent(this,EditorActivity.class);i.putExtra("uri",it.uri.toString());i.putExtra("name",it.name);i.putExtra("type",Storage.getType(this,it.name,it.uri));startActivity(i);}
    private void fileMenu(Storage.Item it){new AlertDialog.Builder(this).setItems(new String[]{"Rename","Delete"},(d,w)->{if(w==0)rename(it);else confirmDelete(it);}).show();}
    private void rename(Storage.Item it){EditText e=new EditText(this);e.setSingleLine(true);e.setText(it.name.replaceFirst("(?i)\\.txt$",""));new AlertDialog.Builder(this).setTitle("Rename").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{try{String nn=Storage.normalize(e.getText().toString());if(nn.equalsIgnoreCase(it.name))return;Storage.rename(this,it.uri,nn);Storage.renameType(this,it.name,nn);refresh();}catch(Exception x){Toast.makeText(this,x.getMessage(),Toast.LENGTH_LONG).show();}}).show();}
    private void confirmDelete(Storage.Item it){new AlertDialog.Builder(this).setTitle("Delete note?").setMessage(it.name).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{try{Storage.delete(this,it.uri);Storage.removeType(this,it.name);refresh();}catch(Exception x){Toast.makeText(this,x.getMessage(),Toast.LENGTH_LONG).show();}}).show();}
    private void sortDialog(){new AlertDialog.Builder(this).setTitle("Sort notes").setItems(new String[]{"Last modified (newest first)","Name (A–Z)"},(d,w)->{newest=w==0;refresh();}).show();}

    private void securityDialog(){
        final android.content.SharedPreferences prefs=getSharedPreferences(PREFS,0);final boolean on=prefs.getBoolean("lock",false);
        if(!on){new AlertDialog.Builder(this).setTitle("App security").setMessage("Use your phone's PIN, password, pattern or biometric authentication when opening NoteLog?").setNegativeButton("Cancel",null).setPositiveButton("Enable",(d,w)->enableSecurityAfterAuthentication()).show();}
        else new AlertDialog.Builder(this).setTitle("App security").setMessage("App lock is enabled. Disable it?").setNegativeButton("Cancel",null).setPositiveButton("Disable",(d,w)->{prefs.edit().putBoolean("lock",false).apply();unlocked=true;waitingForRetry=false;Toast.makeText(this,"App lock disabled",Toast.LENGTH_SHORT).show();}).show();
    }
    private void enableSecurityAfterAuthentication(){authenticating=true;waitingForRetry=false;authenticate(true);}
    private void authenticate(){authenticate(false);}
    private void authenticate(final boolean enabling){
        authenticating=true;waitingForRetry=false;
        try{
            if(Build.VERSION.SDK_INT>=30){android.hardware.biometrics.BiometricPrompt bp=new android.hardware.biometrics.BiometricPrompt.Builder(this).setTitle(enabling?"Enable NoteLog security":"Unlock NoteLog").setSubtitle("Use your device security").setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG|android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build();bp.authenticate(new android.os.CancellationSignal(),getMainExecutor(),callback(enabling));return;}
            if(Build.VERSION.SDK_INT>=29){android.hardware.biometrics.BiometricPrompt bp=new android.hardware.biometrics.BiometricPrompt.Builder(this).setTitle(enabling?"Enable NoteLog security":"Unlock NoteLog").setSubtitle("Use your device security").setDeviceCredentialAllowed(true).build();bp.authenticate(new android.os.CancellationSignal(),getMainExecutor(),callback(enabling));return;}
            android.app.KeyguardManager km=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);Intent i=km==null?null:km.createConfirmDeviceCredentialIntent(enabling?"Enable NoteLog security":"Unlock NoteLog","Use your screen lock to continue");if(i==null){authenticating=false;if(enabling){Toast.makeText(this,"Unable to start device security.",Toast.LENGTH_LONG).show();}else{waitingForRetry=true;showLockOverlay();}return;}startActivityForResult(i,enabling?REQ_ENABLE_LOCK:REQ_UNLOCK);
        }catch(Throwable ex){authenticating=false;if(enabling){waitingForRetry=false;Toast.makeText(this,"Could not start security on this device. No changes were made.",Toast.LENGTH_LONG).show();}else{waitingForRetry=true;showLockOverlay();}}
    }
    private android.hardware.biometrics.BiometricPrompt.AuthenticationCallback callback(final boolean enabling){return new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(android.hardware.biometrics.BiometricPrompt.AuthenticationResult r){authenticating=false;unlocked=true;waitingForRetry=false;if(enabling){getSharedPreferences(PREFS,0).edit().putBoolean("lock",true).apply();Toast.makeText(MainActivity.this,"App lock enabled",Toast.LENGTH_SHORT).show();}refresh();}@Override public void onAuthenticationError(int e,CharSequence s){authenticating=false;if(enabling){waitingForRetry=false;Toast.makeText(MainActivity.this,"Security was not enabled.",Toast.LENGTH_SHORT).show();}else{waitingForRetry=true;showLockOverlay();}}};}
    private void showLockOverlay(){if(lockedOverlay!=null)return;lockedOverlay=new LinearLayout(this);lockedOverlay.setOrientation(LinearLayout.VERTICAL);lockedOverlay.setGravity(Gravity.CENTER);lockedOverlay.setBackgroundColor(bg());TextView t=label("NoteLog is locked",24);t.setGravity(Gravity.CENTER);lockedOverlay.addView(t,new LinearLayout.LayoutParams(-1,dp(60)));TextView s=label("Unlock with your device security",16);s.setGravity(Gravity.CENTER);lockedOverlay.addView(s,new LinearLayout.LayoutParams(-1,dp(50)));Button retry=plainButton("Unlock","Unlock NoteLog");retry.setOnClickListener(v->{if(lockedOverlay!=null&&lockedOverlay.getParent()!=null)((ViewGroup)lockedOverlay.getParent()).removeView(lockedOverlay);lockedOverlay=null;authenticate(false);});lockedOverlay.addView(retry,new LinearLayout.LayoutParams(dp(140),dp(52)));addContentView(lockedOverlay,new ViewGroup.LayoutParams(-1,-1));}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==REQ_FOLDER){if(c==RESULT_OK&&d!=null&&d.getData()!=null){Storage.persistFolder(this,d.getData(),d.getFlags());Storage.ensureWelcomeFile(this);refresh();Toast.makeText(this,"Storage folder selected",Toast.LENGTH_SHORT).show();}return;}if(r==REQ_ENABLE_LOCK){authenticating=false;if(c==RESULT_OK){getSharedPreferences(PREFS,0).edit().putBoolean("lock",true).apply();unlocked=true;waitingForRetry=false;Toast.makeText(this,"App lock enabled",Toast.LENGTH_SHORT).show();refresh();}else{unlocked=true;waitingForRetry=false;Toast.makeText(this,"Security was not enabled.",Toast.LENGTH_SHORT).show();}}else if(r==REQ_UNLOCK){authenticating=false;if(c==RESULT_OK){unlocked=true;waitingForRetry=false;refresh();}else{unlocked=false;waitingForRetry=true;showLockOverlay();}}}
}
