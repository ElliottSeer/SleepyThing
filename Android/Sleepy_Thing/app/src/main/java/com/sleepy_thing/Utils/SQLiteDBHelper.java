package com.sleepy_thing.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "Sleey_Thing_App.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_USER = "user";
    public static final String TABLE_CHAT = "chat";

    //创建 students 表的 sql 语句
    private static final String CREATE_TABLE_USER_SQL;
    static {
        CREATE_TABLE_USER_SQL = "create table " + TABLE_USER + "("
                + "Uid integer primary key,"
                + "Uname text not null,"
                + "Uheadpath text not null"
                + ");";
    }

    private static final String CREATE_TABLE_CHAT_SQL;
    static {
        CREATE_TABLE_CHAT_SQL = "create table " + TABLE_CHAT + "("
                + "Mid integer primary key autoincrement,"
                + "Uid integer not null,"
                + "Mtext text not null,"
                + "Mtime integer not null"
                + ");";
    }

    public SQLiteDBHelper(Context context) {
        // 传递数据库名与版本号给父类
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // 在这里通过 db.execSQL 函数执行 SQL 语句创建所需要的表
        db.execSQL(CREATE_TABLE_USER_SQL);
        db.execSQL(CREATE_TABLE_CHAT_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库版本号变更会调用 onUpgrade 函数，在这根据版本号进行升级数据库
        switch (oldVersion) {
            case 1:
                // do something
                break;

            default:
                break;
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // 启动外键
            db.execSQL("PRAGMA foreign_keys = 1;");
            //或者这样写
            String query = String.format("PRAGMA foreign_keys = %s", "ON");
            db.execSQL(query);
        }
    }

}

