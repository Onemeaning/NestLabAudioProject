package com.meanlam.instantmessage;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client extends Thread
{

    // 定义一个Socket对象
    Socket socket = null;
    Client.sendMessThread sendMessThread;

    public Client(String host, int port)
    {

        try
        {
            // 需要服务器的IP地址和端口号，才能获得正确的Socket对象
            socket = new Socket(host, port);
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public synchronized  void sendMessage(byte[] mByteBuffer)
    {
        sendMessThread = new sendMessThread();
        sendMessThread.getBytesBuffer(mByteBuffer);
    }

    public synchronized  void setFlag()
    {
        sendMessThread.flag = false;
        Log.i("MEAN", "2: "+"已经执行"+ sendMessThread.flag);
    }

    @Override
    public void run()
    {
        // 客户端一连接就可以写数据个服务器了
        new sendMessThread().start();

        super.run();
        try
        {
            // 读Sock里面的数据
            InputStream s = socket.getInputStream();
            byte[] buf = new byte[2048];
            int len = 0;
            while ((len = s.read(buf)) != -1)
            {
                Log.i("MEAN", new String(buf, 0, len));
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // 往Socket里面写数据，需要新开一个线程
    public class sendMessThread extends Thread
    {
        public  boolean flag = true;
        public  byte[] mByteBuffer = null;

        public synchronized void getBytesBuffer(byte[] byteBuffer)
        {
            mByteBuffer = byteBuffer;
        }

        @Override
        public void run()
        {
            super.run();
            OutputStream os = null;
            try
            {
                os = socket.getOutputStream();

                Log.i("MEAN", "1: "+"已经执行"+ flag);
                while (flag)
                {
                    String in = Arrays.toString(mByteBuffer);
//                    Log.i("MEAN", in);
                    os.write(in.getBytes());
                    os.flush();
                }
                Log.i("MEAN", "3: "+"已经执行"+ flag);

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            try
            {
                os.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}