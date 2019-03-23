package meanlam.dualmicrecord;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import meanlam.dualmicrecord.utils.AudioRecordFunc;
import meanlam.dualmicrecord.utils.DataService;
import meanlam.dualmicrecord.utils.PopupWindowFactory;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    static final int VOICE_REQUEST_CODE = 66;
    private Button mButton;
    private Button mButtonclear;
    private Button mButtonCon;
    private Button mButtonCalcTdoa;
    private Button mButtonRecord;
    private TextView mTitleView;

    private static ImageView mImageView;
    private static TextView  mViewTime, tv_cancel;
    private AudioRecordFunc  micInstance;

    private Context            context;
    private PopupWindowFactory mPop;
    private RelativeLayout     rl;
    private        Boolean                   sdcardExist   = false;
    public       static  File                      baocunlujin   = null;
    private        File                      mPcmDirectory = null;
    public static  long                      startTime     = 0;


    //下面是指南针需要的一些参数
    public static final String TAG = "MainActivity1";
    private static final int EXIT_TIME = 2000;// 两次按返回键的间隔判断
    private SensorManager mSensorManager;//传感器管理器
    private Sensor        mAccelerometer;//加速度传感器
    private Sensor        mMagneticField;//磁场强度传感器

    private LocationManager mLocationManager;//定位管理器

    private String mLocationProvider;// 位置提供者名称，GPS设备还是网络

    private float mCurrentDegree = 0f;

    private float[] mAccelerometerValues = new float[3];
    private float[] mMagneticFieldValues = new float[3];

    private float[] mValues = new float[3];
    private float[] mMatrix = new float[9];

    private long firstExitTime = 0L;// 用来保存第一次按返回键的时间

    private TextView     mTvCoord;
    private LinearLayout mLlLocation;
    private TextView     mTvAltitude;//显示海拔信息
    private ImageView    mIvCompass;//显示坐标

    private TextView     mTvAzimuth;//显示方位角信息
    private TextView     mTvPitch;//显示俯仰角信息
    private TextView     mTvRoll;//显示翻转角信息

    public static   int x_pitch = 0;
    public static  int y_roll = 0;
    public static  int z_azimuth = 0;


   private SharedPreferences.Editor editor;
   private SharedPreferences.Editor connect;
   private Intent intentService;
   private  boolean isInService = true;

    @SuppressLint("HandlerLeak")//这个handle文件专门用于处理更新录音或者删除文件后的UI的更新
    private final  Handler mHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                mTitleView.setText("录音文件已经全部删除");
            } else if (msg.what == 2) {
               String length = msg.getData().getString("length");
                mTitleView.setText(length);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 用于开启数据传输服务，数据上传到JSP中，运行在服务进程中
        intentService = new Intent(MainActivity.this, DataService.class);
        startService(intentService);

        initService();
        initViewId();
        requestPermissions();//6.0以上需要权限申请
        makeDirs();

        micInstance = AudioRecordFunc.getInstance();

        //第一步，获取SharedPreferences的编辑者
        SharedPreferences sharedPreferences = getSharedPreferences("ceshi", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        SharedPreferences sharedPreferences1 = getSharedPreferences("connect", Context.MODE_PRIVATE);
        connect = sharedPreferences1.edit();
        connect.putString("IP","192.168.1.1");
        connect.putString("PORT","3306");
        connect.commit();
        mTitleView.setText("录音文件总数目为："+updateTitleInfo());
    }



    /**
     * 初始化布局控件
     */ public void initViewId() {
        findViews();
        context = this;
        rl = findViewById(R.id.rl);
        mButton = findViewById(R.id.button);
        mButtonclear = findViewById(R.id.buttonclear);
        mButtonCon = findViewById(R.id.button_TOconnect);
        mButtonCalcTdoa = findViewById(R.id.button_calcTdoa);
        mButtonRecord = findViewById(R.id.bt_record);
        mTitleView = findViewById(R.id.titleText);

        //PopupWindow的布局文件
        final View view = View.inflate(this, R.layout.layout_microphone, null);
        mPop = new PopupWindowFactory(this, view);
        //PopupWindow布局文件里面的控件
        mImageView = view.findViewById(R.id.iv_recording_icon);
        mViewTime = view.findViewById(R.id.tv_recording_time);
        tv_cancel = view.findViewById(R.id.tv_recording_info);


        //初始化指南针所需要的控件


    }

    // 存储媒体已经挂载，并且挂载点可读/写
    private void makeDirs() {
        if (sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            baocunlujin = new File(Environment.getExternalStorageDirectory() +
                    "/record/");
            MainActivity.this.mPcmDirectory = new File(Environment.getExternalStorageDirectory()
                    + "/record1/");

            if (!baocunlujin.exists()) {
                baocunlujin.mkdirs();
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
                )
        {
                        //授予权限之后允许可以语音
                        startListener();
        }
        else
        {
            //请求获取摄像头权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
                            .RECORD_AUDIO},
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
            if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED))
            {
                    startListener();
            }
            else
             {
                 Toast.makeText(context, "已拒绝权限！", Toast.LENGTH_SHORT).show();
             }
        }
    }


    //更新TitleText的上传提示

    private String updateTitleInfo()
    {
        File[] files = baocunlujin.listFiles();

        return String.valueOf(files.length);
    }

    Handler handler = new Handler();
    Runnable timer = new Runnable() {
        @Override
        public void run() {
           if (System.currentTimeMillis()-startTime>= AudioRecordFunc.maxTimeLength)
           {
               try{
                   new ThreadTwo().start();
                   Thread.sleep(500);
                   handler.removeCallbacks(timer);
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);
                   //               handler.removeCallbacks(timer);
//                    Thread.sleep(1000);
//                   mButtonRecord.setEnabled(false);
//                   mButtonRecord.setText("正在录音");
//                   mButtonRecord.setBackgroundColor(Color.GRAY);
//                   new ThreadOne().start();
//                   MainActivity.startTime = System.currentTimeMillis();


//                   handler.postDelayed(this, 10);//连续录音就打开这个
               }
               catch (Exception e)
               {
                   handler.removeCallbacks(timer);
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);
               }

           }
            handler.postDelayed(this, 10);
        }

    };

    @SuppressLint("ClickableViewAccessibility")
    public void startListener() {
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
                                mHandler1.sendEmptyMessage(1);
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mButtonCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.button_TOconnect) {

                    startActivity(new Intent(MainActivity.this, ConnectDBActivity.class));
                }
            }
        });

        mButtonCalcTdoa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.button_calcTdoa) {
                    if (isInService == true)
                    {
                        stopService(intentService);
                        mButtonCalcTdoa.setText("打开后台数据传输");
                        isInService = false;
                    }
                    else if (isInService == false)
                    {
                        startService(intentService);
                        mButtonCalcTdoa.setText("关闭后台数据传输");
                        isInService = true;
                    }

                }
            }
        });
        mButtonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.bt_record) {
                    Toast.makeText(MainActivity.this,"开始录音",Toast.LENGTH_SHORT).show();
                    mButtonRecord.setEnabled(false);
                    mButtonRecord.setText("正在录音");
                    mButtonRecord.setBackgroundColor(Color.GRAY);
                    new ThreadOne().start();
                   handler.postDelayed(timer,10);

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
                        mPop.showAtLocation(rl, Gravity.CENTER, 0, 0);
                        mButton.setText("松开保存");

                        try {
                            handler.removeCallbacks(timer);
                            mButtonRecord.setEnabled(true);
                            mButtonRecord.setText("点击录音");
                            mButtonRecord.setBackgroundColor(Color.BLUE);

                            ThreadOne t1 = new ThreadOne();
                            mImageView.getDrawable().setLevel((3000 + 9000 * 40 / 100));
                            //setlevel（）中的值取值为0-10000，如果用了clip标签就是把图片剪切成10000份
                            mViewTime.setText("正在录音");
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
                        Log.i("Onemeaning", "7、手指已经离开录音键");
                        break;

                    case MotionEvent.ACTION_MOVE:
                        end_y = (int) event.getY();
                        mov_y = Math.abs(start_y - end_y);
                        if (mov_y < 300 && mov_y > 150) {
                            try {
                                new Thread()
                                {
                                    @Override
                                    public void run() {
                                        micInstance.cancelRecord();
                                    }
                                }.start();
                                mPop.dismiss();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                        if (mov_y < 100) {
                            //                            tv_cancel.setText("松开保存");
                            //                            tv_cancel.setTextColor(Color.parseColor
                            // ("#FFFFFF"));
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
            MainActivity.startTime = System.currentTimeMillis();
            micInstance.startRecordMic();
        }
    }

    private class ThreadTwo extends Thread {
        @Override
        public void run() {
            micInstance.stopMicRecordAndFile();
            Message msg = new Message();
            msg.what = 2;
            Bundle bundle = new Bundle();
            String length = updateTitleInfo();
            bundle.putString("length","录音文件的个数："+length);
            msg.setData(bundle);
            mHandler1.sendMessage(msg);
        }

    }


    //下面是获取三个度数的代码
    private void findViews() {
        mIvCompass = findViewById(R.id.iv_compass);

        mTvCoord = findViewById(R.id.tv_coor);
        mTvAltitude = findViewById(R.id.tv_altitude);
        mLlLocation = findViewById(R.id.ll_Coor);

        mTvAzimuth = findViewById(R.id.tv_azimuth);
        mTvPitch = findViewById(R.id.tv_pitch);
        mTvRoll = findViewById(R.id.tv_roll);



        mLlLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initLocationService();
                updateLocationService();
            }
        });
    }

    private void initService() {
        initSensorService();

        initLocationService();
    }

    private void initSensorService() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void initLocationService() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();// 条件对象，即指定条件过滤获得LocationProvider
        criteria.setAccuracy(Criteria.ACCURACY_FINE);// 较高精度
        criteria.setAltitudeRequired(true);// 是否需要高度信息
        criteria.setBearingRequired(true);// 是否需要方向信息
        criteria.setCostAllowed(true);// 是否产生费用
        criteria.setPowerRequirement(Criteria.POWER_LOW);// 设置低电耗
        mLocationProvider = mLocationManager.getBestProvider(criteria, true);// 获取条件最好的Provider,若没有权限，mLocationProvider 为null
        Log.e(TAG, "mLocationProvider = " + mLocationProvider);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerService();
        startService(intentService);
    }

    private void registerService() {
        registerSensorService();

        updateLocationService();
    }

    private void registerSensorService() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void updateLocationService() {
        if (!checkLocationPermission()) {
            mTvCoord.setText(R.string.check_location_permission);
            return;
        }

        if (mLocationProvider != null) {
            updateLocation(mLocationManager.getLastKnownLocation(mLocationProvider));
            Log.i(TAG, "updateLocationService: "+mLocationManager.getLastKnownLocation(mLocationProvider));
            mLocationManager.requestLocationUpdates(mLocationProvider, 1000, 10, mLocationListener);// 2秒或者距离变化10米时更新一次地理位置
        } else {
            mTvCoord.setText(R.string.cannot_get_location);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregister();
        stopService(intentService);
    }

    private void unregister() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        if (mLocationManager != null) {
            if (!checkLocationPermission()) {
                return;
            }
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    //实现 SensorEventListener 中的方法
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagneticFieldValues = event.values;
        }

        //调用getRotaionMatrix获得变换矩阵mMatrix[]
        SensorManager.getRotationMatrix(mMatrix, null, mAccelerometerValues, mMagneticFieldValues);
        SensorManager.getOrientation(mMatrix, mValues);
        //经过SensorManager.getOrientation(R, values);得到的values值为弧度
        //values[0]  ：azimuth 方向角（z轴），但用（磁场+加速度）得到的数据范围是（-180～180）,也就是说，0表示正北，90表示正东，180/-180表示正南，-90表示正西。
        // 而直接通过方向感应器数据范围是（0～359）360/0表示正北，90表示正东，180表示正南，270表示正西。values[1]数组的第二个元素表示仰俯角（x轴）,
        //values[2]数组的第三个元素表示翻转角（y轴）

        float degree = (float) Math.toDegrees(mValues[0]);
         x_pitch = (int) Math.toDegrees(mValues[1])+6;
         y_roll =  (int) Math.toDegrees(mValues[2]);

        setImageAnimation(degree);

        z_azimuth = (int)degree;

        mCurrentDegree = -degree;

        if (z_azimuth<0)
        {
            z_azimuth = 360 + (int)degree;
        }
            mTvAzimuth.setText(z_azimuth +"°");
            mTvPitch.setText(x_pitch + "°");
            mTvRoll.setText(y_roll +"°");


            editor.putInt("x_pitch", x_pitch);
            editor.putInt("y_roll", y_roll);
            editor.putInt("z_azimuth", z_azimuth);
            // 第三步，提交编辑内容
            editor.commit();

    }

    // 设置指南针图片的动画效果
    private void setImageAnimation(float degree) {
        RotateAnimation ra = new RotateAnimation(mCurrentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        ra.setDuration(200);
        ra.setFillAfter(true);
        mIvCompass.startAnimation(ra);
    }

    /**
     * 适配android 6.0 检查权限
     */
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    /**
     * 更新位置信息
     */
    private void updateLocation(Location location) {
        Log.e(TAG, "location = " + location);
        if (null == location) {
            mTvCoord.setText(getString(R.string.cannot_get_location));
            mTvAltitude.setVisibility(View.GONE);
        } else {
            mTvAltitude.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double altitude = location.getAltitude();
            if (latitude >= 0.0f) {
                sb.append(getString(R.string.location_north, latitude));
            } else {
                sb.append(getString(R.string.location_south, (-1.0 * latitude)));
            }

            sb.append("    ");

            if (longitude >= 0.0f) {
                sb.append(getString(R.string.location_east, longitude));
            } else {
                sb.append(getString(R.string.location_west, (-1.0 * longitude)));
            }
            mTvCoord.setText(getString(R.string.correct_coord, sb.toString()));
            mTvAltitude.setText(getString(R.string.correct_altitude, altitude));
        }

    }


    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status != LocationProvider.OUT_OF_SERVICE) {
                if (!checkLocationPermission()) {
                    mTvCoord.setText(R.string.check_location_permission);
                    return;
                }
                updateLocation(mLocationManager.getLastKnownLocation(mLocationProvider));
            } else {
                mTvCoord.setText(R.string.check_location_permission);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //连续按两次退出程序
    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        if (curTime - firstExitTime < EXIT_TIME) {
            finish();
        } else {
            Toast.makeText(this, R.string.exit_toast, Toast.LENGTH_SHORT).show();
            firstExitTime = curTime;
        }

    }

}
