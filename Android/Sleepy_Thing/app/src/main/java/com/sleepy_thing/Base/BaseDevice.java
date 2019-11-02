package com.sleepy_thing.Base;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

import static com.sleepy_thing.MainActivity.Baby_self;
import static com.sleepy_thing.MainActivity.Huyou_dev;
import static com.sleepy_thing.MainActivity.mDate;
import static com.sleepy_thing.MainActivity.mDevice;

public class BaseDevice {
    private String DevName = "SleepyThing";

    private int LedPower = 0;               //00断开 01在连接 10在断开 11连接
    private int NBConnectState = 0;                //00断开 01在连接 10在断开 11连接
    private int BleConnectState = 0;           //00断开 01在连接 10在断开 11连接
    private int LedLevel = 0;

//    private static final String TEST_CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
//    private static final String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
//    private static final String CHARACTERISTIC_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
//    private static final String CHARACTERISTIC_UUID_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

    private static final String SERVICE_UUID   =     "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static final String CHARACTERISTIC_UUID  = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    private Queue<Chat> RecvQue = new LinkedList<Chat>();
    private Chat SendChat = null;
    private static final boolean sendtimeout = false;
    private int MsgSendFlag = 1;    //0 sending 1 sent 2 error 3 timeout
    private BleDevice mBleDevice;
    private byte[] cmd = new byte[6];

    protected final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public BaseDevice (String Name) {
        this.DevName = Name;
        this.addPropertyChangeListener(new UserChangeListener());
    }

    public String getDevName(){return this.DevName;}
    public int getLedPower(){return this.LedPower;}
    public int getBleConnectState(){return this.BleConnectState;}
    public int getNBConnectState(){return this.NBConnectState;}
    public int getLedLevel(){return this.LedLevel;}
    public int getMsgSendFlag(){return this.MsgSendFlag;}

    public void setLedPower(boolean fromUser, int state){
        if(fromUser == true){support.firePropertyChange("LPU", this.LedPower, state);}  //LedPower Changed by User
        else {support.firePropertyChange("LPD", this.LedPower, state);}            //LedPower Changed by Device
        this.LedPower = state;
    }

    public void setNBConnectState(boolean fromUser,int state){
        if(fromUser == true){support.firePropertyChange("NPU", this.NBConnectState, state);}
        else {support.firePropertyChange("NPD", this.NBConnectState, state);}
        this.NBConnectState = state;
    }

    public void setBleConnectState(boolean fromUser,int state){
        int old_state = this.BleConnectState;
        this.BleConnectState = state;
        if(fromUser == true){support.firePropertyChange("CSU", old_state, state);Log.i("DEBUG", "BD CSU " + old_state + " " + this.BleConnectState);}
        else {support.firePropertyChange("CSD", old_state, state);Log.i("DEBUG", "BD CSD " + old_state + " " + this.BleConnectState);}
    }

    public void setLedLevel(boolean fromUser,int ledLevel){
        if(fromUser == true){support.firePropertyChange("LLU", this.LedLevel, ledLevel);}
        else {support.firePropertyChange("LLD", this.LedLevel, ledLevel);}
        this.LedLevel = ledLevel;
    }

    public void setMsgSendFlag(int falg){support.firePropertyChange("MSGSD", this.MsgSendFlag, falg);this.MsgSendFlag = falg;}

    public void addRecvQue(String text, long time){
        Chat recvchat = new Chat(Huyou_dev.getUid(),text,time);
        this.RecvQue.offer(recvchat);
        support.firePropertyChange("RECHAT", 0, this.RecvQue.size());
    }

