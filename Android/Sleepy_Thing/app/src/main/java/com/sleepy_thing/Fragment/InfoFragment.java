package com.sleepy_thing.Fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.hb.dialog.dialog.ConfirmDialog;
import com.hb.dialog.myDialog.MyAlertInputDialog;
import com.sleepy_thing.Base.BaseFragment;
import com.sleepy_thing.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import static com.sleepy_thing.MainActivity.Baby_self;
import static com.sleepy_thing.MainActivity.HeadFolder;
import static com.sleepy_thing.MainActivity.Huyou_dev;
import static com.sleepy_thing.MainActivity.mDate;
import static com.sleepy_thing.MainActivity.mDevice;

public class InfoFragment extends BaseFragment {

    private Button BabyName;
    private Button HuyouName;
    private TextView NBState;
    private ImageButton BabyHeader;
    private ImageButton HuyouHeader;
    private Button NBButton;
    private String[] NBSleepShow =
    {
        "睡觉中",
        "呼呼呼",
        "ZzZzZz"
    };
    private String[] NBWakeShow =
    {
        "醒着呐",
        "想你中",
    };
    private String[] NBDisCntShow =
    {
        "失联中",
        "生死未卜中",
    };

    private final int BABYHEADER_PICKER_SELECT = 0;
    private final int DEVHEADER_PICKER_SELECT = 1;
    private final int CROP_BABY_PHOTO_DONE = 2;
    private final int CROP_DEV_PHOTO_DONE = 3;


