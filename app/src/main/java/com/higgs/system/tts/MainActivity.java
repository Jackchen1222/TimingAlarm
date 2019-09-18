package com.higgs.system.tts;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import com.higgs.system.tts.SpeakerService.SpeakerServiceBinder;
import java.util.Calendar;

public class MainActivity extends Activity implements View.OnClickListener{

    private SpeakerServiceBinder mSpeakerServiceBinder;

    private ServiceConnection mconnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mSpeakerServiceBinder = (SpeakerServiceBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.startAlarm).setOnClickListener(this);
//        init();
        bindSpeakerService();
    }

    private void bindSpeakerService(){
        Intent intent = new Intent(this, SpeakerService.class);
        bindService(intent, mconnection, Service.BIND_AUTO_CREATE);
    }

    private void init(){
        Intent intent =new Intent(this, AlarmReceiver.class);
        PendingIntent sender= PendingIntent.getBroadcast(this, 0, intent, 0);
        Calendar calendar= Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());//将时间设定为系统目前的时间
        calendar.add(Calendar.SECOND, 5);//系统时间推迟五秒钟，如果为-5，那么就是比系统时间提前五秒钟
        AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startAlarm:
                if( mSpeakerServiceBinder != null ){
                    mSpeakerServiceBinder.speek("你好呀,");
                }
                break;
                default:
        }
    }

}