    public void SendChat(String text, long time){
        SendChat = new Chat(Baby_self.getUid(),text,time);
        setMsgSendFlag(0);
        byte[] textb = text.getBytes(Charset.forName("UTF-8"));
        String msg = "AT+NMGS="+textb.length+","+byteArrayToHexStr(textb)+"\r\n";
        Log.i("NBIOT",msg);
        MsgSend(msg);
        String str = new String(textb,Charset.forName("UTF-8"));
        Log.i("NBIOT","send :" +str);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mDevice.getMsgSendFlag() == 0){ setMsgSendFlag(3); }
            }
        }, 5000);//5000ms后无sent或error回复 视为消息发送超时
    }


    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static byte[] hexStrToByteArray(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte) Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }
    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }
    private static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }
        return hex.toString();
    }

    public Queue<Chat> getRecvQue()
    {
        return this.RecvQue;
    }
    public Chat getSendChat() {return this.SendChat;}

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void askReport(){
        byte[] ACK = {'R','P','O','T','\r','\n'};
        MsgSend(ACK);
    }

    public void Scan_Connect(){
        BleManager.getInstance().scanAndConnect(new BleScanAndConnectCallback() {
            @Override
            public void onScanning(BleDevice bleDevice) {setBleConnectState(false,4);}
            @Override
            public void onScanStarted(boolean success) {setBleConnectState(false,5);}
            @Override
            public void onScanFinished(BleDevice scanResult) {
                if(scanResult!= null){Log.i("DEBUG", "scanResult " + scanResult.getName());}

                 //扫描结束，结果即为扫描到的第一个符合扫描规则的BLE设备，如果为空表示未搜索到（主线程）
                if(scanResult!= null && scanResult.getName().equals(DevName) ) //这个名字是固定的 用于连接设备的 不能改
                {
                    setBleConnectState(false,7);
                    mBleDevice = scanResult;
                }
                else{
                //注意 可能经过多轮扫描
                    setBleConnectState(false,9);
                    //setBleConnectState(false,0);   //Listener响应完成后才执行set 千万注意！
                }
            }
            @Override
            public void onStartConnect() {setBleConnectState(false,6);}
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                setBleConnectState(false,0);
            }
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                setBleConnectState(false,3);
                mBleDevice = bleDevice;
                BleManager.getInstance().setMtu(mBleDevice, 512, new BleMtuChangedCallback() {
                    @Override
                    public void onSetMTUFailure(BleException exception) {
                        // 设置MTU失败
                    }

                    @Override
                    public void onMtuChanged(int mtu) {
                        // 设置MTU成功，并获得当前设备传输支持的MTU值
                    }
                });
            }
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                // 连接断开，isActiveDisConnected是主动断开还是被动断开（主线程）
                if(isActiveDisConnected){
                    setBleConnectState(false,8);
                }
                else {
                    setBleConnectState(false,0);
                }
            }
        });
    }
    public void DisConnect(){
        BleManager.getInstance().disconnect(this.mBleDevice);
        this.setBleConnectState(false,0);
    }

    public void MsgSend(byte[] msg){
        BleManager.getInstance().write(
                mBleDevice,
                SERVICE_UUID,
                CHARACTERISTIC_UUID,
                msg,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {}
                    @Override
                    public void onWriteFailure(BleException exception) {}
                });
    }

    public void MsgSend(String msg){
        BleManager.getInstance().write(
                mBleDevice,
                SERVICE_UUID,
                CHARACTERISTIC_UUID,
                msg.getBytes(),
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {}
                    @Override
                    public void onWriteFailure(BleException exception) {}
                });
    }

    public void MsgRecvStart()
    {
        BleManager.getInstance().notify(
                mBleDevice,
                SERVICE_UUID,
                CHARACTERISTIC_UUID,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {Log.i("DEBUG","onNotifySuccess");}
                    @Override
                    public void onNotifyFailure(BleException exception) {Log.i("DEBUG","onNotifyFailure");}
                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        // 打开通知后，设备发过来的数据将在这里出现

                        Log.i("NBIOT",hexToAscii(byteArrayToHexStr(data)));
                        Log.i("NBIOT","recv datalen = " + data.length);
                        mDevice.MsgProcess(data);
                    }
                });
    }

