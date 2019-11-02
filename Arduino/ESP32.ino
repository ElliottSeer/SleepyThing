#include <Ticker.h>
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <string.h>

using namespace std;

#define KEY_PIN         23
#define LEDC_PIN        16

#define KEY_PRESS       1

#define LEDC_FREQ       200     // 频率
#define LEDC_CHANNEL    0       // 通道
#define LEDC_RESOLUTION 12      //4096 (1-LEDC_MAX_VAL)
#define LEDC_MAX_VAL    600     // 最大百分比（功率限制）
#define LEDxAPP         (LEDC_MAX_VAL/100)
#define LED_ON         111
#define LED_OFF        121
#define LED_SLOW_TIME   500
#define LEDC_Change_Limit   LEDC_MAX_VAL/3

#define SR1_RXD          27
#define SR1_TXD          14
#define NBIOT_EN_PIN    12

#define key_state_0     0
#define key_state_1     1
#define key_state_2     2
#define key_state_3     3

#define key_no          0
#define key_click       1
#define key_double      2
#define key_long        3
#define key_ledc_step   2

#define key_time_clip       20  //ms
#define ledc_time_clip      10  //ms
#define sysctrl_time_clip   1000   //ms
#define nbp_time_clip      50 //ms
#define BLE_SLEEP_TIME      600 //s

#define CMD_CL          1
#define CMD_WK          2
#define CMD_RP          3
#define CMD_SY          4

#define BLE_CONNECT_TIMEOUT       10000  //ms
// #define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
// #define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
// #define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

static BLECharacteristic *pCharacteristic;
static BLEService* pService;
static BLEServer *pServer;
static int BleConnectCnt;

static uint8_t BleRx[1000]; //500中文 max
static int BleRecvLen = 0;
static int BleRecvFlag = 0;

static uint8_t NBRx[1000];  //500中文
static int NBRecvFlag = 0;  //消息读取后 必须NBRecvFlag = 0；NBRecvLen = 0；
static int NBRecvLen = 0;
static int NBCmdRetry = 0;

static int AppLevel = 0;
static int LedcLevel = LEDC_MAX_VAL/2;  //需要和APP的百分比进行换算 todo
static int LedcLevelCrt = LedcLevel;
static int LedcLevelLast = LedcLevel;   //上一次关灯前的光强

bool LedPower;
bool BleConnected =  0;
bool BlePower;
bool NBCSCON;
bool LedBreath;

Ticker tKeyManager;
Ticker tLedcController;
Ticker tSysController;
Ticker tNBProcesser;

static unsigned char KeyVal = 0;
static unsigned char KeyValCrt = 0;

static int BleNoContCnt = 0;
static int BleNoUserCnt = 0;   //无响应计时
static int LedNoUserCnt = 0;
static int NbNoUserCnt = 0;

//NBIOT 每隔20s老化一次 即断开连接 在过程中如有数据发送则唤醒NB IOT 硬件将主动连续为NBIOT唤醒3次 第4次NBIOT自动睡眠

