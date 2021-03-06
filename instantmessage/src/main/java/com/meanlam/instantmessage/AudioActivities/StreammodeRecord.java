package com.meanlam.instantmessage.AudioActivities;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.meanlam.instantmessage.Client;
import com.meanlam.instantmessage.R;
import com.meanlam.instantmessage.socket.ReceiveMessageThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StreammodeRecord extends AppCompatActivity {

    @BindView(R.id.bt_streammodeRecord)
    Button mButton;

    @BindView(R.id.tv_streammodeText)
    TextView showInfo;

    @BindView(R.id.bt_streammodePlay)
    Button mButtonPlay;

    //表示录音状态的，确保多线访问时状态量保持相同
    private volatile boolean         isRecording;
    private          ExecutorService mExecutorService;
    private          File            mAudioFile;
    private          long            startRecordTime,stopRecordTiem;
    private  byte[] mBuffer;
    private AudioRecord mAudioRecord;
    private FileOutputStream mFileOutputStream;

    private static final int  BUFFER_SIZE = 2048;

    private volatile boolean isPlaying;

    private   Client client ;
    OutputStream os;
    Socket socket;
    InputStream is = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streammode_record);

        ButterKnife.bind(this);
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];

        Toolbar toolbar =  findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);

    }

    @OnClick(R.id.bt_streammodeRecord)
    public void start()
    {
        if (isRecording)
        {
            mButton.setText("开始录音");
            isRecording = false;
            mButton.setBackgroundResource(R.color.colorPrimary);
        }
        else
        {
            mButton.setText("停止录音");
            mButton.setBackgroundResource(R.color.colorAccent);

            isRecording = true;
            //提交后台任务，执行开始录音逻辑
             mExecutorService.submit(new Runnable() {
                 @Override
                 public void run() {
                     if(!startRecord())
                     {
                        recordFail();
                     }
                 }
             }) ;
            }

    }

    @OnClick(R.id.bt_streammodePlay)
    public void paly()
    {

        if (mAudioFile!=null && !isPlaying)
        {
            isPlaying = true;
            mButtonPlay.setText("正在播放");
            mButtonPlay.setBackgroundResource(R.color.colorAccent);

            Log.i("STREAMMODE", "paly1: "+mAudioFile.getAbsolutePath());
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    Log.i("STREAMMODE", "paly2: "+mAudioFile.getAbsolutePath());
                    doPlay(mAudioFile);
                }
            });
        }
        else
        {
            showInfo.setText("");
            showInfo.setText("没有选中录音文件呐");
        }
    }


    private void doPlay(File audioFile) {
//        配置播放器

//        音乐类型是扬声器播放
        int streamType = AudioManager.STREAM_MUSIC;

//        与录音时候的采用频率是相同的，改变这个频率会改变声音的速度
        int sampleRate = 44100;

//      录音的时候采用的是双声道的
        int channalConfig =  AudioFormat.CHANNEL_OUT_STEREO;

//        录音的时候用的是16BIT的
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

//        流模式
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,channalConfig,audioFormat);

//      构造AudioTrack
        AudioTrack mAudioTrack = new AudioTrack(streamType,sampleRate,channalConfig,audioFormat,Math.max(minBufferSize,BUFFER_SIZE),mode);

//        从文件流中读取数据
        FileInputStream fileInputStream = null;
        try {
//         将我们的audioFile文件创建一个输入流
            fileInputStream = new FileInputStream(audioFile);

            //        循环读数据写到播放器去
            int read;
//            将输入流的数据读取到缓冲区中去暂存，只要没读完，就循环的读取数据到缓冲区
            mAudioTrack.play();
            while((read = fileInputStream.read(mBuffer)) > 0)
            {
//                将缓冲区中的第0个字节到第read个字节的数据写入到AudioTrack中去
                int ret = mAudioTrack.write(mBuffer,0,read);
                switch (ret)
                {
                    case AudioTrack.ERROR_INVALID_OPERATION:
                    case AudioTrack.ERROR_BAD_VALUE:
                    case AudioManager.ERROR_DEAD_OBJECT:
                        playFail();
                        return;
                    default:
                        break;
                }
            }
        }catch (RuntimeException | IOException e)
        {
            e.printStackTrace();
            playFail();
        }
        finally {
            isPlaying = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mButtonPlay.setText(R.string.streamModePlay);
                    mButtonPlay.setBackgroundResource(R.color.colorPrimary);
                }
            });
