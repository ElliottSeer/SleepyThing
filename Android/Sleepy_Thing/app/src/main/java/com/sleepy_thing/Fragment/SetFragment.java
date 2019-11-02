package com.sleepy_thing.Fragment;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.sdsmdg.harjot.crollerTest.Croller;
import com.sdsmdg.harjot.crollerTest.OnCrollerChangeListener;
import com.sleepy_thing.Base.BaseFragment;
import com.sleepy_thing.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static com.sleepy_thing.MainActivity.mDevice;

public class SetFragment extends BaseFragment {

    private static Croller LightCroller;
    private static boolean LightCrollerEnable;
    private static int LightCrollerLock;        //这个控件没法disable 我傻咧

    private static Switch BleSwitch;
    private static Switch LightSwitch;  //may cause memory leak

    public static Context mContext;

    @Override
    protected View onCreateOwnView(LayoutInflater inflater, ViewGroup container) {

        View mView = inflater.inflate(R.layout.set_layout, container, false);

        this.mContext = getActivity();

        LightCroller = mView.findViewById(R.id.LightCroller);
        LightCroller.setLabel(" ");
        BleSwitch = mView.findViewById(R.id.BleSwitch);
        LightSwitch = mView.findViewById(R.id.LightSwitch);

        //Init
        switch (mDevice.getBleConnectState())
        {
            case(0):{
                BleSwitch.setChecked(false);
                BleSwitch.setEnabled(true);
                LightSwitch.setEnabled(false);
                LightSwitch.setChecked(false);
                LightCrollerEnable = (false);
                LightCroller.setLabel(" ");
                LightCroller.setProgress(0);
                break;
            }
            case(1):{
                BleSwitch.setChecked(false);
                BleSwitch.setEnabled(false);
                LightSwitch.setEnabled(false);
                LightSwitch.setChecked(false);
                LightCrollerEnable = (false);
                LightCroller.setLabel(" ");
                LightCroller.setProgress(0);
                break;
            }
            case(2):{
                BleSwitch.setChecked(true);
                BleSwitch.setEnabled(false);
                LightSwitch.setChecked(mDevice.getLedPower() == 3);
                LightSwitch.setEnabled(false);
                LightCrollerEnable = (false);
                LightCroller.setLabel(" ");
                LightCroller.setProgress(mDevice.getLedLevel());
                break;
            }
            case(3):{
                BleSwitch.setChecked(true);
                BleSwitch.setEnabled(true);
                LightSwitch.setChecked(mDevice.getLedPower() == 3);
                LightSwitch.setEnabled(true);
                if(mDevice.getLedPower() == 3) {
                    LightCroller.setEnabled(true);
                    LightCroller.setLabel(""+mDevice.getLedLevel());
                    LightCroller.setProgress(mDevice.getLedLevel());
                }
                else {
                    LightCrollerEnable = (false);
                    LightCroller.setLabel(" ");
                    LightCroller.setProgress(0);
                }
                break;
            }
            default:break;
        }

        mDevice.addPropertyChangeListener(new DevChangeListener());

        //蓝牙连接 完成
        BleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(BleSwitch.isEnabled()){
                    if (isChecked) {
                        mDevice.setBleConnectState(true,1);
                    }
                    else {
                        mDevice.setBleConnectState(true,2);
                    }
                }
            }
        });


        LightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mDevice.getBleConnectState() == 3 && LightSwitch.isEnabled()){
                    if (isChecked) {
                        mDevice.setLedPower(true,1);
                        LightSwitch.setEnabled(false);
                        Log.i("DEBUG","set LedPower:1");
                    }
                    else {
                        mDevice.setLedPower(true,2);
                        LightSwitch.setEnabled(false);
                        Log.i("DEBUG","set LedPower:2");
                    }
                }
            }
        });


        LightCroller.setOnCrollerChangeListener(new OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(Croller croller, int progress) {
                if(LightCrollerEnable == false) {
                    LightCroller.setLabel(" ");
                    LightCroller.setProgress(LightCrollerLock);
                }
                else if(LightCrollerEnable == true && mDevice.getLedPower() == 3)
                {
                    mDevice.setLedLevel(true,progress);
                    LightCroller.setLabel("" + progress);
                }
            }
            @Override
            public void onStartTrackingTouch(Croller croller) {
                if(LightCrollerEnable == false) {
                    LightCrollerLock = LightCroller.getProgress();
                }
            }
            @Override
            public void onStopTrackingTouch(Croller croller) {
                if(LightCrollerEnable == true){
                    mDevice.askReport();
                }
            }
        });

        return mView;
    }

    public static class DevChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            switch (evt.getPropertyName()){
                case ("LPD"):{
                    if((int)evt.getNewValue() == 3)
                    {
                        LightSwitch.setChecked(true);
                        LightSwitch.setEnabled(true);
                        LightCrollerEnable = (true);
                        LightCroller.setLabel(""+ mDevice.getLedLevel());
                        LightCroller.setProgress(mDevice.getLedLevel());
                    }
                    else if((int)evt.getNewValue() == 0)
                    {
                        LightSwitch.setChecked(false);
                        LightSwitch.setEnabled(true);
                        LightCrollerEnable = (false);
                        LightCroller.setLabel("");
                        //LightCroller.setProgress(0);
                    }
                    break;
                }

                //蓝牙连接 完成
                case ("CSD"):{
                    Log.i("DEBUG", "SF CSD " + evt.getOldValue() + " " + evt.getNewValue());
                    switch ((int)evt.getNewValue()){
                        case (0):
                            Toast.makeText(SetFragment.mContext,"Connection Fialed T_T",Toast.LENGTH_SHORT)
                                    .show();
                            BleSwitch.setEnabled(false);    //防止正常连接时 蓝牙断开 按键被check
                            BleSwitch.setChecked(false);
                            BleSwitch.setEnabled(true);
                            LightSwitch.setEnabled(false);
                            LightSwitch.setChecked(false);
                            LightCrollerEnable = (false);
                            LightCroller.setLabel("");
                            LightCroller.setProgress(0);
                            break;
                        case (3):
                            Toast.makeText(SetFragment.mContext,"Connection Success ^_^",Toast.LENGTH_SHORT)
                                    .show();
                            BleSwitch.setChecked(true);
                            BleSwitch.setEnabled(true);
                            LightSwitch.setEnabled(true);
                            LightSwitch.setChecked(mDevice.getLedPower() == 3);
                            if(mDevice.getLedPower() == 3) {
                                LightCroller.setEnabled(true);
                                LightCroller.setLabel("" + mDevice.getLedLevel());
                                LightCroller.setProgress(mDevice.getLedLevel());
                            }
                            else {
                                LightCrollerEnable = (false);
                                LightCroller.setLabel("");
                                LightCroller.setProgress(0);
                            }
                            break;
                        case (4):
                            BleSwitch.setEnabled(false);
                            Toast.makeText(SetFragment.mContext,"On Scanning... @_@",Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case (5):
                            BleSwitch.setEnabled(false);
                            Toast.makeText(SetFragment.mContext,"On Scan Started @_@",Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case (6):
                            BleSwitch.setEnabled(false);
                            Toast.makeText(SetFragment.mContext,"On Start Connect !_!",Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case (8):
                            Toast.makeText(SetFragment.mContext,"Active DisConnected ->_->",Toast.LENGTH_SHORT)
                                    .show();
                            BleSwitch.setChecked(false);
                            BleSwitch.setEnabled(true);
                            LightSwitch.setEnabled(false);
                            LightSwitch.setChecked(false);
                            LightCrollerEnable = (false);
                            LightCroller.setLabel("");
                            LightCroller.setProgress(0);
                            mDevice.setBleConnectState(true,0);
                            break;
                        case (7):
                            BleSwitch.setEnabled(false);
                            Toast.makeText(SetFragment.mContext,"On Scan Success !_!",Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        case (9):
                            Toast.makeText(SetFragment.mContext,"On Scan Failed T_T",Toast.LENGTH_SHORT)
                                    .show();
                            BleSwitch.setChecked(false);
                            BleSwitch.setEnabled(true);
                            LightSwitch.setEnabled(false);
                            LightSwitch.setChecked(false);
                            LightCrollerEnable = (false);
                            LightCroller.setLabel("");
                            LightCroller.setProgress(0);
                            mDevice.setBleConnectState(true,0);
                            break;
                    }
                    break;
                }

                case ("LLD"):{
                    if(mDevice.getLedPower() == 3 &&  LightCrollerEnable){
                        LightCroller.setProgress((int)evt.getNewValue());
                        LightCroller.setLabel(""+(int)evt.getNewValue());
                    }
                    break;
                }

                case ("CFLT"):{
                    Toast.makeText(SetFragment.mContext,"Operational conflict @_@",Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }
}
