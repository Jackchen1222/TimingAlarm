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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class TimingAlarmActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    public static Context mContext;
    public static SpeakerServiceBinder mSpeakerServiceBinder;

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

    private Map<Integer, Map<Integer, String>> excelMap;
    private Handler mHandler;
    private ReadExcelFileContentThread mReadExcelFileContentThread;

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
        mHandler = new Handler();
        mReadExcelFileContentThread = new ReadExcelFileContentThread();
        mHandler.post(mReadExcelFileContentThread);
        bindSpeakerService();
        BellControl.context = this;
    }

    private class ReadExcelFileContentThread implements Runnable{
        @Override
        public void run() {
            int typeLen = Utils.ExcelOnceCellType.values().length;
            int[] cellOrder = new int[typeLen];
            ExcelFileOperate efo = new ExcelFileOperate();
            excelMap = efo.readExcelFile(null);
            Iterator<Map.Entry<Integer, Map<Integer, String>>> entries = excelMap.entrySet().iterator();
            while(entries.hasNext()){
                Map.Entry<Integer, Map<Integer, String>> rowEntry = entries.next();
                Map<Integer, String> cellEntry = rowEntry.getValue();
                String startT = "", continueT = "", screenC = "", voiceC = "", remarkS = "";
                for(Map.Entry<Integer, String> cEntry :cellEntry.entrySet()){
                    if(rowEntry.getKey() == 0){
                        String[] cellArray = cEntry.getValue().split("=");
                        for(int i = 0 ; i < typeLen; i++){
                            if(Utils.ExcelOnceCellType.values()[i].getName().equals(cellArray[0])){
                                cellOrder[cEntry.getKey()] = Utils.ExcelOnceCellType.values()[i].getCellType();
                                break;
                            }
                        }
                        for(int i = 0;i < typeLen; i++){
                            Log.e(TAG, "cellOrder[" + i + "]=" + cellOrder[i]);
                        }
                    }else{
                        int iorder = cEntry.getKey();
                        String value = cEntry.getValue();
                        if(Utils.ExcelOnceCellType.TO.getCellType() == cellOrder[iorder] ){
                            startT = value;
                        }else if(Utils.ExcelOnceCellType.TL.getCellType() == cellOrder[iorder]){
                            continueT = value;
                        }else if(Utils.ExcelOnceCellType.SC.getCellType() == cellOrder[iorder]){
                            screenC = value;
                        }else if(Utils.ExcelOnceCellType.VC.getCellType() == cellOrder[iorder]){
                            voiceC = value;
                        }else if(Utils.ExcelOnceCellType.RM.getCellType() == cellOrder[iorder]){
                            remarkS = value;
                        }
                    }
                }
                if(rowEntry.getKey() > 0) {
                    addAlarm(startT, continueT, screenC, voiceC, remarkS, rowEntry.getKey());
                }
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mHandler != null){
            mHandler.removeCallbacks(mReadExcelFileContentThread);
        }
        if(mSpeakerServiceBinder != null){
            unbindService(mconnection);
        }
    }

    private void addAlarm( String startTime, String continueTime, String screenContent,
                           String voiceContent, String remarkStr, int id){
        Log.e( TAG, "startTime=" + startTime + ",continueTime=" + continueTime
                + ",screenContent=" + screenContent + ",voiceContent=" + voiceContent
                + ",remarkStr=" + remarkStr + ",id=" + id );
        if(!startTime.trim().equals("")) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            Bundle bundle = new Bundle();
            bundle.putString(Utils.ExcelOnceCellType.TO.contentStr, startTime);
            bundle.putString(Utils.ExcelOnceCellType.TL.contentStr, continueTime);
            bundle.putString(Utils.ExcelOnceCellType.SC.contentStr, screenContent);
            bundle.putString(Utils.ExcelOnceCellType.VC.contentStr, voiceContent);
            bundle.putString(Utils.ExcelOnceCellType.RM.contentStr, remarkStr);
            intent.putExtra(Utils.BundleExtraFlag, bundle);
            intent.setAction(Utils.REMINDERS);
            PendingIntent sender = PendingIntent.getBroadcast(this, id, intent, 0);
            String[] startTimeArray = startTime.split(":");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startTimeArray[0]));
            calendar.set(Calendar.MINUTE, Integer.valueOf(startTimeArray[1]));
//        calendar.setTimeInMillis(System.currentTimeMillis());//将时间设定为系统目前的时间
//        calendar.add(Calendar.SECOND, time );//系统时间推迟五秒钟，如果为-5，那么就是比系统时间提前五秒钟
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
        }
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