/*
    协议：
    APP->DEV 
        NBIOT:  1029M
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

void BleRecvProcess(uint8_t* data, uint16_t len)
{
    if(len >= 4){
        if(data[0] == 'R' && data[1] == 'P' && data[2] == 'O' && data[3] == 'T')
        {
            byte RPOT[5] = {'S','Y','L','N','\0'};
            RPOT[3] = AppLevel + 128;
            for(int i = 0; i<4;i++){Serial.write(RPOT[i]);}
            BleMsgNotify(RPOT,5);
        }
        else if(data[0] == 'C' && data[1] == 'X' && data[2] == 'L')
        {
            //CONFILT JUDGE
            if(digitalRead(KEY_PIN) == KEY_PRESS)
            {
                byte CFLT[5] = {'C','F','L','T','\0'};
                for(int i = 0; i<5;i++){Serial.write(CFLT[i]);}
                BleMsgNotify(CFLT,5);
            }
            else
            {
                int AppLightLevel = data[3] - 128;
                if(AppLightLevel == LED_ON)
                {
                    LedOn();
                }
                else if(AppLightLevel == LED_OFF)
                {
                    LedOff();
                }
                else if(AppLightLevel >= 0 && AppLightLevel <= 100)
                {
                    //CHANGE LIGHT
                    AppLevel = AppLightLevel;
                    LedcLevel = AppLevel_to_LedcLevel(AppLevel);
                }
            }
        }
        else    //AT+NMGS
        {
            //数据格式在APP中封装 这里不需要转码 直接转发给NBIOT即可
            Serial.println("|CmdMatch"+CmdMatch(data,len,"AT+NMGS",7));
            Serial.print("|S0sd");
            Serial.write(data,len);
            Serial1.write(data,len);
        }
    }
}

int16_t CmdMatch(const uint8_t* Scmd, const int16_t Scmdlen, const uint8_t* Dcmd, const int16_t Dcmdlen)    //返回匹配位置 没有返回 -1
{
    int i = 0, j = 0, k = i;
    if(Scmdlen < Dcmdlen)
        return -1;
	while (i < Scmdlen && j < Dcmdlen) {
		if (Scmd[i] == Dcmd[j]) {
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

int16_t CmdMatch(const uint8_t* Scmd, const int16_t Scmdlen, std::string Dcmd, const int16_t Dcmdlen)    //返回匹配位置 没有返回 -1
{
    int i = 0, j = 0, k = i;
    if(Scmdlen < Dcmdlen)
        return -1;
	while (i < Scmdlen && j < Dcmdlen) {
		if (Scmd[i] == Dcmd[j]) {
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

bool GetCmdRet(const uint8_t* Dcmd, const int16_t Dcmdlen)
{
    bool ret;
    while (Serial1.available() > 0)
    {
        NBRx[NBRecvLen++] = char(Serial1.read());  //每次读一个char字符，并相加
        delay(2);   //ms
    }
    if (NBRx[NBRecvLen] == '\n' && NBRx[NBRecvLen-1] == '\r')
    { 
        ++NBRecvLen;                    //修正数组长度
        int pos = CmdMatch(NBRx, NBRecvLen, Dcmd, Dcmdlen);
        if(pos == -1){
            ret = false;
        }
        else if(pos > 0){
            ret = true;
        }
        NBRecvLen = 0;                  //准备开始下一轮消息周期
    }
    return ret;
}

bool GetCmdRet(std::string Dcmd, const int16_t Dcmdlen)
{
    bool ret;
    while (Serial1.available() > 0)
    {
        NBRx[NBRecvLen++] = char(Serial1.read());  //每次读一个char字符，并相加
        Serial.print(NBRx[NBRecvLen]);
        delay(50);   //ms
    }
    if (NBRx[NBRecvLen] == '\n' && NBRx[NBRecvLen-1] == '\r')
    { 
        ++NBRecvLen;                    //修正数组长度
        for(int i = 0; i<NBRecvLen;i++){Serial.write(NBRx[i]);}
        int pos = CmdMatch(NBRx, NBRecvLen, Dcmd, Dcmdlen);
        if(pos == -1){
            ret = false;
        }
        else if(pos > 0){
            ret = true;
        }
        NBRecvLen = 0;                  //准备开始下一轮消息周期
    }
    return ret;
}

int NbInIt()
{
    Serial1.begin(9600, SERIAL_8N1, SR1_RXD, SR1_TXD);
    //设备入网配置
    Serial1.write("AT\r\n");
    delay(500);
    Serial1.write("AT\r\n");
    delay(500);
    if(!GetCmdRet("OK",2)){return 1;}
    Serial1.write("AT+CFUN=1\r\n");     //打开射频
    delay(500);
    if(!GetCmdRet("OK",2)){return 2;}
    
    Serial1.write("AT+CGATT=1\r\n");    //激活网络   重复下发 AT+CGATT? 直到收到 +CGATT:1   OK
    delay(500);
    NBCmdRetry = 10;
    while(!GetCmdRet("+CGATT:1",8) && NBCmdRetry>0){
        delay(500);
        NBCmdRetry--;
    }

    Serial1.write("AT+CEREG=1\r\n");     //查询网络是否注册   重复下发 AT+CEREG? 直到收到 +CEREG:0,1   OK
    delay(500);
    NBCmdRetry = 10;
    while(!GetCmdRet("+CEREG:0,1",10) && NBCmdRetry>0){
        delay(500);
        NBCmdRetry--;
    }

    Serial1.write("AT+CSCON=1\r\n");    //自动上报网络状态
    delay(500);
    if(!GetCmdRet("OK",2)){return 5;}
    Serial1.write("AT+NSMI=1\r\n");     //开启发送消息通知  OK +NSMI:SENT
    delay(500);
    if(!GetCmdRet("OK",2)){return 6;}
    Serial1.write("AT+NNMI=1\r\n");     //开启新消息通知    OK  通知+数据 
    delay(500);
    if(!GetCmdRet("OK",2)){return 7;}
    //设备连接服务器
    Serial1.write("AT+NCDP=117.60.157.137,5683\r\n");
    delay(500);
    if(!GetCmdRet("OK",2)){return 8;}
    Serial1.write("AT+CFUN=1\r\n");
    delay(500);
    if(!GetCmdRet("OK",2)){return 9;}
    return 0;
}

int AppLevel_to_LedcLevel(int AppLevel)
{
    return AppLevel * LEDxAPP;
}

int LedcLevel_to_AppLevel(int LedcLevel)
{
    return LedcLevel/LEDxAPP;
}

void LedOff()
{
    LedPower = 0;
    ledcDetachPin(LEDC_PIN);  
    LedcLevelLast = LedcLevelCrt;
    AppLevel = LED_OFF;
    if(BleConnected){
        byte RPOT[4] = {'S','Y','L','N'};
        RPOT[3] = AppLevel + 128;
        BleMsgNotify(RPOT,4);
    }
}

void LedOn()
{
    LedPower = 1;
    ledcAttachPin(LEDC_PIN, LEDC_CHANNEL);
    LedcLevel = LedcLevelLast;
    AppLevel = LedcLevel_to_AppLevel(LedcLevelLast);
    if(BleConnected){
        byte RPOT[4] = {'S','Y','L','N'};
        RPOT[3] = AppLevel + 128;
        BleMsgNotify(RPOT,4);
    }
}

class MyServerCallbacks: public BLEServerCallbacks 
{
    void onConnect(BLEServer* pServer) 
    {
        BleConnected = true;
        uint8_t  onConnect[10] = {'C','C','C','C','C','C','C','C','C','\0'};
        BleMsgNotify(onConnect,10);
    };
    void onDisconnect(BLEServer* pServer) 
    {
        BleConnected = false;
    }
};

//蓝牙收到信息后的回调函数
class MyCallbacks: public BLECharacteristicCallbacks 
{
    void onWrite(BLECharacteristic *pCharacteristic) 
    {
        std::string rxStr = pCharacteristic->getValue();
        if (rxStr.length() > 0) {
            /*relase*/  //debug 数据没发出去
            for (int i = 0; i < rxStr.length(); i++){
                //Serial.print(rxStr[i]);         //*
                //Serial.flush();
                BleRx[BleRecvLen++] = rxStr[i];
                if(BleRecvLen >= 2 && BleRx[BleRecvLen-2] == '\r' && BleRx[BleRecvLen-1] == '\n'){
                    
                    BleRx[BleRecvLen++] = '\0';
                    
                    BleRecvFlag = 1;
                    Serial.print("|Blrc:");
                    for(int i = 0; i<BleRecvLen;i++){Serial.print(BleRx[i]);}
                    Serial.flush();
                    BleRecvProcess(BleRx,BleRecvLen);
                    BleRecvFlag = 0;
                    BleRecvLen = 0;
                }
            }
        }
    }
};

