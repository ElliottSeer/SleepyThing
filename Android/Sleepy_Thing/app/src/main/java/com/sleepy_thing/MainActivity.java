package com.sleepy_thing;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.sleepy_thing.Adapter.MainContentAdapter;
import com.sleepy_thing.Base.BaseDevice;
import com.sleepy_thing.Base.Chat;
import com.sleepy_thing.Base.User;
import com.sleepy_thing.Utils.SQLiteDBHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends FragmentActivity {

    private ViewPager mViewPager;
    public static BleDevice mBleDevice;
    public static BaseDevice mDevice;
    public static SQLiteDBHelper DBHelper;
    public static SQLiteDatabase Database;
    public static File HeadFolder;
    public static File BabyHeader;
    public static File DevHeader;

    //读写权限
    private static String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    List<String> mPermissionList = new ArrayList<>();
    private final int mRequestCode = 100;//权限请求码

    public static User Baby_self;
    public static User Huyou_dev;
    public static Date mDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDate = new Date();
        mDevice =  new BaseDevice("Sleepy_Thing");
        DBHelper = new SQLiteDBHelper(this.getApplicationContext());
        Database = DBHelper.getWritableDatabase();

        setContentView(R.layout.activity_main);

        initView();

        requestPermissions();

        MatchHeadFiles();

        BleManager.getInstance().init(getApplication());

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(0, 5000)
                .setSplitWriteNum(20)
                .setConnectOverTime(10000)
                .setOperateTimeout(5000);

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(false, mDevice.getDevName())   // 只扫描指定广播名的设备，可选
                .setAutoConnect(false)                               // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)                              // 扫描超时时间，可选，默认10秒
                .build();

        BleManager.getInstance().initScanRule(scanRuleConfig);

        if(BleManager.getInstance().isBlueEnable() != true)
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0x01);
        }



    }

    private void MatchHeadFiles(){
        HeadFolder = new File(getApplicationContext().getFilesDir(), "Headers");
        //先看有没有文件夹 没有就建
        if (!HeadFolder.exists())
        {
            //HeadFolder.mkdir();//创建文件夹
            if (HeadFolder.mkdirs()) {
                Log.i("HeaderFile","创建文件夹成功");
            } else { Log.i("HeaderFile","创建文件夹失败"); }
        }

        //再看有没有图片 没有就建
        try{
            BabyHeader = new File(HeadFolder.getPath(), "BabyHeader.png");
            if(!BabyHeader.exists()){
                BabyHeader.createNewFile();                     //新建空文件
                FileOutputStream fOut = new FileOutputStream(BabyHeader);
                BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.header_right).compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();   fOut.close();
            }
        } catch (Exception e) {}

        try{
            DevHeader = new File(HeadFolder.getPath(), "DevHeader.png");
            if(!DevHeader.exists()){
                DevHeader.createNewFile();                     //新建空文件
                FileOutputStream fOut = new FileOutputStream(DevHeader);
                BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.header_left).compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();   fOut.close();
            }
        } catch (Exception e) {}

        if(firstRun())  //第一次运行 直接插入数据库
        {
            Baby_self = new User(0,"称呼你的名字", BabyHeader.getPath());
            Baby_self.addtoDB();
            Huyou_dev = new User(1,"小金毛的名字",DevHeader.getPath());
            Huyou_dev.addtoDB();
            Chat chat = new Chat(1,"生日快乐！！！",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"嗷呜！",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"等了这么久终于要见到你啦",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"我就是你的生日礼物",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"之一！",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"那你就是我的主人啦",(long)(1568736000*1000)); chat.addtoDB();
            chat = new Chat(1,"要好好照顾我嗷！",(long)(1568736000*1000)); chat.addtoDB();
        }
        else {  //第n次运行 从数据库更新
            Baby_self = new User(0,"needupdate", BabyHeader.getPath());
            Baby_self.updateUser();
            Huyou_dev = new User(1,"needupdate",DevHeader.getPath());
            Huyou_dev.updateUser();
        }
    }

    private void initView() {
        //创建内容适配器。
        mViewPager = this.findViewById(R.id.content_pager);
        MainContentAdapter mainContentAdapter = new MainContentAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mainContentAdapter);
    }

    private void requestPermissions() {
        mPermissionList.clear();//清空没有通过的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);//添加还未授予的权限
                }
            }
            //申请权限
            if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
                ActivityCompat.requestPermissions(this, permissions, mRequestCode);
            } else {
                //说明权限都已经通过，可以做你想做的事情去
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            boolean hasPermissionDismiss = false;//有权限没有通过
            if (mRequestCode == requestCode) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == -1) {
                        hasPermissionDismiss = true;
                    }
                }
                //如果有权限没有被允许
                if (hasPermissionDismiss) {
                    Toast.makeText(getApplicationContext(),"不给权限就要崩溃啦嗷嗷嗷",Toast.LENGTH_SHORT).show();
                }else{
                    if(Huyou_dev.getUname().equals("小金毛的名字")){
                        Toast.makeText(getApplicationContext(),"集齐所有权限 召唤小金毛！",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"集齐所有权限 召唤" + Huyou_dev.getUname() + "!",Toast.LENGTH_SHORT).show();
                    }

                }
            }
    }

    private boolean firstRun() {
        SharedPreferences sharedPreferences = getSharedPreferences("FirstRun",0);
        Boolean first_run = sharedPreferences.getBoolean("First",true);
        if (first_run){
            sharedPreferences.edit().putBoolean("First",false).commit();
            return true;
        }
        else {
            return false;
        }
    }
}
