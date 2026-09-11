package com.example.simplejournal;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** Local storage backed by a user-selected SAF folder. The notes themselves are ordinary .txt files. */
public final class Storage {
    private static final String PREFS="storage";
    private static final String TREE="tree_uri";
    private static final String TYPES_PREFS="note_types";
    private static final String LEGACY_TYPES_FILE=".simplejournal_types";
    private Storage() {}
    public static class Item { public String name; public long date; public Uri uri; Item(String n,long d,Uri u){name=n;date=d;uri=u;} }

    public static Uri getTree(Context c){ String s=prefs(c).getString(TREE,null); return s==null?null:Uri.parse(s); }
    public static void setTree(Context c,Uri tree){ prefs(c).edit().putString(TREE,tree.toString()).apply(); }
    public static boolean hasFolder(Context c){ return getTree(c)!=null; }

    public static List<Item> list(Context c,String query,boolean newest){
        ArrayList<Item> out=new ArrayList<>(); Uri tree=getTree(c); if(tree==null)return out;
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        try{
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_LAST_MODIFIED};
            try(Cursor cur=c.getContentResolver().query(children,cols,null,null,null)){
                if(cur!=null)while(cur.moveToNext()){
                    String name=cur.getString(1); long date=cur.isNull(3)?0:cur.getLong(3);
                    if(name==null)continue;
                    // Some Android file providers keep deleted documents as hidden .trashed-* files.
                    // Never show those provider-trash entries as NoteLog files.
                    String lowerName=name.toLowerCase(Locale.ROOT);
                    if(lowerName.startsWith(".trashed"))continue;
                    if(!lowerName.endsWith(".txt"))continue;
                    Uri u=DocumentsContract.buildDocumentUriUsingTree(tree,cur.getString(0));
                    String text=read(c,u);
                    if(q.isEmpty()||name.toLowerCase(Locale.ROOT).contains(q)||text.toLowerCase(Locale.ROOT).contains(q))out.add(new Item(name,date,u));
                }
            }
        }catch(Exception ignored){}
        out.sort((a,b)->newest?Long.compare(b.date,a.date):a.name.compareToIgnoreCase(b.name)); return out;
    }

    public static Uri create(Context c,String name,String content)throws IOException{
        Uri tree=getTree(c); if(tree==null)throw new IOException("Choose a storage folder first.");
        name=normalize(name);
        if(find(c,tree,name)!=null)throw new IOException("File already exists");
        Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        Uri created=DocumentsContract.createDocument(c.getContentResolver(),parent,"text/plain",name);
        if(created==null)throw new IOException("Unable to create file");
        write(c,created,content); return created;
    }

    public static Uri find(Context c,Uri tree,String name){
        if(tree==null)return null;
        String target=normalize(name);
        try{
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME};
            try(Cursor cur=c.getContentResolver().query(children,cols,null,null,null)){
                if(cur!=null)while(cur.moveToNext())if(target.equalsIgnoreCase(cur.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(tree,cur.getString(0));
            }
        }catch(Exception ignored){}
        return null;
    }

    public static String normalize(String n){ n=n==null?"":n.trim(); if(!n.toLowerCase(Locale.ROOT).endsWith(".txt"))n+=".txt"; return n; }

    /** Creates the one-time built-in guide in each newly selected folder. */
    public static void ensureWelcomeFile(Context c){
        Uri tree=getTree(c); if(tree==null)return;
        SharedPreferences p=c.getSharedPreferences("welcome",Context.MODE_PRIVATE);
        String key=tree.toString();
        if(key.equals(p.getString("initialized_tree",null)))return;
        try{
            String name="Welcome to NoteLog.txt";
            if(find(c,tree,name)==null){
                String guide="NoteLog — Quick Guide\n\n"+
                        "Welcome! This file explains the basics of NoteLog.\n\n"+
                        "GETTING STARTED\n"+
                        "• Tap + to create a new file.\n"+
                        "• Choose the file type first, then enter the filename.\n"+
                        "• Notes and Bullets use • automatically.\n"+
                        "• Check-Box files use ☐. Tap a checkbox to mark it ☑.\n"+
                        "• Log files add timestamps to completed entries.\n"+
                        "• Plain files are completely free-form text.\n\n"+
                        "FINDING NOTES\n"+
                        "• Use the search box on the home screen to search filenames and file contents.\n"+
                        "• Use Find inside a note to locate text in the current file.\n\n"+
                        "FILES & STORAGE\n"+
                        "• NoteLog stores ordinary .txt files in the folder you choose.\n"+
                        "• Your notes are not stored only inside the app, so they remain accessible from Android Files.\n"+
                        "• You can rename, delete, and sort files from the home screen.\n\n"+
                        "SYNC OPTIONS\n"+
                        "NoteLog does not have built-in cloud synchronization. This is intentional: the files remain ordinary text files that you control.\n\n"+
                        "Option 1 — Cloud storage\n"+
                        "If Android Files lets you select a folder provided by a cloud-storage app such as Google Drive or OneDrive, you can choose that folder as your NoteLog folder. Availability depends on the storage provider and Android version.\n\n"+
                        "Option 2 — Computer sync\n"+
                        "Use a sync tool that synchronizes the selected folder between your phone and computer. This keeps the files as normal .txt files. Avoid editing the same file on two devices at the same time to reduce conflicts.\n\n"+
                        "Option 3 — Manual backup\n"+
                        "Copy the NoteLog folder to a computer, USB drive, or another backup location whenever you want. Because the files are plain text, they are easy to archive and restore.\n\n"+
                        "APP LOCK\n"+
                        "Open the menu to enable device-security protection. NoteLog uses your Android device authentication.\n\n"+
                        "THEME\n"+
                        "Use the menu to choose System, Light, or Dark mode.\n\n"+
                        "TIP\n"+
                        "Keep this guide, rename it, or delete it whenever you want. It is an ordinary text file in your selected folder.\n";
                create(c,name,guide);
                setType(c,name,"Plain");
            }
            p.edit().putString("initialized_tree",key).apply();
        }catch(Exception ignored){}
    }
    public static String read(Context c,Uri u){try(InputStream in=c.getContentResolver().openInputStream(u)){return readStream(in);}catch(Exception e){return "";}}
    private static String readStream(InputStream in)throws IOException{if(in==null)return "";BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null){if(b.length()>0)b.append('\n');b.append(s);}return b.toString();}
    public static void write(Context c,Uri u,String text)throws IOException{try(OutputStream o=c.getContentResolver().openOutputStream(u,"wt")){if(o==null)throw new IOException("Unable to write");o.write(text.getBytes(StandardCharsets.UTF_8));o.flush();}}
    public static void delete(Context c,Uri u)throws IOException{
        if(u==null)throw new IOException("Unable to delete: invalid file reference");
        ContentResolver r=c.getContentResolver();
        try{
            // SAF document providers are not required to return a positive row count
            // from ContentResolver.delete(). Use DocumentsContract.deleteDocument(),
            // which is the proper API for a document selected through ACTION_OPEN_DOCUMENT_TREE.
            DocumentsContract.deleteDocument(r,u);
            return;
        }catch(Exception first){
            // A few older/custom providers implement delete() but not deleteDocument().
            try{
                int rows=r.delete(u,null,null);
                if(rows>0)return;
            }catch(Exception ignored){}
            throw new IOException("Unable to delete this file. The selected storage provider may not support deletion.",first);
        }
    }
    public static void rename(Context c,Uri u,String name)throws IOException{Uri r=DocumentsContract.renameDocument(c.getContentResolver(),u,normalize(name));if(r==null)throw new IOException("Unable to rename");}
    public static String date(long ms){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(ms));}

    /* Note type is app metadata, not a hidden file in the user's chosen folder. */
    public static void setType(Context c,String fileName,String type){if(fileName==null||type==null)return;prefsTypes(c).edit().putString(normalize(fileName).toLowerCase(Locale.ROOT),type).apply();}
    public static String getType(Context c,String fileName,Uri uri){
        String key=normalize(fileName).toLowerCase(Locale.ROOT);
        String t=prefsTypes(c).getString(key,null);
        if(t!=null)return t;
        t=inferType(c,uri);
        if(t!=null)setType(c,fileName,t);
        return t==null?"Plain":t;
    }
    public static String getType(Context c,String fileName){return getType(c,fileName,null);}
    public static void removeType(Context c,String fileName){prefsTypes(c).edit().remove(normalize(fileName).toLowerCase(Locale.ROOT)).apply();}
    public static void renameType(Context c,String oldName,String newName){String t=getType(c,oldName,null);removeType(c,oldName);if(t!=null)setType(c,newName,t);}

    private static String inferType(Context c,Uri u){
        if(u==null)return null;
        String text=read(c,u);
        if(text.startsWith("[YYYY-MM-DD HH:MM] -")||text.matches("(?s).*^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2} - .*$"))return "Log";
        if(text.startsWith("☐ ")||text.startsWith("☑ "))return "Check-Box";
        if(text.startsWith("• "))return "Bullets";
        return null;
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static SharedPreferences prefsTypes(Context c){return c.getSharedPreferences(TYPES_PREFS,Context.MODE_PRIVATE);}

    public static Intent folderIntent(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);return i;}
    public static void persistFolder(Context c,Uri tree,int flags){
        try{c.getContentResolver().takePersistableUriPermission(tree,flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
        setTree(c,tree);
        migrateLegacyMetadata(c);
    }

    /** Imports metadata from V1.2 once, then removes only the metadata file created by this app. */
    private static void migrateLegacyMetadata(Context c){
        try{
            Uri legacy=findDocument(c,getTree(c),LEGACY_TYPES_FILE);
            if(legacy==null)return;
            String all=read(c,legacy);
            if(!all.isEmpty())for(String line:all.split("\\n")){int p=line.indexOf('\t');if(p>0){String file=line.substring(0,p);String type=line.substring(p+1);if(!file.isEmpty()&&!type.isEmpty()&&prefsTypes(c).getString(normalize(file).toLowerCase(Locale.ROOT),null)==null)setType(c,file,type);}}
            c.getContentResolver().delete(legacy,null,null);
        }catch(Exception ignored){}
    }

    private static Uri findDocument(Context c,Uri tree,String name){
        if(tree==null)return null;
        try{
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME};
            try(Cursor cur=c.getContentResolver().query(children,cols,null,null,null)){if(cur!=null)while(cur.moveToNext())if(name.equals(cur.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(tree,cur.getString(0));}
        }catch(Exception ignored){}
        return null;
    }
}
