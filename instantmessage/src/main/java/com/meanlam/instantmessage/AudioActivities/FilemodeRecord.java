package com.meanlam.instantmessage.AudioActivities;

//绿色，已经加入控制暂未提交
//        红色，未加入版本控制.
//
//        蓝色，加入，已提交，有改动
//        白色，加入，已提交，无改动
//        灰色：版本控制已忽略文件。

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.meanlam.instantmessage.R;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FilemodeRecord extends AppCompatActivity {

    @BindView(R.id.bt_filemodeRecord)
    Button pressToSay;

    @BindView(R.id.tv_filemodeText)
    TextView showInfo;

    @BindView(R.id.bt_filemodePlay)
    Button clickToPaly;

    @BindView(R.id.filemodePlayFile)
    EditText mEditTextPlayFile;

    private ExecutorService mExecutorService;
    private MediaRecorder   mMediaRecorder;
    private File            mAudioFile;
    private long            mStartRecordTime,mStopRecordTime;
    private  volatile   boolean             isPlaying;
    private MediaPlayer mMediaPlayer;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filemode_record);
        ButterKnife.bind(this);

        Toolbar toolbar =  findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);

        mExecutorService = Executors.newSingleThreadExecutor();

//        录音JNI函数不具备线程安全性，所以要用单线程
        pressToSay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        startRecord();
                        break;

                    case MotionEvent.ACTION_UP:
                        stopRecord();
                        break;

                    case MotionEvent.ACTION_CANCEL:



                }
                return true;
            }
        });

    }

    private void startRecord() {
        //首先需要改变UI的状态
        pressToSay.setText("正在说话");
        pressToSay.setBackgroundResource(R.color.colorAccent);

//        提交一个后台任务 执行录音逻辑
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
               //释放之前的录音的MediaRecorder
                releaseRecorder();

                //执行我们的录音逻辑，如果失败，提示用户
                if(!doStart())
                {
                    recordFail();
                }

            }
        });

    }

    private void recordFail() {
        mAudioFile = null;
        //给用户一个提示，需要在主线程中执行
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showInfo.setText("录音失败");
                Toast.makeText(FilemodeRecord.this,"录音失败",Toast.LENGTH_SHORT).show();
            }
        });


    }

    /**
     * 停止录音的逻辑
     * @return
     */
    private boolean doStop() {

        try{
            mMediaRecorder.stop();
            //记录停止时间，统计时长
            // 只接受超过三秒的录音，在UI上显示出来
            mStopRecordTime = System.currentTimeMillis();
           final int second = (int) ((mStopRecordTime-mStartRecordTime)/1000);
            if (second > 3)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showInfo.setText("");
                        showInfo.setText( second + "秒");
                    }
                });
            }

        }
        catch (IllegalStateException e)
        {
            e.printStackTrace();
            return  false;
        }

        return  true;
    }

    /**
     * 启动录音逻辑
     * @return
     */
    private boolean doStart() {
        try{
            //首先需要创建一个MediaRecord
            mMediaRecorder = new MediaRecorder();
            //创建一个录音文件
            mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/record/"+System.currentTimeMillis()+".m4a");

            if (!mAudioFile.getParentFile().exists())
            {
                mAudioFile.getParentFile().mkdirs();

            }
            mAudioFile.createNewFile();

            //配置我们的MediaRecord
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                //保存文件为MP4格式
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setAudioSamplingRate(44100);

                //音频编码为AAC格式
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                //音质比较好的频率
                mMediaRecorder.setAudioEncodingBitRate(96000);


                mMediaRecorder.setOutputFile(mAudioFile);


            //开始录音
                mMediaRecorder.prepare();
                mMediaRecorder.start();

            //记录录音时长

            mStartRecordTime = System.currentTimeMillis();

        }catch (IOException  | RuntimeException e)
        {
            e.printStackTrace();
            //捕获异常避免闪退，返回false
            return  false;
        }

        return  true;
    }

    /**
     * 释放MediaRecorder
     */
    private void releaseRecorder() {

        if(mMediaRecorder!=null)
        {
            mMediaRecorder.release();
        }
    }

    /**
     * 停止录音
     */
    private void stopRecord() {

        pressToSay.setText("按住说话");
        pressToSay.setBackgroundResource(R.color.colorPrimary);

        //        提交一个后台任务 执行停止录音逻辑
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {

                //                执行停止录音逻辑，如果失败需要提醒用户
                if(!doStop())
                {
                   recordFail();
                }
                //                释放MediaRec

                releaseRecorder();
            }
        });
    }

    @OnClick(R.id.bt_filemodePlay)
    public void play()
    {
        try {
            //        检查当前的状态，这是一个后台播放的，多次点击会造成多次播放，所以一个volatile类型的状态控制
            if (mAudioFile!=null && !isPlaying) {
                //          设置当前播放状态
                isPlaying = true;
                clickToPaly.setText("正在播放录音");
                clickToPaly.setBackgroundResource(R.color.colorAccent);
                mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record/" + mEditTextPlayFile.getText().toString());


                mExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("FILEMODE", "doPlay: " + mAudioFile.getAbsolutePath());
                        doPlay(mAudioFile);
                    }
                });
            }
        }
        catch (Exception e)
        {
            playFail();
            stopPlay();
        }
    }

    /**
     * 实际播放逻辑
     * @param audioFile
     */
    private void doPlay(File audioFile) {
        //配置我们的播放器MediaPlay
            mMediaPlayer = new MediaPlayer();
        try{
            //        捕获异常，防止闪退
            mMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            //设置监听回调
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
//                    播放结束，释放播放器
                    stopPlay();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
//                     提示用户
                    playFail();
//                    释放播放器
                    stopPlay();
//                  错误已经处理，返回true
                    return true;
                }
            });
//            配置音量是否循环,不循环
            mMediaPlayer.setVolume(1,1);
            mMediaPlayer.setLooping(false);

            mMediaPlayer.prepare();
            mMediaPlayer.start();

        }catch (RuntimeException | IOException e)
        {
            e.printStackTrace();
//            提醒用户播放失败
              playFail();
//            释放播放器
            stopPlay();

        }

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

    private void stopPlay() {
        //充值我们的播放状态
        isPlaying = false;

        clickToPaly.setText(R.string.fileModePlay);
        clickToPaly.setBackgroundResource(R.color.colorPrimary);


//        释放播放器
        if (mMediaPlayer!=null)
        {
//            重置监听器，防止内存泄漏

            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.setOnErrorListener(null);

            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

//        activity销毁时，立即停止执行后台任务
        mExecutorService.shutdown();
        releaseRecorder();
        stopPlay();
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
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
