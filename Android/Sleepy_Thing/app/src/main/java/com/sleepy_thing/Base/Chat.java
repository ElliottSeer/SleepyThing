package com.sleepy_thing.Base;

import android.content.ContentValues;

import com.sleepy_thing.Utils.SQLiteDBHelper;

import static com.sleepy_thing.MainActivity.Database;

public class Chat {
    private int Uid;
    private String Mtext;
    private long Mtime;

    public Chat(int uid, String mtext, long mtime){
        this.Uid = uid;
        this.Mtext = mtext;
        this.Mtime = mtime;
    }

    public Chat(Chat chat){
        this.Uid = chat.Uid;
        this.Mtext = chat.Mtext;
        this.Mtime = chat.Mtime;
    }

    public int getUid(){return this.Uid;}
    public String getText(){return this.Mtext;}
    public long getTime(){return this.Mtime;}

    public void addtoDB()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Uid",this.Uid);
        contentValues.put("Mtext",this.Mtext);
        contentValues.put("Mtime",this.Mtime);
        Database.insert(SQLiteDBHelper.TABLE_CHAT,null,contentValues);
    }
}
