package com.meanlam.instantmessage.socket;

import android.util.Log;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;
import java.nio.CharBuffer;

public class ReceiveMessageThread extends Thread {
    private Socket socket;
    private String msg ;
    public ReceiveMessageThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Reader reader = new InputStreamReader(socket.getInputStream());
                CharBuffer charbuffer = CharBuffer.allocate(8192);
                int index = -1;
                Log.i("MEAN1", "进入读取服务器发送的数据：");
                while ((index = reader.read(charbuffer)) != -1) {
                    charbuffer.flip();//设置从0到刚刚读取到的位置
                    msg = charbuffer.toString();
                    Log.i("MEAN1", charbuffer.toString());
                    if (charbuffer.toString().contains("服务器"))
                    {
                        reader.close();
                        break;
                    }
                }
                Log.i("MEAN1", "读取服务器发送的数据完成");
                break;
            } catch (Exception e) {
                Log.i("MEAN1", "进入读取服务器发送的数据异常：");
                e.printStackTrace();
                break;
            }
        }
    }

    public String getMsgFromServer()
    {
        run();
        return msg;
    }
}