int BleMsgNotify(std::string Msg)
{
    if (BleConnected) {
        pCharacteristic->setValue(Msg);
        pCharacteristic->notify();
        Serial.print("BLE send:");
        for (int i = 0;i<Msg.length();i++)
            Serial.print(Msg[i]);
            Serial.println(" ");
		delay(5); // bluetooth stack will go into congestion, if too many packets are sent
    }
}

int BleMsgNotify(uint8_t* Msg, uint16_t MsgLen)
{
    if (BleConnected) {
        pCharacteristic->setValue(Msg,MsgLen);
        pCharacteristic->notify();
        Serial.print("BLE send:");
        Serial.write(Msg, MsgLen);
        Serial.print('\n');
		delay(5); // bluetooth stack will go into congestion, if too many packets are sent
    }
}

void setup()
{
    Serial.begin(115200);

    //KEY
    pinMode(KEY_PIN, INPUT);

    //LEDC
    LedPower = 0;
    ledcSetup(LEDC_CHANNEL, LEDC_FREQ, LEDC_RESOLUTION); // 设置通道
    ledcAttachPin(LEDC_PIN, LEDC_CHANNEL);              // 将通道与对应的引脚连接
    ledcWrite(LEDC_CHANNEL, 0);

    //BLE
    BlePower = 1;
    BLEDevice::init("Sleepy_Thing"); // Give it a name
    pServer = BLEDevice::createServer();                 // Create the BLE Server
    pServer->setCallbacks(new MyServerCallbacks());                 // Create the BLE Service
    pService = pServer->createService(SERVICE_UUID);    // Create a BLE Characteristic
    pCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID,
                        BLECharacteristic::PROPERTY_WRITE  |
                        BLECharacteristic::PROPERTY_NOTIFY
                        );
    pCharacteristic->addDescriptor(new BLE2902());
    pCharacteristic->setCallbacks(new MyCallbacks());       
             
    pService->start();                                              // Start the service
    pServer->getAdvertising()->start();                             // Start advertising

    //NBIOT
    delay(3000);
    Serial.println(NbInIt());
    
    //以设备划分任务
    tKeyManager.attach_ms(key_time_clip, KeyManager);               //按键处理函数 控制蓝牙开关；灯开关、亮度预设*
    tLedcController.attach_ms(ledc_time_clip, LedcController);      //根据预设值 调整LEDC 发送调整信息至上位机（灯光信息同步）
    tSysController.attach_ms(sysctrl_time_clip, SysController);     //控制系统 如ESP32睡眠、唤醒；NB睡眠、唤醒（NB信息上报）
    tNBProcesser.attach_ms(nbp_time_clip, NBProcesser);          //定时查询NB串口，如有直接发送至上位机
     
}

