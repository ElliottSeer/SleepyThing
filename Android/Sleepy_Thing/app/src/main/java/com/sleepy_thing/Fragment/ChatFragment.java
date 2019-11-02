package com.sleepy_thing.Fragment;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sleepy_thing.Adapter.ChatAdapter;
import com.sleepy_thing.Base.BaseFragment;
import com.sleepy_thing.Base.Chat;
import com.sleepy_thing.R;
import com.sleepy_thing.Utils.SQLiteDBHelper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static com.sleepy_thing.MainActivity.Database;
import static com.sleepy_thing.MainActivity.Huyou_dev;
import static com.sleepy_thing.MainActivity.mDate;
import static com.sleepy_thing.MainActivity.mDevice;

public class ChatFragment extends BaseFragment {

    private TextView tvName;
    private RecyclerView rvChat;
    private EditText etInput;
    private Button btnSend;
    private List<Chat> list;
    private static final int MAX_CHAT = 50;
    private ChatAdapter chatAdapter;

    @Override
    protected View onCreateOwnView(LayoutInflater inflater, ViewGroup container) {
        View mView = inflater.inflate(R.layout.chat_layout, container, false);

        tvName = mView.findViewById(R.id.tvName);

        if(Huyou_dev.getUname().equals("小金毛的名字")){
            tvName.setText("可怜兮兮的没有名字的小金毛");
        }
        else {
            tvName.setText(Huyou_dev.getUname());
        }

        rvChat = mView.findViewById(R.id.rvChat);
        etInput = mView.findViewById(R.id.etInput);
        btnSend = mView.findViewById(R.id.btnSend);

        list = new ArrayList<Chat>();
        chatAdapter = new ChatAdapter(this.getContext(),list);
        rvChat.setLayoutManager(new LinearLayoutManager(this.getContext()));
        rvChat.setAdapter(chatAdapter);

        if(mDevice.getBleConnectState() != 3) {
            etInput.setEnabled(false);
            etInput.setText("");
            btnSend.setEnabled(false);
        }
        else {
            etInput.setEnabled(true);
            btnSend.setEnabled(true);
        }

        Init();

        mDevice.addPropertyChangeListener(new ChatChangeListener());

        return mView;
    }

    public void Init(){
        //从数据库中取至多MAX_CHAT个数据先显示出来
        List<Chat> templist = loadfromDB();
        Log.i("CHAT","loadfromDB" + list.size());
        for(int i = 0; i<templist.size();i++)
        {
            list.add(templist.get(i));
            Log.i("CHAT",list.get(i).getText());
            chatAdapter.notifyItemChanged(list.size()-1);
            rvChat.scrollToPosition(list.size()-1);
        }


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnSend.isEnabled()){
                    String text = etInput.getText().toString();
                    if(!TextUtils.isEmpty(text))
                    {
                        mDevice.SendChat(text,mDate.getTime());
                    }
                }
            }
        });
    }

    public List<Chat> loadfromDB()
    {
        List<Chat> list = new ArrayList<Chat>();;
        Cursor cursor = Database.query(SQLiteDBHelper.TABLE_CHAT, null,
                null, null,
                null, null, "Mid DESC", Integer.toString(MAX_CHAT));
        if (cursor.moveToLast()) {
             do{
                Chat chat = new Chat(cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getInt(3));
                list.add(chat);
            }while (cursor.moveToPrevious());
        }
        cursor.close();
        return list;
    }

    public class ChatChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            switch (evt.getPropertyName()) {
                case ("RECHAT"):{
                    while (!mDevice.getRecvQue().isEmpty()){
                        Chat chat = new Chat(mDevice.getRecvQue().poll());  //移除并返回头部
                        if(list.size() >= MAX_CHAT) {
                            list.remove(0);
                            chatAdapter.notifyItemRemoved(0);
                            chatAdapter.notifyItemRangeChanged(0, list.size());
                        }
                        list.add(chat);
                        chat.addtoDB(); //显示成功后加入数据库
                        chatAdapter.notifyItemChanged(list.size() - 1);
                        rvChat.scrollToPosition(list.size()-1);
                    }
                    break;
                }

                case ("CSD"):{
                    if((int)evt.getNewValue() == 3)
                    {
                        etInput.setEnabled(true);
                        btnSend.setEnabled(true);
                    }
                    else {
                        etInput.setEnabled(false);
                        etInput.setText("");
                        btnSend.setEnabled(false);
                    }
                    break;
                }

                case ("MSGSD"):{
                    switch ((int)evt.getNewValue()){
                        case (1):
                        {
                            Chat chat = mDevice.getSendChat();
                            if(list.size() >= MAX_CHAT) {
                                list.remove(0);
                                chatAdapter.notifyItemRemoved(0);
                                chatAdapter.notifyItemRangeChanged(0, list.size());
                            }
                            list.add(chat);
                            chat.addtoDB(); //显示成功后加入数据库
                            chatAdapter.notifyItemChanged(list.size() - 1);
                            rvChat.scrollToPosition(list.size()-1);
                            btnSend.setEnabled(true);
                            etInput.setEnabled(true);
                            etInput.setText("");
                            break;
                        }
                        case (0):
                        {
                            btnSend.setEnabled(false);
                            etInput.setEnabled(false);
                            Toast.makeText(SetFragment.mContext,"Sending Message ->->->",Toast.LENGTH_SHORT)
                                    .show();
                            break;
                        }
                        case (2):
                        {
                            btnSend.setEnabled(true);
                            etInput.setEnabled(true);
                            Toast.makeText(SetFragment.mContext,"Message Send Error",Toast.LENGTH_SHORT)
                                    .show();
                            //清除刚刚要发送的消息 防止重复添加至发送队列
                            break;
                        }
                        case (3):
                        {
                            btnSend.setEnabled(true);
                            etInput.setEnabled(true);
                            Toast.makeText(SetFragment.mContext,"Message Send Timeout",Toast.LENGTH_SHORT)
                                    .show();
                            //清除刚刚要发送的消息 防止重复添加至发送队列
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }
}