//            关闭文件输入流
            if (fileInputStream!=null)
              closeQuietly(fileInputStream);
//            播放器释放
              resetQuietly(mAudioTrack);
        }

    }

    private boolean startRecord() {

        try{
            //创建语音文件
            mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/record/"+System.currentTimeMillis()+".pcm");
            if (!mAudioFile.getParentFile().exists())
            {
                mAudioFile.getParentFile().mkdirs();

            }
            mAudioFile.createNewFile();

            //创建文件输出流
            mFileOutputStream = new FileOutputStream(mAudioFile);

            //         配置AudioRecord
            int audioSourcec = MediaRecorder.AudioSource.MIC;
            int sampaleRate = 44110;
            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

//            计算AudioRecor内部缓冲区最小的buffer大小
            int minBurrferSize = AudioRecord.getMinBufferSize(sampaleRate,channelConfig,audioFormat);


//            BUFFER 不能小于最低要求也不能小于我们每次读取的大小
            mAudioRecord = new AudioRecord(audioSourcec,sampaleRate,channelConfig,audioFormat,Math.max(minBurrferSize,BUFFER_SIZE));

            //        开始录音
            mAudioRecord.startRecording();

            //        记录开始语音时间，用于统计录音时长
            startRecordTime = System.currentTimeMillis();

//            创建Socket对象
             socket = new Socket("192.168.137.1",6768);
            //2、获取输出流，向服务器端发送信息
              os = socket.getOutputStream();//字节输出流




            //        循环读取数据写到输出流中
//            new SendMessageThread(socket,isRecording,os,mAudioRecord,mBuffer,BUFFER_SIZE,mFileOutputStream).start();



            while(isRecording)
            {
                int read = mAudioRecord.read(mBuffer,0,BUFFER_SIZE);
                if(read>0)
                {
//                    Log.i("MEAN", "startRecord: "+ Arrays.toString(mBuffer));
                    os.write(mBuffer,0,read);
                    os.flush();

                    //   读取成功，写入文件，获取写入到手机内存中去
                  mFileOutputStream.write(mBuffer);
                }
                else
                {
                    //提醒用户读取失败
                    return  false;
                }
            }

            // 退出循环，停止；录音，释放资源
            return  stopRecord();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return  false;

        }
        finally {
            // 释放AudioRecord的资源
            if (mAudioRecord!=null)
            {
                mAudioRecord.release();
            }
        }
    }

    private boolean stopRecord() {
        try {

            //停止录音，关闭输出流，记录结束时间，统计录音时长
            mAudioRecord.stop();
            mAudioRecord.release();
            mFileOutputStream.close();

            stopRecordTiem = System.currentTimeMillis();
            final int second = (int)((stopRecordTiem-startRecordTime)/1000);

            if (second > 3)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showInfo.setText("");
                        showInfo.setText( mAudioFile.getName()+second + "秒");
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showInfo.setText("");
                        showInfo.setText( "时间不足三秒，请重新录制");
                        mAudioFile.delete();
                    }
                });
            }
           ReceiveMessageThread rs =  new ReceiveMessageThread(socket);
           final String msg =  rs.getMsgFromServer();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showInfo.setText("");
                    showInfo.setText(msg);
                    mAudioFile.delete();
                }
            });

        }
        catch (Exception e)
        {
            Log.i("MEAN1", "出现错误信息了啊");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void recordFail() {

        //给用户一个提示，需要在主线程中执行
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                isRecording = false;
                showInfo.setText("录音失败");
            }
        });
    }

    private void playFail() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showInfo.setText("");
                showInfo.setText("播放失败");
            }
        });

    }

    private void resetQuietly(AudioTrack audioTrack) {
        try {
            audioTrack.stop();
            audioTrack.release();
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void closeQuietly(FileInputStream fileInputStream) {
        try{
            fileInputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdownNow();
        try {
            os.close();
            socket.close();
        }catch (Exception e)
        {

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    //这个是控制右上方的三个点的设置键
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