void loop()
{
    // Empty. Things are done in Tasks.
}

void KeyManager()               //按键处理函数 控制蓝牙开关；灯开关、亮度预设*
{
    static bool LightInc = 1;     //长按时变亮  耗时(LEDC_MAX_VAL*key_time_clip/key_ledc_step)/1000 要小于电容按键放电时间！
    static bool LightCtrlEn = 1;       //到达边界时停用按键 保护电容

    KeyVal = key_read();
    if(KeyValCrt != KeyVal)
    {
        if(KeyVal ==  key_no && KeyValCrt == key_long){
            if(BleConnected){
                byte RPOT[4] = {'S','Y','L','N'};
                RPOT[3] = AppLevel + 128;
                BleMsgNotify(RPOT,4);
            }
        }

        KeyValCrt = KeyVal;
        if(KeyValCrt == key_long)
            LightCtrlEn = 1;    //再次长按时启用控制
    }
    /*
    长按：
    关灯状态下：由0直接上升 同时唤醒系统 打开蓝牙
    开灯状态：在原光照基础上改变
    */
    if(KeyVal == key_long)
    {
        if(LedPower == 0)
        {
            LedPower = 1;
            ledcAttachPin(LEDC_PIN, LEDC_CHANNEL);
            LedcLevel = 0;
        }
        if(BlePower == 0)
        {
            pService->start();  
            pServer->getAdvertising()->start();  
        }
        if(LightCtrlEn)
        {
            if(LightInc)
            {
                LedcLevel+=key_ledc_step;
                if(LedcLevel >= LEDC_MAX_VAL)
                {
                    LedcLevel = LEDC_MAX_VAL;
                    LightInc = 0;
                    LightCtrlEn = 0;
                }
            }
            else
            {
                LedcLevel-=key_ledc_step;
                if(LedcLevel <= 0)
                {
                    LedcLevel = 0;
                    LightInc = 1;
                    LightCtrlEn = 0;
                }
            }
        } 
    }
    /*
    双击：
    关灯状态下：打开灯光 恢复上一次光强（如果存在的话，不存在就初始光强）打开蓝牙 等待连接(30s) 
    开灯状态：关灯，保存光强 不开始离开计时！ 
    */
    if(KeyVal == key_double)
    {
        if(LedPower == 0)
        {
            LedOn();
            if(BlePower == 0)
            {
                BlePower = 1;
                pService->start();
                pServer->getAdvertising()->start(); 
            }
        }
        else
        {
            LedOff();
        }
    }
    /*
    单：
    关灯关蓝牙状态下：打开蓝牙 等待连接（呼吸灯*10s 再按取消）连接完成时关闭呼吸灯
    开灯状态：打开蓝牙 等待连接
    */
    if(KeyVal == key_click)
    {
        if(BlePower == 0)
        {
            BlePower = 1;
            if(BleConnected == false)
            {
                pService->start();         
                pServer->getAdvertising()->start();
                BleNoContCnt = BLE_CONNECT_TIMEOUT/key_time_clip;
                // if(LedPower == false)
                // {
                //     while(LedPower == false && BleConnected == false && BleNoContCnt > 0)
                //     {
                //         //呼吸灯打开 等待BleConnected或超时，关闭呼吸灯;
                //         BleNoContCnt--;
                //         LedBreath = true;
                //     }
                //     LedBreath = false;
                // }
                // else
                // {
                //     while(BleConnected == false && BleNoContCnt > 0);{BleNoContCnt--;}
                // }
            }

        }
    }
}

