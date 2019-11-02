package com.sleepyclient;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Build.HOST;

public class MainActivity extends AppCompatActivity {

    private TextView tvState;
    private TextView tvRecv;
    private Button btClear;
    private Button btSend;
    private EditText etInput;
    private String receiveMsg;
    private PrintWriter printWriter;
    private BufferedReader in;
    private static final String TAG = "TAG";
    private ExecutorService mExecutorService = null;

    private static final String HOST = "NB平台IP";
    private static final int PORT = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvState = findViewById(R.id.tvState);
        tvRecv = findViewById(R.id.tvRecv);
        btClear = findViewById(R.id.btClear);
        btSend = findViewById(R.id.btSend);
        etInput = findViewById(R.id.etInput);

        TaskCenter.sharedCenter().connect(HOST,PORT);


        btClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskCenter.sharedCenter().send("注册包".getBytes());
            }
        });

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = etInput.getText().toString();
                if(!TextUtils.isEmpty(msg)){
                    TaskCenter.sharedCenter().send(msg.getBytes(Charset.forName("UTF-8")));
                    Log.i(TAG,msg.getBytes(Charset.forName("UTF-8")).toString());
                }
            }
        });

        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {
                tvState.setText("断开连接 " + HOST + ":" + PORT);
            }
        });
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {
                tvState.setText("连接成功 " + HOST + ":" + PORT);
            }
        });
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {
                tvRecv.setText(receicedMessage);
            }
        });

    }

}
