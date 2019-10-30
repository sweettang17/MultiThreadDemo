package com.example.multithreaddemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressBar progress_bar;
    private Button btn_multi,btn_async,Handler,AsynTask,other;
    private TextView tv_progress;
    private ImageView img_pic;

//    private static final String DOWNLOAD_URL = "http://desk-fd.zol-img.com.cn/t_s1920x1080c5/g5/M00/07/07/ChMkJIXw8QmI06k-EABYKy-RYbJ4AACddwM0pTOAFgrj303.jpg";
    private static final String DOWNLOAD_URL = "http://source.unplash.com/random/1000x600/?raca,car";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress_bar=findViewById(R.id.progress_bar);

        btn_multi=findViewById(R.id.btn_multi);
        btn_multi.setOnClickListener(this);

        btn_async=findViewById(R.id.btn_async);
        btn_async.setOnClickListener(this);

        Handler=findViewById(R.id.Handler);
        Handler.setOnClickListener(this);

        AsynTask=findViewById(R.id.AsynTask);
        AsynTask.setOnClickListener(this);

        other=findViewById(R.id.other);
        other.setOnClickListener(this);

        tv_progress=findViewById(R.id.tv_progress);

        img_pic=findViewById(R.id.img_pic);



    }

    private CalculateThread calculateThread;
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_multi:
                calculateThread = new CalculateThread();
                calculateThread.start();
                break;
            case R.id.btn_async:
                new MyAsyncTask(this).execute(100);
                break;
            case R.id.Handler:
                new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
                break;
            case R.id.AsynTask:
                new DownloadImage(this).execute(DOWNLOAD_URL);
                break;
            case R.id.other:
                break;
        }

    }
    private static final int START_NUM=1;
    private static final int ADDING_NUM=2;
    private static final int ENDING_NUM=3;
    private static final int CANCEL_NUM=4;
    private MyHandler myHandler=new MyHandler(this);

    static class MyHandler extends Handler {
        private WeakReference<Activity> ref;

        public MyHandler(Activity activity){
            this.ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainActivity activity = (MainActivity) ref.get();
            if (activity==null){
                return;
            }
            switch (msg.what){
                case START_NUM:
                    activity.progress_bar.setVisibility(View.VISIBLE);
                    break;
                case ADDING_NUM:
                    activity.progress_bar.setVisibility(msg.arg1);
                    activity.tv_progress.setText("计算机已完成"+msg.arg1+"%");
                    break;
                case ENDING_NUM:
                    activity.progress_bar.setVisibility(View.GONE);
                    activity.tv_progress.setText("计算机已完成，结果为："+msg.arg1);
                    activity.Handler.removeCallbacks(activity.calculateThread);
                    break;
                case CANCEL_NUM:
                    activity.progress_bar.setProgress(0);
                    activity.progress_bar.setVisibility(View.GONE);
                    activity.tv_progress.setText("计算已取消");
                    break;
            }
        }
    }
    private MyUIHandler uiHandler=new MyUIHandler(this);
    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;

    static class MyUIHandler extends Handler {
        private WeakReference<Activity> ref;

        public MyUIHandler(Activity activity){
            this.ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainActivity activity = (MainActivity) ref.get();
            if (activity==null){
                return;
            }
            switch (msg.what){
                case MSG_SHOW_PROGRESS:
                    activity.progress_bar.setVisibility(View.VISIBLE);
                    break;
                case MSG_SHOW_IMAGE:
                    activity.progress_bar.setVisibility(View.GONE);
                    activity.img_pic.setImageBitmap((Bitmap) msg.obj);
                    break;
            }
        }
    }
    private class DownloadImageFetcher implements Runnable{
        private String imgUrl;

        public DownloadImageFetcher(String strUrl){
            this.imgUrl = strUrl;
        }

        @Override
        public void run() {
            InputStream in = null;
            uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();

            try{
                URL url = new URL(imgUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                in = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);

                Message msg = uiHandler.obtainMessage();
                msg.what = MSG_SHOW_IMAGE;
                msg.obj = bitmap;
                uiHandler.sendMessage(msg);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if (in != null){
                    try{
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    class CalculateThread extends Thread{
        @Override
        public void run(){
            int result = 0;
            boolean isCancel = false;

            myHandler.sendEmptyMessage(START_NUM);

            for(int i = 0;i<=100;i++){
                try{
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                if (i % 5 == 0){
                    Message msg = Message.obtain();
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    myHandler.sendMessage(msg);
                }
            }
            if (!isCancel){
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);
            }
        }
    }

    static class MyAsyncTask extends AsyncTask<Integer,Integer,Integer> {
        private WeakReference<AppCompatActivity>ref;

        public MyAsyncTask(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            MainActivity activity = (MainActivity)this.ref.get();
            activity.progress_bar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int sleep = params[0];
            int result = 0;
            for (int i=0;i<101;i++){
                try {
                    Thread.sleep(sleep);
                    result+=i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (i%5==0){
                    publishProgress(i);
                }
                if (isCancelled()){
                    break;
                }
            }return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            MainActivity activity = (MainActivity)this.ref.get();
            activity.progress_bar.setProgress(values[0]);
            activity.tv_progress.setText("计算机已完成"+values[0]+"%");
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            MainActivity activity = (MainActivity) this.ref.get();
            activity.tv_progress.setText("已完成计算，结果为："+result);
            activity.progress_bar.setVisibility(View.GONE);

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            MainActivity activity = (MainActivity) this.ref.get();
            activity.tv_progress.setText("计算机已取消");
            activity.progress_bar.setProgress(0);
            activity.progress_bar.setVisibility(View.GONE);
        }

        public void execute(int i) {
        }
    }

    private class DownloadImage extends AsyncTask<String,Bitmap,Bitmap>{

        private WeakReference<AppCompatActivity> ref;
        private String stUrl;

        public DownloadImage(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            InputStream stream = null;
            Bitmap bitmap = null;

            MainActivity activity = (MainActivity) this.ref.get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                URL url = new URL(stUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int totalLen = connection.getContentLength();
                if (totalLen==0){
                    activity.progress_bar.setProgress(0);
                }
                if (connection.getResponseCode()==200){
                    stream = connection.getInputStream();
                    int len = 1;
                    int progress = 0;
                    byte[] tmps = new byte[1024];
                    while ((len=stream.read(tmps))!=-1){
                        progress += len;
                        activity.progress_bar.setProgress(progress);
                        bos.write(tmps,0,len);
                    }
                    bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(),0,bos.size());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (stream!=null){
                    try{
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            MainActivity activity = (MainActivity) this.ref.get();
            if (bitmap!=null){
                activity.img_pic.setImageBitmap(bitmap);
            }
        }
    }
}

