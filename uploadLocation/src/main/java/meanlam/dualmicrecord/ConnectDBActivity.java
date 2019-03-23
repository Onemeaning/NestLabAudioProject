package meanlam.dualmicrecord;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import meanlam.dualmicrecord.networkutils.ConnectDataBase;
import meanlam.dualmicrecord.networkutils.SelfDialog;
import meanlam.dualmicrecord.uploadfileutil.FormFile;
import meanlam.dualmicrecord.uploadfileutil.SocketHttpRequester;

public class ConnectDBActivity extends AppCompatActivity {

    private Button   button_connect;
    private Button   button_uoload;
    private Button button_download;
    private TextView mTitleText;
    private TextView textview;
    private String   str;
    private File     file;
    private File[]  files;
    private Connection  connection;
    private static final String TAG="Meanlam_ConDB";
    private  int length = 0;
    private  int lengthOK = 0;

    private ListView      liebiao = null;
    private SimpleAdapter adpter  = null;
    private        List<Map<String, String>> luyinliebiao  = null;
    private SharedPreferences connect;

    // 消息显示到控件
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1003:
                    str = msg.getData().getString("result");
                    textview.setTextColor(Color.GREEN);
                    textview.setText(str);
                default:
                    break;
            }
        }
    };
    @SuppressLint("HandlerLeak")
    Handler mHandler1 = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1004:
                    String length = msg.getData().getString("unSendAudio");
                    mTitleText.setText(length);
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_connectserver);

        liebiao =  this.findViewById(R.id.liebiao);

        button_connect =  findViewById(R.id.connect);
        button_uoload = findViewById(R.id.bt_uploadfile);
        button_download = findViewById(R.id.bt_download);

        mTitleText = findViewById(R.id.titleText2);
        textview = findViewById(R.id.sqlContent);

        connect = getSharedPreferences("connect", Context.MODE_PRIVATE);

        // 出现输入IP的dialog
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   openCustomerDialog();
            }
        });

        //一键上传功能
        button_uoload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAllFile();
            }
        });

        //测试数据库是否联通
        button_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDBDataToList();
            }
        });
    }

    /**
     *往服务器中传送录音文件夹下的所有的文件（按照录音文件夹下顺序上传）
     */
    private void uploadAllFile()
    {
    Runnable runnable1 =new Runnable() {
        public void run() {
            MainActivity.baocunlujin =  new File(Environment.getExternalStorageDirectory() + "/record/");
            files = MainActivity.baocunlujin.listFiles();
            length = files.length;
            lengthOK = 0;
            boolean serverState = true;
            for (int i = 0; i < files.length; i++) {

                Message msg = new Message();
                msg.what = 1004;
                Bundle bundle = new Bundle();
                bundle.putString("unSendAudio","未发送个数："+length+"   已经发送个数："+lengthOK);
                msg.setData(bundle);
                mHandler1.sendMessage(msg);
                file = new File(Environment.getExternalStorageDirectory()+"/record/", files[i].getName());
                Log.i(TAG, "文件是否存在： " + file.exists());
                uploadFile(file);
                if(files == null)
                {
                    serverState = false;
                    break;
                }
                files[i].delete();
                ++lengthOK;
                --length;
            }

            Message msg = new Message();
            msg.what = 1004;
            Bundle bundle = new Bundle();
            if (serverState==true)
                bundle.putString("unSendAudio","文件发送完成");
            else
                bundle.putString("unSendAudio","【警告】服务器没有打开，请检查");
            msg.setData(bundle);
            mHandler1.sendMessage(msg);
        }
    };
        new Thread(runnable1).start();
    }

    /**
     * 上传音频到服务器
     *
     * @param imageFile 包含路径
     */
    public void uploadFile(File imageFile) {
        Log.i(TAG, "upload start");
        try {
            String requestUrl = "http://"+connect.getString("IP","192.168.1.1")+":"+"8080/Up_Load/UploadServlet";
            //请求普通信息
            Map<String, String> params = new HashMap<String, String>();
            params.put("WAV_NAME", imageFile.getName());
            //上传文件
            FormFile formfile = new FormFile(imageFile.getName(), imageFile, "image", "application/octet-stream");
            boolean state =  SocketHttpRequester.post(requestUrl, params, formfile);

           if (state!=true)
           {
               files=null;
           }
            Log.i(TAG, "upload success");

        } catch (Exception e) {
            Log.i(TAG, "upload error from uploadException "+e.toString());
            e.printStackTrace();
        }
        Log.i(TAG, "upload end");
    }

    //自定义LoginDialog，用于输入用户的IP和PORT
    public void openCustomerDialog(){
        final SelfDialog selfDialog = new SelfDialog(getContext());
        selfDialog.setShowTitle(true);
        selfDialog.setLoginListener(new SelfDialog.onLoginListener() {
            String editUserName;
            String editPassword;
             final CheckBox cbServiceItem = selfDialog.getCbServiceItem();
             String str = "";
            @Override
            public void onClick(View v) {
                 editUserName = selfDialog.getEditUserName().getText().toString();
                 editPassword = selfDialog.getEditPassword().getText().toString();

                if (editUserName.equals("")||editPassword.equals(""))
                {
                    editUserName = connect.getString("IP","192.168.1.1");
                    editPassword = connect.getString("PORT","3306");
                    Toast.makeText(ConnectDBActivity.this, editUserName + ":"+editPassword, Toast.LENGTH_LONG).show();
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run()
                                {
                                    ConnectDataBase dataBase =  new ConnectDataBase(editUserName, editPassword ,ConnectDBActivity.this);
                                   try{
                                       connection = ConnectDataBase.getSQLConnection();
                                       str = "已连接："+editUserName+":"+editPassword+"\n";
                                       ConnectDataBase.query(connection);

                                   }
                                   catch (Exception e)
                                   {
                                       str = "无法连接到"+editUserName+":"+editPassword+"\n";
                                       Log.i("Onemeaning", "SQLException: " + connection.toString());
                                   }
                                    Message msg = new Message();
                                    msg.what = 1003;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("result", str);
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                            }
                    ).start();
                    selfDialog.dismiss();
                }

                else
                {
                   SharedPreferences.Editor connectEdit = connect.edit();
                   connectEdit.putString("IP",editUserName);
                   connectEdit.putString("PORT",editPassword);
                   connectEdit.commit();
                   Toast.makeText(ConnectDBActivity.this, editUserName+editPassword, Toast.LENGTH_LONG).show();

                    new Thread(
                            new Runnable() {
                                @Override
                                public void run()
                                {
                            ConnectDataBase dataBase =  new ConnectDataBase(editUserName, editPassword ,ConnectDBActivity.this);
                            try{
                                    connection = ConnectDataBase.getSQLConnection();
                                    ConnectDataBase.query(connection);
                                     str = "已连接："+editUserName+":"+editPassword+"\n";
                            }
                            catch (Exception e)
                            {
                                str = "无法连接到"+editUserName+":"+editPassword+"\n";
                                Log.i(TAG, "SQLException:: " + connection.toString());
                            }
                            Message msg = new Message();
                            msg.what = 1003;
                            Bundle bundle = new Bundle();
                            bundle.putString("result", str);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                                }
                            }
                    ).start();
                    selfDialog.dismiss();
                }
            }
        });
    }

    //获取当前对象
    private Context getContext(){
        return this;
    }

    //从数据中获取数据到List列表中去
    private void getDBDataToList() {
        luyinliebiao = new ArrayList<Map<String, String>>();
        luyinliebiao.clear();
            for (int i = 0; i < ConnectDataBase.nameList.size(); i++) {
                Map<String, String> fileinfo = new HashMap<String, String>();
                fileinfo.put("fileName", ConnectDataBase.nameList.get(i) );
                fileinfo.put("tdoa", ConnectDataBase.todaList.get(i));
                this.luyinliebiao.add(fileinfo);
            }
            this.adpter = new SimpleAdapter(this, this.luyinliebiao, R.layout.recorderfiles, new
                    String[]{"fileName","tdoa"}, new int[]{R.id.fileName,R.id.TDOA});
            this.liebiao.setAdapter(this.adpter);
    }

}