    @Override
    protected View onCreateOwnView(LayoutInflater inflater, ViewGroup container) {
        View mView = inflater.inflate(R.layout.info_layout, container, false);

        BabyName = mView.findViewById(R.id.baby_name);
        HuyouName = mView.findViewById(R.id.huyou_name);
        NBState = mView.findViewById(R.id.dev_nb_st);
        BabyHeader = mView.findViewById(R.id.baby_header);
        HuyouHeader = mView.findViewById(R.id.huyou_header);
        NBButton = mView.findViewById(R.id.dev_nb_st_bt);

        Init();
        mDevice.addPropertyChangeListener(new InfoChangeListener());

        NBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO NBButton
            }
        });

        BabyHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startPhoto(InfoFragment.this,BABYHEADER_PICKER_SELECT);
            }
        });

        HuyouHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startPhoto(InfoFragment.this,DEVHEADER_PICKER_SELECT);
                //HuyouHeader.setImageBitmap(BitmapFactory.decodeFile(Huyou_dev.getUheadPath()));
            }
        });

        BabyName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDevice.getBleConnectState() == 3) {
                    final MyAlertInputDialog myAlertInputDialog = new MyAlertInputDialog(getContext()).builder()
                            .setTitle("请输入")
                            .setEditText("");
                    myAlertInputDialog.setPositiveButton("确认", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String name = myAlertInputDialog.getResult();
                            Baby_self.setUname(name);
                            mDevice.SendChat("叫我" + name,mDate.getTime());
                            BabyName.setText(Baby_self.getUname());
                            myAlertInputDialog.dismiss();
                        }
                    }).setNegativeButton("取消", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            myAlertInputDialog.dismiss();
                        }
                    });
                    myAlertInputDialog.show();
                }
                else {
                    ConfirmDialog confirmDialog = new ConfirmDialog(getContext());
                    confirmDialog.setLogoImg(R.mipmap.dialog_notice).setMsg("连接之后修改你的称呼");
                    confirmDialog.setClickListener(new ConfirmDialog.OnBtnClickListener() {
                        @Override
                        public void ok() {

                        }

                        @Override
                        public void cancel() {

                        }
                    });
                    confirmDialog.show();
                }
            }
        });

        HuyouName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDevice.getBleConnectState() == 3) {
                    final MyAlertInputDialog myAlertInputDialog = new MyAlertInputDialog(getContext()).builder()
                            .setTitle("请输入")
                            .setEditText("");
                    myAlertInputDialog.setPositiveButton("确认", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String name = myAlertInputDialog.getResult();
                            Huyou_dev.setUname(name);
                            mDevice.SendChat("以后你的名字就叫" + name,mDate.getTime());
                            HuyouName.setText(Huyou_dev.getUname());
                            myAlertInputDialog.dismiss();
                        }
                    }).setNegativeButton("取消", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            myAlertInputDialog.dismiss();
                        }
                    });
                    myAlertInputDialog.show();
                }
                else {
                    ConfirmDialog confirmDialog = new ConfirmDialog(getContext());
                    confirmDialog.setLogoImg(R.mipmap.dialog_notice).setMsg("连接之后修改我的名字");
                    confirmDialog.setClickListener(new ConfirmDialog.OnBtnClickListener() {
                        @Override
                        public void ok() {

                        }

                        @Override
                        public void cancel() {

                        }
                    });
                    confirmDialog.show();
                }
            }
        });

        return mView;
    }

    public static void startPhoto(Fragment fragment, int requestCode){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//intent隐式调用启动相册界面
        intent.setType("image/*");//设置数据类型
        ComponentName componentName = intent.resolveActivity(fragment.getContext().getPackageManager());
        Log.d("PICTURE", "startPhoto: "+componentName);
        if (componentName != null) {//防止启动意图时app崩溃
            fragment.startActivityForResult(intent, requestCode);
        }
    }

    public void cropPhoto(Fragment fragment,Uri uri,int requestCode) {

        Intent intent = new Intent("com.android.camera.action.CROP");
        Log.i("beento","执行到压缩图片了");
        intent.setDataAndType(uri, "image/*");

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 250);
        intent.putExtra("outputY", 250);

        File NewHeader = null;
        NewHeader = new File(HeadFolder.getPath(), "BULL.jpeg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(NewHeader));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//设置输出格式
        intent.putExtra("return-data", true);

        Log.i("beento","即将跳到剪切图片");
        switch (requestCode){
            case (BABYHEADER_PICKER_SELECT):startActivityForResult(intent, CROP_BABY_PHOTO_DONE);break;
            case (DEVHEADER_PICKER_SELECT):startActivityForResult(intent, CROP_DEV_PHOTO_DONE);break;
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case (BABYHEADER_PICKER_SELECT):
            {
                Log.i("beento","BABYHEADER_PICKER_SELECT");
                if (resultCode == Activity.RESULT_OK) {
                    cropPhoto(InfoFragment.this, data.getData(),BABYHEADER_PICKER_SELECT);
                }
                break;
            }
            case (DEVHEADER_PICKER_SELECT):
            {
                if (resultCode == Activity.RESULT_OK) {
                    cropPhoto(InfoFragment.this, data.getData(),DEVHEADER_PICKER_SELECT);
                }
                break;
            }
            case (CROP_BABY_PHOTO_DONE):
            {
                if (data != null) {
                    Bundle extras = data.getExtras();
                    Bitmap head = extras.getParcelable("data");
                    Log.i("beento","CROP_BABY_PHOTO_DONE");
                    File NewHeader = null;
                    NewHeader = new File(HeadFolder.getPath(), "BabyHeader.jpeg");

                    try {
                        NewHeader.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(NewHeader);
                        head.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            case (CROP_DEV_PHOTO_DONE):
            {
                Log.i("beento","CROP_DEV_PHOTO_DONE");
                File NewHeader = null;
                NewHeader = new File(HeadFolder.getPath(), "DevHeader.jpeg");

                if (!NewHeader.exists()){
                    try {
                        NewHeader.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void Init(){
        BabyName.setText(Baby_self.getUname());
        BabyHeader.setImageBitmap(BitmapFactory.decodeFile(Baby_self.getUheadPath()));
        HuyouName.setText(Huyou_dev.getUname());
        HuyouHeader.setImageBitmap(BitmapFactory.decodeFile(Huyou_dev.getUheadPath()));

        if(mDevice.getBleConnectState() == 3)  //蓝牙已连接状态
        {
            if(mDevice.getNBConnectState() == 3){ BLEonNBon(); }
            else if (mDevice.getNBConnectState() == 0){ BLEonNBoff();}
            else if (mDevice.getNBConnectState() == 1){ NBButton.setEnabled(false);}
        }
        else { BLEoffNBunknow(); }
    }

    private void BLEonNBon(){
        //TODO 头像和文本更换使能
        NBState.setText(NBWakeShow[new Random().nextInt(NBWakeShow.length)]);
        NBButton.setEnabled(true);
        NBButton.setText("去睡觉");
    }

    private void BLEonNBoff(){
        NBState.setText(NBSleepShow[new Random().nextInt(NBSleepShow.length)]);
        NBButton.setEnabled(true);
        NBButton.setText("唤醒");
    }

    private void BLEoffNBunknow(){
        NBState.setText(NBDisCntShow[new Random().nextInt(NBDisCntShow.length)]);
        NBButton.setEnabled(false);
        NBButton.setText("等待连接");
    }

    public class InfoChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            switch (evt.getPropertyName()) {
                case ("NPD"):
                    switch ((int)evt.getNewValue()){
                        case (3):
                            BLEonNBon();
                            break;
                        case (0):
                            BLEonNBoff();
                            break;
                    }
                    break;
            }
        }
    }

}
