package meanlam.dualmicrecord;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import meanlam.dualmicrecord.utils.AudioRecordFunc;
import meanlam.dualmicrecord.utils.PopupWindowFactory;
import meanlam.dualmicrecord.utils.TimeUtils;

public class MainActivity extends AppCompatActivity {

    static final int VOICE_REQUEST_CODE = 66;

    private Button mButton;
    private Button mButtonclear;

    private static ImageView mImageView;
    private static TextView  mViewTime, tv_cancel;
    private AudioRecordFunc micInstance;

    private Context            context;
    private PopupWindowFactory mPop;
    private RelativeLayout     rl;
    private        ListView                  liebiao       = null;
    private        SimpleAdapter             adpter        = null;
    private        List<Map<String, Object>> luyinliebiao  = null;
    private        Boolean                   sdcardExist   = false;
    private        File                      baocunlujin   = null;
    private        File                      mPcmDirectory = null;
    private        boolean                   isDistory     = false;
    private static double                    volume        = 0;
    public static  long                      startTime     = 0;

    @SuppressLint("HandlerLeak")
    private static Handler mHandler  = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //
            if (msg.what == 3) {
                mImageView.getDrawable().setLevel((int) (3000 + 9000 * volume / 100));//setlevel（）中的值取值为0-10000，如果用了clip标签就是把图片剪切成10000份
                mViewTime.setText(TimeUtils.long2String(System.currentTimeMillis() - startTime));
            }

        }
    };
    @SuppressLint("HandlerLeak")
    private final  Handler mHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                MainActivity.this.getFileList();
                Toast.makeText(MainActivity.this, baocunlujin.getName() + "文件夹中文件已删除", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, mPcmDirectory.getName() + "文件夹中文件已删除", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 2) {
                MainActivity.this.getFileList();
                Toast.makeText(MainActivity.this, "录音文件以保存到" + baocunlujin.getName() + "文件夹中", Toast.LENGTH_SHORT).show();
            }
            //修改之后尝试更新看看是什么效果，这是从GitHub中修改的文件，是否能pull到我的项目中去呢
            //测试我们创建的分支是否有效的，分支为MyBranch
        }
    };

    private static Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            MainActivity.updateMicStatus();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;


        rl = (RelativeLayout) findViewById(R.id.rl);
        mButton = (Button) findViewById(R.id.button);
        mButtonclear = findViewById(R.id.buttonclear);
        liebiao = (ListView) this.findViewById(R.id.liebiao);

        //PopupWindow的布局文件
        final View view = View.inflate(this, R.layout.layout_microphone, null);
        mPop = new PopupWindowFactory(this, view);

        //PopupWindow布局文件里面的控件
        mImageView = (ImageView) view.findViewById(R.id.iv_recording_icon);
        mViewTime = (TextView) view.findViewById(R.id.tv_recording_time);
        tv_cancel = (TextView) view.findViewById(R.id.tv_recording_info);
        makeDirs();
        //6.0以上需要权限申请
        requestPermissions();

        micInstance = AudioRecordFunc.getInstance();

        MainActivity.this.getFileList();
        liebiao.setOnItemClickListener(new OnItemClickListenerImp());
    }

    /**
     * 用于实时获取手机麦克风的音量大小
     * @author
     * Meanlam
     */
    public static void updateMicStatus() {

        if (AudioRecordFunc.micRecord != null) {
            Log.i("Meanlam", "OK ");
            byte[] byte_buffer = new byte[AudioRecordFunc.micbufferSizeInBytes];
            int readSize = AudioRecordFunc.micRecord.read(byte_buffer, 0, AudioRecordFunc.micbufferSizeInBytes);
            long v = 0;
            for (int i = 0; i < byte_buffer.length; i++) {
                v += byte_buffer[i] * byte_buffer[i];
            }
            // 平方和除以数据总长度，得到音量大小。
            double mean = v / (double) readSize;
            volume = 20 * Math.log10(mean);
            Log.i("Meanlam", "volum:: " + volume);

            mHandler.postDelayed(mUpdateMicStatusTimer, 1);
            mHandler.sendEmptyMessage(3);
        }
    }

    // 存储媒体已经挂载，并且挂载点可读/写
    private void makeDirs() {
        if (sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            MainActivity.this.baocunlujin = new File(Environment.getExternalStorageDirectory() + "/record/");
            MainActivity.this.mPcmDirectory = new File(Environment.getExternalStorageDirectory() + "/record1/");

            if (!MainActivity.this.baocunlujin.exists()) {
                MainActivity.this.baocunlujin.mkdirs();
            }
            if (!MainActivity.this.mPcmDirectory.exists()) {
                MainActivity.this.mPcmDirectory.mkdirs();
            }
        }
    }

    /**
     * 开启扫描之前判断权限是否打开
     */
    private void requestPermissions() {
        //判断是否开启摄像头权限
        if ((ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                ) {
            StartListener();

            //判断是否开启语音权限
        } else {
            //请求获取摄像头权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    VOICE_REQUEST_CODE);
        }

    }

    /**
     * 请求权限回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VOICE_REQUEST_CODE) {
            if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager
                    .PERMISSION_GRANTED)) {
                StartListener();
            } else {
                Toast.makeText(context, "已拒绝权限！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //播放列表音频文件
    private class OnItemClickListenerImp implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (MainActivity.this.adpter.getItem(position) instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) MainActivity.this.adpter.getItem(position);
                Uri uri = Uri.fromFile(
                        new File(MainActivity.this.baocunlujin.toString() + File.separator + map.get("tishi")));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(uri, "audio/*");
                MainActivity.this.startActivity(intent);
            }

        }

    }

    //获取录音文件列表
    private void getFileList() {
        luyinliebiao = new ArrayList<Map<String, Object>>();
        if (sdcardExist) {
            File[] files = MainActivity.this.baocunlujin.listFiles();
            for (int i = 0; i < files.length; i++) {
                Map<String, Object> fileinfo = new HashMap<String, Object>();
                fileinfo.put("tishi", files[i].getName());
                this.luyinliebiao.add(fileinfo);
            }
            this.adpter = new SimpleAdapter(this, this.luyinliebiao, R.layout.recorderfiles, new String[]{"tishi"},
                    new int[]{R.id.tishi});
            this.liebiao.setAdapter(this.adpter);
        }
    }

    public void StartListener() {
        //Button的touch监听
        mButtonclear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonclear) {
                    try {
                        AudioRecordFunc.clearCache(baocunlujin);
                        AudioRecordFunc.clearCache(mPcmDirectory);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //                                Looper.prepare();
                                //                                Message msg = Message.obtain();
                                //                                msg.what = 1;
                                mHandler1.sendEmptyMessage(1);
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int start_x = 0, start_y = 0, end_x, end_y, mov_x, mov_y;
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        tv_cancel.setTextColor(Color.parseColor("#FFFFFF"));
                        mImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.record_microphone));
                        start_y = (int) event.getY();
                        mPop.showAtLocation(rl, Gravity.CENTER, 0, 0);
                        mButton.setText("松开保存");
                        try {
                            ThreadOne t1 = new ThreadOne();
                            t1.start();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        try {
                            ThreadTwo t2 = new ThreadTwo();
                            t2.start();
                            Thread.sleep(400);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mPop.dismiss();
                        mButton.setText("按住录音");

                        //                        tv_cancel.setTextColor(Color.parseColor("#FFFFFF"));
                        Log.i("Onemeaning", "7、手指已经离开录音键");
                        break;


                    case MotionEvent.ACTION_MOVE:
                        end_y = (int) event.getY();
                        mov_y = Math.abs(start_y - end_y);
                        if (mov_y < 300 && mov_y > 100) {
                            //                            tv_cancel.setText("手指松开，取消录音");
                            //                            tv_cancel.setTextColor(Color.parseColor("#FFFFFF"));
                        }
                        if (mov_y < 100) {
                            //                            tv_cancel.setText("松开保存");
                            //                            tv_cancel.setTextColor(Color.parseColor("#FFFFFF"));
                        }
                        break;

                }
                return true;
            }
        });
    }

    private class ThreadOne extends Thread {
        @Override
        public void run() {
            micInstance.startRecordMic();
        }
    }

    private class ThreadTwo extends Thread {
        @Override
        public void run() {
            micInstance.stopMicRecordAndFile();
            mHandler1.sendEmptyMessage(2);

        }

    }


}