void SysController() //user_leave_check //控制系统 如ESP32睡眠、唤醒；NB睡眠、唤醒（NB信息上报）
{
    // NoUserCnt++;

    // if(NoUserCnt > BLE_SLEEP_TIME)  //NB 使用单独计时器
    // {
    //     BlePower = 0;
    //     //todo
    // }
}

//APP 控制有延迟 变量同步策略待更改
//关灯or自然滑动到0 开灯or自然滑动到起始 不在这里处理按键活动。在SYSCTRL里设置关灯等待时长
void LedcController()   //根据预设值 调整LEDC 发送调整信息至上位机（灯光信息同步）
{
    if(LedcLevelCrt != LedcLevel)
    {
        if(LedcLevelCrt - LedcLevel > LEDC_Change_Limit)
        {
            int ol = LedcLevelCrt;
            while(LedcLevelCrt != LedcLevel)
            {
                ledcWrite(LEDC_CHANNEL, LedcLevelCrt--);
                delay(LED_SLOW_TIME/(ol - LedcLevel));
            }
        }
        else if(LedcLevel - LedcLevelCrt > LEDC_Change_Limit)
        {
            int ol = LedcLevelCrt;
            while(LedcLevelCrt != LedcLevel)
            {
                ledcWrite(LEDC_CHANNEL, LedcLevelCrt++);
                delay(LED_SLOW_TIME/(LedcLevel - ol));
            }
        }
        else
        {
            ledcWrite(LEDC_CHANNEL, LedcLevel);
            LedcLevelCrt = LedcLevel;
        }
    }
}

void NBProcesser()     //定时查询NB串口，解析数据 注意每段数据以/r/n切分
{
    while (Serial1.available() > 0)
    {
        NBRx[NBRecvLen++] = char(Serial1.read());  //每次读一个char字符，并相加
        delay(2);   //ms
    }
    if (NBRecvLen >= 2 && NBRx[NBRecvLen-2] == '\r' && NBRx[NBRecvLen-1] == '\n')
    { 
        NBRx[++NBRecvLen] = '\0';                    //修正数组长度
        //++NBRecvLen;
        BleMsgNotify(NBRx,NBRecvLen);   //上报数据到BLE
        Serial.print("|S1rc:");
        for(int i = 0; i<NBRecvLen;i++){Serial.print(NBRx[i]);}
        NBRecvLen = 0;                  //准备开始下一轮消息周期
        //Serial1.flush();
    }
}
















































