/*
    协议：
    APP->DEV
        NBIOT:  1030M
            Text:       由APP封装好消息格式 [Text]
            SET WAKE:   由APP封装好消息格式 [WAKE]
            SET SLEEP:  不发消息20s内自动进入Idel模式
        CMD:
            "RPOT"  ask for report
            "CXL[lightlevel+128]" set LightLevel/LightPower
    DEV->APP
        NBIOT:
            Text:   监听模式所有消息直接转发上位机
        CMD:
        *   "SYL[lightlevel+128]"   system report format
        *   "CFLT"  op confilt
*/

    public void MsgProcess(byte[] data){
        if(data != null && data.length >= 4){
            if(cmdMatch(data,"SYL") >= 0)
            {
                //解析LightLevel
                int ll = data[3] & 0xFF;
                if(ll >= 128 && ll <= (100+128)){
                    this.setLedPower(false,3);
                    this.setLedLevel(false,ll - 128);
                    Log.i("DEBUG","get report LedPower:"+ LedPower + "  LedLevel:" + LedLevel);
                }
                //  #define LED_ON         111      不改变LightLevel
                else if(ll == (111+128)){
                    this.setLedPower(false,3);
                    Log.i("DEBUG","get report LedPower:"+ LedPower + "  LedLevel:" + LedLevel);
                }
                // #define LED_OFF         121
                else if(ll == (121+128)){
                    this.setLedPower(false,0);
                    Log.i("DEBUG","get report LedPower:"+ LedPower + "  LedLevel:" + LedLevel);
                }
            }

            else if(cmdMatch(data,"CFLT") >= 0) {
                support.firePropertyChange("CFLT", 0, 0);
            }

            else if(cmdMatch(data,"+CSCON:0") >= 0) {
                setNBConnectState(false,0);
            }

            else if(cmdMatch(data,"+CSCON:1") >= 0) {
                setNBConnectState(false,3);
            }

            else if(cmdMatch(data,"+NSMI:SENT") >= 0) {
                setMsgSendFlag(1);
            }

            else if(cmdMatch(data,"+NNMI:") >= 0) {
                int pos = 0;
                String exp = "+NNMI:24,123456789";
                while(data[pos++] != (byte)exp.charAt(8));
                String Ds = hexToAscii(byteArrayToHexStr(data));
                String[] msgs= Ds.split(":");
                String[] msgus= msgs[1].split(",");
                int msglen = Integer.parseInt(msgus[0]);
                byte[] bs = new byte[msglen*2];
                System.arraycopy(data, pos, bs, 0, msglen*2);
//                String text = new String(hexStrToByteArray(asciiToHex(msgus[1])),Charset.forName("UNICODE"));
                String text = new String(bs,Charset.forName("UTF-8"));
                Log.i("BNIOT",hexUTF82String(text));
                //String text = hexUTF82String(byteArrayToHexStr(bs));
                addRecvQue(hexUTF82String(text),mDate.getTime());
            }
            else if(cmdMatch(data,"ERROR") >= 0) {
                if(getMsgSendFlag() == 0) { setMsgSendFlag(2); }
            }
        }
    }

    //来自C的原汁原味的暴力匹配 干就得了
    private int cmdMatch(byte[] data,String exp){
        int Scmdlen = data.length;
        int Dcmdlen = exp.length();
        Log.i("match","data.length = " + data.length + "  exp.length() = "+exp.length());
        int i = 0, j = 0, k = i;
        if(Scmdlen < Dcmdlen)
            return -1;
        while (i < Scmdlen && j < Dcmdlen) {
            if (data[i] == (byte) exp.charAt(j)) {
                ++i;
                ++j;
            }
            else {
                j = 0;
                i = ++k;//匹配失败，i从主串的下一位置开始，k中记录了上一次的起始位置
            }
        }
        if (j >= Dcmdlen) {
            return k;
        }
        else
            return -1;
    }

    public class UserChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            switch (evt.getPropertyName()) {
                case ("LPU"):{
                    if ((int) evt.getNewValue() == 1) {
                        cmd[0] = 'C';
                        cmd[1] = 'X';
                        cmd[2] = 'L';
                        cmd[3] = (byte) (111 + 128);
                        cmd[4] = '\r';
                        cmd[5] = '\n';
                        MsgSend(cmd);
                        Log.i("DEBUG","op set LedPower:1");
                    } else if ((int) evt.getNewValue() == 2 ){
                        cmd[0] = 'C';
                        cmd[1] = 'X';
                        cmd[2] = 'L';
                        cmd[3] = (byte) (121 + 128);
                        cmd[4] = '\r';
                        cmd[5] = '\n';
                        MsgSend(cmd);
                        Log.i("DEBUG","op set LedPower:2");
                    }
                    break;
                }

                case ("LLU"):{
                    int lightlevel = (int)evt.getNewValue();
                    cmd[0] = 'C';
                    cmd[1] = 'X';
                    cmd[2] = 'L';
                    cmd[3] = (byte) (lightlevel + 128);
                    cmd[4] = '\r';
                    cmd[5] = '\n';
                    MsgSend(cmd);
                    setLedLevel(false,lightlevel);
                    break;
                }

                //蓝牙连接 完成
                case ("CSU"):{
                    if ((int) evt.getNewValue() == 1 && (int) evt.getOldValue() == 0) { Scan_Connect(); }
                    else if ((int) evt.getNewValue() == 2 && (int) evt.getOldValue() == 3) { DisConnect(); }
                    break;
                }

                case ("CSD"):{
                    if ((int) evt.getNewValue() == 3) {MsgRecvStart();askReport();}
                }

                case ("NPU"):{
                    if((int)evt.getNewValue() == 3) {
                        //wake nbiot
                    }
                    else {
                        //sleep nbiot
                    }
                    break;
                }
            }
        }
    }

    /**
     * @Title:bytes2HexString
     * @Description:字节数组转16进制字符串
     * @param b
     * 字节数组
     * @return 16进制字符串
     * @throws
     */
    public static String bytes2HexString(byte[] b) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            result.append(String.format("%02X",b[i]));
        }
        return result.toString();
    }

    /**
     * @Title:hexString2Bytes
     * @Description:16进制字符串转字节数组
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static byte[] hexString2Bytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = Integer.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }


    /**
     * @Title:string2HexUTF8
     * @Description:字符UTF8串转16进制字符串
     * @param strPart
     * 字符串
     * @return 16进制字符串
     * @throws
     */
    public static String string2HexUTF8(String strPart) {

        return string2HexString(strPart,"UTF-8");
    }

    /**
     * @Title:string2HexUTF8
     * @Description:字符UTF-16LE串转16进制字符串,此UTF-16LE等同于C#中的Unicode
     * @param strPart
     * 字符串
     * @return 16进制字符串
     * @throws
     */
    public static String string2HexUTF16LE(String strPart) {

        return string2HexString(strPart,"UTF-16LE");
    }

    /**
     * @Title:string2HexUnicode
     * @Description:字符Unicode串转16进制字符串
     * @param strPart
     * 字符串
     * @return 16进制字符串
     * @throws
     */
    public static String string2HexUnicode(String strPart) {

        return string2HexString(strPart,"Unicode");
    }
    /**
     * @Title:string2HexGBK
     * @Description:字符GBK串转16进制字符串
     * @param strPart
     * 字符串
     * @return 16进制字符串
     * @throws
     */
    public static String string2HexGBK(String strPart) {

        return string2HexString(strPart,"GBK");
    }

    /**
     * @Title:string2HexString
     * @Description:字符串转16进制字符串
     * @param strPart 字符串
     * @param tochartype hex目标编码
     * @return 16进制字符串
     * @throws
     */
    public static String string2HexString(String strPart,String tochartype) {
        try{
            return bytes2HexString(strPart.getBytes(tochartype));
        }catch (Exception e){
            return "";
        }
    }

    ///////////////////////////////////////////////////
    /////////////////////////////////////////////////

    /**
     * @Title:hexUTF82String
     * @Description:16进制UTF-8字符串转字符串
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static String hexUTF82String(String src) {

        return hexString2String(src,"UTF-8","UTF-8");
    }

    /**
     * @Title:hexUTF16LE2String
     * @Description:16进制UTF-8字符串转字符串，,此UTF-16LE等同于C#中的Unicode
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static String hexUTF16LE2String(String src) {

        return hexString2String(src,"UTF-16LE","UTF-8");
    }

    /**
     * @Title:hexGBK2String
     * @Description:16进制GBK字符串转字符串
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static String hexGBK2String(String src) {

        return hexString2String(src,"GBK","UTF-8");
    }

    /**
     * @Title:hexUnicode2String
     * @Description:16进制Unicode字符串转字符串
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static String hexUnicode2String(String src) {
        return hexString2String(src,"Unicode","UTF-8");
    }

    /**
     * @Title:hexString2String
     * @Description:16进制字符串转字符串
     * @param src
     * 16进制字符串
     * @return 字节数组
     * @throws
     */
    public static String hexString2String(String src,String oldchartype, String chartype) {
        byte[] bts=hexString2Bytes(src);
        try{if(oldchartype.equals(chartype))
            return new String(bts,oldchartype);
        else
            return new String(new String(bts,oldchartype).getBytes(),chartype);
        }
        catch (Exception e){

            return"";
        }
    }
}
