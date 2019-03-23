package meanlam.dualmicrecord.utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DataService extends Service {

    private boolean threadDisable = false;
    private int z_azimuth = 0;
    private int x_pitch = 0;
    private int y_roll = 0;
    private  String TAG = "DataService";

    private  SharedPreferences sharedPreferences;
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("ceshi",Context.MODE_PRIVATE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!threadDisable)
                {
                    try{
                        x_pitch = sharedPreferences.getInt("x_pitch",0);
                        y_roll = sharedPreferences.getInt("y_roll",0);
                        z_azimuth = sharedPreferences.getInt("z_azimuth",0);
                        Log.i(TAG, "x_pitch:"+x_pitch +"y_roll:"+y_roll+"z_azimuth:"+z_azimuth);
                        new postTask().execute(z_azimuth,x_pitch,y_roll);
                        Thread.sleep(1000);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        threadDisable = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
class postTask extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {
        //依次获取用户名，密码与路径
        String azimuth =params[0].toString();
        String pitch =params[1].toString();
        String roll =params[2].toString();
        try {
            StringBuffer sb = new StringBuffer();
            //建立连接
            URL url=new URL("http://192.168.137.1:8080/Three_Value/Split_Three_Values.jsp");
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setConnectTimeout(3000);        //设置连接超时时间
            httpURLConnection.setDoInput(true);                  //打开输入流，以便从服务器获取数据
            httpURLConnection.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
            httpURLConnection.setRequestMethod("POST");    //设置以Post方式提交数据
            httpURLConnection.setUseCaches(false);               //使用Post方式不能使用缓存
            //设置请求体的类型是文本类型
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String s =azimuth + ","+ pitch + ","+roll;
            //传递参数
            Map<String, String> params1 = new HashMap<String, String>();
            params1.put("ThreeValues", s);
            sb.append("ThreeValues").append('=').append(URLEncoder.encode(s, "UTF-8"));

            byte[] data = sb.toString().getBytes();//得到实体的二进制数据;
            //设置请求体的长度
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
            //获得输出流，向服务器写入数据
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data);
            //获得响应状态
            int resultCode = httpURLConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == resultCode) {
                String readLine = "";
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
                while ((readLine = responseReader.readLine()) != null) {
                    sb.append(readLine).append("\n");
                }
                responseReader.close();
            }
            return (sb.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        //获取Android studio与web后台数据交互获得的值
        String s= (String) o;
        //吐司Android studio与web后台数据交互获得的值
        //            Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }
}