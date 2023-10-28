package com.example.myapplication;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReadWriteLock;

public class MainActivity extends AppCompatActivity {

    EditText editTextIPAddress,editTextPort;//输入IP地址,端口号
    Button buttonConnect;//连接按钮
    TextView textViewRecv;//接收显示数据
    CheckBox checkBoxRecHex;//16进制显示
    Button buttonClearRec;//清除接收
    Socket socket;
    InputStream inputStream;//输入流
    byte[] RevBuff = new byte[1460];//缓存数据

    Button buttonSendData;//发送数据
    EditText editTextSendData;//输入框
    OutputStream outputStream;//输出流

    CheckBox checkBoxSendHex;//16进制发送
    MyHandler myHandler;//使用Handler更新控件
    Button buttonClearSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myHandler = new MyHandler();
        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setText("连接");
        editTextIPAddress = findViewById(R.id.editTextIPAddress);
        editTextPort = findViewById(R.id.editTextPort);
        textViewRecv = findViewById(R.id.textViewRecv);
        editTextSendData = findViewById(R.id.editTextSendData);
        checkBoxRecHex = findViewById(R.id.checkBoxRecHex);
        checkBoxSendHex = findViewById(R.id.checkBoxSendHex);
        buttonSendData = findViewById(R.id.buttonSendData);
        buttonClearSend = findViewById(R.id.buttonClearSend);
        buttonClearRec = findViewById(R.id.buttonClearRec);
        buttonClearRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewRecv.setText("");
            }
        });

        buttonClearSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextSendData.setText("");
            }
        });

        buttonSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(checkBoxSendHex.isChecked()){//16进制发送
                                byte[] byt = hexStringToByteArray(editTextSendData.getText().toString());
                                if (byt!=null){
                                    outputStream.write(byt);
                                }
                            }
                            else{
                                outputStream.write(editTextSendData.getText().toString().getBytes());
                            }
                        }catch (Exception e){
                            Log.e("err", e.toString() );
                        }
                    }
                }).start();
            }
        });

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonConnect.getText()=="连接"){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                            try{
                                socket = new Socket(editTextIPAddress.getText().toString(),Integer.valueOf(editTextPort.getText().toString()));
                                if(socket.isConnected()){
                                    msg.what = 1;//设置消息变量的 what 变量值 为1
                                    inputStream = socket.getInputStream();//获取数据流通道
                                    outputStream = socket.getOutputStream();//获取输出流
                                    Recv();//调用接收函数
                                }
                            }catch (Exception e){
                                msg.what = 0;//设置消息变量的 what 变量值 为0
                            }
                            myHandler.sendMessage(msg);//插入消息队列
                        }
                    }).start();
                }
                else{
                    try{ socket.close(); }catch (Exception e){} //关闭连接
                    try{ inputStream.close(); }catch (Exception e){}
                    buttonConnect.setText("连接");//按钮显示连接
                }
            }
        });
    }


    public void Recv(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket!= null && socket.isConnected()){
                    try{
                        int Len = inputStream.read(RevBuff);//获取数据
                        if(Len!=-1){
                            Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                            msg.what = 3;//设置消息变量的 what 变量值 为3
                            msg.arg1 = Len;//接收的数据个数
                            msg.obj = RevBuff;//传递数据
                            myHandler.sendMessage(msg);//插入消息队列
                        }
                        else{//连接异常断开
                            Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                            msg.what = 0;//设置消息变量的 what 变量值 为0
                            myHandler.sendMessage(msg);//插入消息队列
                            break;
                        }
                    }catch (Exception e){//连接异常断开
                        Message msg = myHandler.obtainMessage();//从消息队列拉取个消息变量
                        msg.what = 0;//设置消息变量的 what 变量值 为0
                        myHandler.sendMessage(msg);//插入消息队列
                        break;
                    }
                }
            }
        }).start();
    }

    //Handler
    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    Toast.makeText(MainActivity.this,"连接出错",Toast.LENGTH_SHORT).show();
                    buttonConnect.setText("连接");//按钮显示连接
                    break;
                case 1:
                    buttonConnect.setText("断开");//按钮显示断开
                    break;
                case 3:
                    byte[] Buffer = new byte[msg.arg1];//创建一个数组
                    System.arraycopy((byte[])msg.obj, 0,Buffer , 0, msg.arg1);//拷贝数据
                    if(checkBoxRecHex.isChecked()){//16进制显示
                        textViewRecv.append(byteToHexStr(Buffer));
                    }else{
                        textViewRecv.append(new String(Buffer));//显示在textView
                    }
                    break;
                default: break;
            }
        }
    }
    /**
     * 16进制byte转16进制String--用空格隔开
     * @param bytes
     * @return
     */
    public static String byteToHexStr(byte[] bytes) {
        String str_msg = "";
        for (int i = 0; i < bytes.length; i++){
            str_msg = str_msg + String.format("%02X",bytes[i])+" ";
        }
        return str_msg;
    }

    /***
     *"2B44EFD9" --> byte[]{0x2B, 0x44, 0xEF,0xD9}
     * @param hexString
     * @return
     */
    public static byte[] hexStringToByteArray(String hexString) {
        StringBuilder sb = null;
        hexString = hexString.replaceAll(" ", "");

        if ((hexString.length()%2)!=0) {//数据不是偶数
            sb = new StringBuilder(hexString);//构造一个StringBuilder对象
            sb.insert(hexString.length()-1, "0");//插入指定的字符串
            hexString = sb.toString();
        }

        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            if ((
                    (hexString.charAt(i)>='0' && hexString.charAt(i)<='9') ||
                            (hexString.charAt(i)>='A' && hexString.charAt(i)<='F') ||
                            (hexString.charAt(i)>='a' && hexString.charAt(i)<='f')
            )&&
                    (hexString.charAt(i+1)>='0' && hexString.charAt(i+1)<='9') ||
                    (hexString.charAt(i+1)>='A' && hexString.charAt(i+1)<='F') ||
                    (hexString.charAt(i+1)>='a' && hexString.charAt(i+1)<='f')){
                // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
                bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
                        .digit(hexString.charAt(i+1), 16));
            }
            else return null;
        }
        return bytes;
    }
}
