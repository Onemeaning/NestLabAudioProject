package com.meanlam.instantmessage.socket;

import android.media.AudioRecord;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class SendMessageThread extends Thread{

    private Socket           socket;
    private boolean          isRecord;
    private OutputStream     os;
    private AudioRecord      mAudioRecord;
    private byte[]           mBuffer;
    private int              BUFFER_SIZE ;
    private FileOutputStream mFileOutputStream;

    public SendMessageThread(Socket socket, boolean isRecord, OutputStream os, AudioRecord mAudioRecord,byte[] mBuffer,int BUFFER_SIZE,FileOutputStream mFileOutputStream){
        this.socket=socket;
        this.isRecord = isRecord;
        this.os = os;
        this.mAudioRecord = mAudioRecord;
         this.mBuffer = mBuffer;
         this.BUFFER_SIZE = BUFFER_SIZE;
        this.mFileOutputStream = mFileOutputStream;
    }
    @Override
    public void run(){
        while(isRecord){
            try{
                int read = mAudioRecord.read(mBuffer,0,BUFFER_SIZE);
                if(read>0)
                {
                    Log.i("MEAN", "startRecord: "+ Arrays.toString(mBuffer));
                    os.write(mBuffer,0,read);
                    os.flush();

                    //   读取成功，写入文件，获取写入到手机内存中去
                    mFileOutputStream.write(mBuffer);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
