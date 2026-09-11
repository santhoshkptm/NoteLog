package com.example.simplejournal;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

public class AboutActivity extends Activity {
    // ===== ABOUT PAGE CONTENT =====
    // Edit ONLY the values in this section to personalize the About page.
    private static final String ABOUT_NAME="Santhosh";
    private static final String ABOUT_MESSAGE="NoteLog is a small, private, local-first notes and logs app. Your notes are kept as ordinary text files in a folder you choose, so they remain yours and stay easy to access.";
    private static final String BUY_ME_A_COFFEE_URL="https://www.buymeacoffee.com/";
    private static final String GITHUB_PROFILE_URL="https://github.com/";
    private static final String GITHUB_PROJECT_URL="https://github.com/";
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private boolean dark(){String t=getSharedPreferences("prefs",0).getString("theme","system");if(t.equals("dark"))return true;if(t.equals("light"))return false;return (getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
    private int fg(){return dark()?Color.WHITE:Color.rgb(30,30,30);} private int bg(){return dark()?Color.rgb(30,31,36):Color.rgb(250,250,250);}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);build();}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg());setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->{int top=0,bottom=0;if(Build.VERSION.SDK_INT>=30){top=insets.getInsets(WindowInsets.Type.statusBars()).top;bottom=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}view.setPadding(dp(20),top+dp(16),dp(20),bottom+dp(20));return insets;});root.requestApplyInsets();
        TextView title=new TextView(this);title.setText("About NoteLog");title.setTextSize(26);title.setTypeface(null,1);title.setTextColor(fg());root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView name=new TextView(this);name.setText(ABOUT_NAME);name.setTextSize(19);name.setTextColor(fg());name.setPadding(0,dp(8),0,dp(8));root.addView(name);
        TextView thanks=new TextView(this);thanks.setText("Thank you for using NoteLog.\n\n"+ABOUT_MESSAGE);thanks.setTextSize(16);thanks.setTextColor(fg());thanks.setPadding(0,dp(8),0,dp(24));root.addView(thanks);
        TextView coffee=link("☕  Buy me a coffee");root.addView(coffee,new LinearLayout.LayoutParams(-1,dp(52)));coffee.setOnClickListener(v->openUrl(BUY_ME_A_COFFEE_URL));
        TextView github=link("◉  GitHub profile");root.addView(github,new LinearLayout.LayoutParams(-1,dp(52)));github.setOnClickListener(v->openUrl(GITHUB_PROFILE_URL));
        TextView project=link("◉  NoteLog on GitHub");root.addView(project,new LinearLayout.LayoutParams(-1,dp(52)));project.setOnClickListener(v->openUrl(GITHUB_PROJECT_URL));
        Button back=new Button(this);back.setText("Back");root.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));back.setOnClickListener(v->finish());
    }
    private TextView link(String s){TextView v=new TextView(this);v.setText(s);v.setTextSize(17);v.setTextColor(dark()?0xFF90CAF9:0xFF1565C0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private void openUrl(String u){try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));}catch(Exception e){Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show();}}
}