/***************************************************************************
程序功能：一个按键的单击、双击、长按。三种按键方式，然后做不同的处理。
***************************************************************************/
static unsigned char key_driver(void)
{
    static unsigned char key_state_buffer1 = key_state_0;
    static unsigned int key_timer_cnt1 = 0;    //注意这里长按键时间可能很长
    unsigned char key_return = key_no;
    unsigned char key;
    
    key = digitalRead(KEY_PIN);  //read the I/O states

    switch(key_state_buffer1)
    {
        case key_state_0:
            if(key == KEY_PRESS)
                    key_state_buffer1 = key_state_1;
                    //按键被按下，状态转换到按键消抖和确认状态//
            break;
        case key_state_1:
            if(key == KEY_PRESS)
            {
                    key_timer_cnt1 = 0;
                    key_state_buffer1 = key_state_2;
                    //按键仍然处于按下状态
                    //消抖完成，key_timer开始准备计时
                    //状态切换到按下时间计时状态
            }
            else
                    key_state_buffer1 = key_state_0;
                    //按键已经抬起，回到按键初始状态
            break;  //完成软件消抖
        case key_state_2:
            if(key != KEY_PRESS)
            {
                    key_return = key_click;  //按键抬起，产生一次click操作
                    key_state_buffer1 = key_state_0;  //转换到按键初始状态
            }
            else if(++key_timer_cnt1 >= 1000/key_time_clip)  //按键继续按下，计时超过1000ms
            {
                    key_return = key_long;  //送回长按事件
                    key_state_buffer1 = key_state_3;  //转换到等待按键释放状态
            }
            break;
        case key_state_3:  //等待按键释放
            if(key != KEY_PRESS)  //按键释放
            {
                key_state_buffer1 = key_state_0;  //切回按键初始状态
                if(BleConnected){
                    byte RPOT[4] = {'S','Y','L','N'};
                    RPOT[3] = AppLevel + 128;
                    BleMsgNotify(RPOT,4);
                    for(int i = 0; i<4;i++){Serial.write(RPOT[i]);}
                }
            } 
            else    //长按时保持输出key_long 注释此段实现只输出一次 只能按3s？还有BUG
                key_return = key_long;
                key_state_buffer1 = key_state_2;
            break;
    }
    return key_return;
}

/***************************************************************************
函数功能：中层按键处理函数，调用底层函数一次，处理双击事件的判断， 返回上层正确的无键、单击、双击、长按四种状态
本函数由上层循环调用，间隔10ms
***************************************************************************/
unsigned char key_read(void)
{
    static unsigned char key_state_buffer2 = key_state_0;
    static unsigned char key_timer_cnt2 = 0;
    unsigned char key_return = key_no;
    unsigned char key;
    
    key = key_driver();
    
    switch(key_state_buffer2)
    {
        case key_state_0:
            if(key == key_click)
            {
                    key_timer_cnt2 = 0;  //第一次单击，不返回，到下个状态判断是否会出现双击
                    key_state_buffer2 = key_state_1;
            }
            else    //对于无键、长按，返回原事件
                    key_return = key;  
            break;
        case key_state_1:
            if(key == key_click)  //又一次单击，时间间隔小于500ms
            {
                    key_return = key_double;  //返回双击事件，回到初始状态
                    key_state_buffer2 = key_state_0;
            }
            else if(++key_timer_cnt2 >= (500/key_time_clip))
            {
                    //这里500ms内肯定读到的都是无键事件，因为长按大于1000ms
                    //在1s前底层返回的都是无键                                
                    key_return = key_click;  //500ms内没有再次出现单击事件，返回单击事件
                    key_state_buffer2 = key_state_0;  //返回初始状态
            }
            break;
    }
    return key_return;
}

