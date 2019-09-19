package com.higgs.system.tts;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.baidu.tts.client.SpeechError;
import com.higgs.system.tts.SpeakerService.SpeakerServiceBinder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;

public class TimingAlarmActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    public static Context mContext;
    public static SpeakerServiceBinder mSpeakerServiceBinder;
    private Map<Integer, Map<Integer, String>> excelMap;
    private ServiceConnection mconnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "speaker service connect!");
            mSpeakerServiceBinder = (SpeakerServiceBinder)iBinder;
            mSpeakerServiceBinder.initListener(new MyTtsListener());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeakerServiceBinder = null;
            Log.e(TAG, "speaker service disconnect!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initStatus();
    }

    private void initView(){
        findViewById(R.id.startAlarm).setOnClickListener(this);
        findViewById(R.id.deleteAlarm).setOnClickListener(this);
    }

    private synchronized void initStatus(){
        mContext = this;
        ExcelFileOperate efo = new ExcelFileOperate();
        efo.readExcelFile(null);

        bindSpeakerService();
        addAlarm("你好呀", 5, 1);
        addAlarm("发生了什么", 10, 2);
        BellControl.context = this;
    }

    private void bindSpeakerService(){
        Intent intent = new Intent(this, SpeakerService.class);
        bindService(intent, mconnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mSpeakerServiceBinder == null){
            bindSpeakerService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if(mSpeakerServiceBinder != null) {
            unbindService(mconnection);
        }
    }

    private void addAlarm(String speechContent, int time, int id){
        Intent intent =new Intent(this, AlarmReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putString("content", speechContent);
        intent.putExtra(Utils.BundleExtraFlag,bundle);
        intent.setAction(Utils.REMINDERS);
        PendingIntent sender= PendingIntent.getBroadcast(this, id, intent, 0);
        Calendar calendar= Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());//将时间设定为系统目前的时间
        calendar.add(Calendar.SECOND, time );//系统时间推迟五秒钟，如果为-5，那么就是比系统时间提前五秒钟
        AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    private void deleteAlarm(){
        Intent intent =new Intent(this, AlarmReceiver.class);
        intent.setAction(Utils.REMINDERS);
        PendingIntent sender= PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.cancel(sender);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startAlarm:
                if( mSpeakerServiceBinder != null ){
                    mSpeakerServiceBinder.speek("你好呀,");
                }
                break;
            case R.id.deleteAlarm:
                BellControl.getInstance().stopRing();
                break;
                default:
        }
    }

    private class MyTtsListener implements BaiduTtsListener.TtsListener{

        @Override
        public void speechStart(String s) {
            Log.e(TAG, "speechStart");
        }

        @Override
        public void speechFinish(String s) {
            Log.e(TAG, "speechFinish");
        }

        @Override
        public void speechProgress(String s, int i) {

        }

        @Override
        public void speechError(String s, SpeechError speechError) {

        }
    }

}
