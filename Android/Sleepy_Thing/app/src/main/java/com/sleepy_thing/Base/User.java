package com.sleepy_thing.Base;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.sleepy_thing.Utils.SQLiteDBHelper;

import java.nio.ByteBuffer;

import static com.sleepy_thing.MainActivity.Database;

public class User {
    private int Uid;
    private String Uname;
    private String UheadPath;

    public User(int uid) {this.Uid = uid;}

    public User(int uid, String uname, String uhead){
        this.Uid = uid;
        this.Uname = uname;
        this.UheadPath = uhead;
    }

    public int getUid(){return this.Uid;}
    public String getUname(){return this.Uname;}
    public String getUheadPath(){return this.UheadPath;}

    public void addtoDB(){
        ContentValues contentValues = new ContentValues();
        contentValues.put("Uid",this.Uid);
        contentValues.put("Uname",this.Uname);
        contentValues.put("Uheadpath",this.UheadPath);
        Database.insert(SQLiteDBHelper.TABLE_USER,null,contentValues);
    }

    public void updateDB()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Uname",this.Uname);
        contentValues.put("Uheadpath",this.UheadPath);
        Database.update(SQLiteDBHelper.TABLE_USER,contentValues,"Uid = ?",new String[]{Integer.toString(this.Uid)});
    }

    public void setUname(String name)
    {
        this.Uname = name;
        ContentValues contentValues = new ContentValues();
        contentValues.put("Uname",this.Uname);
        Database.update(SQLiteDBHelper.TABLE_USER,contentValues,"Uid = ?",new String[]{Integer.toString(this.Uid)});
    }

    public void setUhead(String headtringpath)
    {
        this.UheadPath = headtringpath;
        ContentValues contentValues = new ContentValues();
        contentValues.put("Uheadpath",this.UheadPath);
        Database.update(SQLiteDBHelper.TABLE_USER,contentValues,"Uid = ?",new String[]{Integer.toString(this.Uid)});
    }

    public void updateUser()
    {
        Cursor cursor = Database.query(SQLiteDBHelper.TABLE_USER, null,
            "Uid = ?", new String[]{Integer.toString(this.Uid)},
                null,null,null,null);
        if (cursor.moveToFirst()) {
            Uname = cursor.getString(cursor.getColumnIndex("Uname"));
            UheadPath = cursor.getString(cursor.getColumnIndex("Uheadpath"));
        }
        cursor.close();
    }

    public static byte[] BitMap2Byte(Bitmap bitmap)
    {
//        Bitmap DisBitmap = Bitmap.createScaledBitmap(bitmap, 600, 600, true);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        DisBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//        return baos.toByteArray();
        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer);
        byte[] data = buffer.array();
        return data;
    }


    public Bitmap Byte2BitMap(byte[] bytein)
    {
        Bitmap bmpout = BitmapFactory.decodeByteArray(bytein, 0, bytein.length);
        return bmpout;
    }

}
