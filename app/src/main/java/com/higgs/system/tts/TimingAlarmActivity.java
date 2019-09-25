package com.higgs.system.tts;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.tts.client.SpeechError;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.codekidlabs.storagechooser.utils.DiskUtil;
import com.dalong.marqueeview.MarqueeView;
import com.help.excel.DirTraversal;
import com.help.excel.LogUtils;
import com.help.excel.PermissionsUtils;
import com.help.excel.SaveParameter;
import com.higgs.system.tts.ExcelFileOperate.FileLocation;
import com.higgs.system.tts.SpeakerService.SpeakerServiceBinder;
import com.higgs.system.tts.Utils.ExcelOnceCellType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.logging.SimpleFormatter;

public class TimingAlarmActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "TimingAlarmActivity";
    public static Context mContext;
    public static SpeakerServiceBinder mSpeakerServiceBinder;

    private ServiceConnection mconnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.e(TAG, "speaker service connect!");
            mSpeakerServiceBinder = (SpeakerServiceBinder)iBinder;
            mSpeakerServiceBinder.initListener(new MyTtsListener());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeakerServiceBinder = null;
            LogUtils.e(TAG, "speaker service disconnect!");
        }
    };

    private Map<Integer, Map<Integer, String>> excelMap;
    private Handler mHandler;
    private ReadExcelFileContentThread mReadExcelFileContentThread;
    private SaveParameter mSaveParameter;
    private boolean forceModifyExcelFile;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;
    private Button btnChooseExcelPath,btnStopCurrentAlarm, btnStartSetAlarm, btnTest;
    private TextView tvShowExcelPath;
    public static MarqueeView mvRollScreenContent;
    private static int countAlarm;
    private int isTestAlarmId;
    private boolean isTestStates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initStatus();
    }

    private void initView(){
        btnChooseExcelPath = findViewById(R.id.btn_choose_file_path);
        btnChooseExcelPath.setOnClickListener(this);
        btnStopCurrentAlarm = findViewById(R.id.stopCurrentAlarm);
        btnStopCurrentAlarm.setOnClickListener(this);
        btnStartSetAlarm = findViewById(R.id.startSettingAlarm);
        btnStartSetAlarm.setOnClickListener(this);
        tvShowExcelPath = findViewById(R.id.showExcelPath);
        mvRollScreenContent = findViewById(R.id.rollScreenContent);
        btnTest = findViewById(R.id.testAlarm);
        btnTest.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_choose_file_path:
                chooser = builder.build();
                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
                        LogUtils.e(TAG, "path=" + path);
                        tvShowExcelPath.setText(path);
                        mSaveParameter.setEditorValue(Utils.excelFilePath, path );
                        Toast.makeText(TimingAlarmActivity.this, "您选择的路径是" + path, Toast.LENGTH_SHORT).show();
                    }
                });

                chooser.setOnCancelListener(new StorageChooser.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        Toast.makeText(getApplicationContext(), "Storage Chooser Cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });

                chooser.setOnMultipleSelectListener(new StorageChooser.OnMultipleSelectListener() {
                    @Override
                    public void onDone(ArrayList<String> selectedFilePaths) {
                        for(String s: selectedFilePaths) {
                            LogUtils.e(TAG, s);
                        }
                    }
                });

                chooser.show();
                break;
            case R.id.stopCurrentAlarm:
                BellControl.getInstance().stopRing();
                break;
            case R.id.startSettingAlarm:
                countAlarm = 0;
                mHandler.post(mReadExcelFileContentThread);
                break;
            case R.id.testAlarm:
                if(isTestStates){
                    BellControl.getInstance().defaultAlarmMediaPlayer();
//                    mSpeakerServiceBinder.speek("你好呀,小朋友！");
                    btnTest.setText("关闭");
                    isTestStates = false;
                }else{
                    if(BellControl.getInstance().isPlaying()){
                        BellControl.getInstance().stopRing();
                    }
//                    mSpeakerServiceBinder.stop();
                    btnTest.setText("开启");
                    isTestStates = true;
                }
                try {
                    throw new Exception();
                } catch (Exception e) {
                    LogUtils.e(TAG, "testAlarm status=" + isTestStates);
                }

//                Calendar calendar = Calendar.getInstance();
//                Date date = calendar.getTime();
//                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//                String startTime = sdf.format(date);
//                addAlarm(startTime, "0:20", "闹铃时间到了!", "你好呀，小朋友", "无", isTestAlarmId++);
                break;
            default:
        }
    }


    private synchronized void initStatus(){
        mContext = this;
        isTestAlarmId = 1000;
        isTestStates = false;
        forceModifyExcelFile = false;
        // ----------------- Localization -------------------
        Content c = new Content();
        c.setCreateLabel("Create");
        c.setInternalStorageText("My Storage");
        c.setCancelLabel("Cancel");
        c.setSelectLabel("Select");
        c.setOverviewHeading("Choose Drive");

        builder.withActivity(this)
                .withFragmentManager(getFragmentManager())
                .setMemoryBarHeight(1.5f)
//                .disableMultiSelect()
                .withContent(c);
        builder.crunch();
        builder.setType(StorageChooser.FILE_PICKER);
        builder.skipOverview(true);
        builder.allowCustomPath(true);

        ArrayList<String> formats = new ArrayList<>();
        formats.add("xlsx");
        formats.add("xls");
        builder.customFilter(formats);

        mSaveParameter = new SaveParameter(this);
        mHandler = new Handler();
        mReadExcelFileContentThread = new ReadExcelFileContentThread();
        if(mSaveParameter.getEditorValue(Utils.excelFilePath) != null) {
            tvShowExcelPath.setText(mSaveParameter.getEditorValue(Utils.excelFilePath));
        }
        getpermission();
//        bindSpeakerService();
        BellControl.context = this;
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    private class ReadExcelFileContentThread implements Runnable{

        @Override
        public void run() {
            int typeLen = Utils.ExcelOnceCellType.values().length;
            int[] cellOrder = new int[typeLen];
            ExcelFileOperate efo = new ExcelFileOperate();
            String readExcelFilePath = mSaveParameter.getEditorValue(Utils.excelFilePath);
            if(readExcelFilePath != null && !readExcelFilePath.trim().equals("")){
                excelMap = efo.readExcelFile(readExcelFilePath, FileLocation.PATH );
            }else{
                Toast.makeText(TimingAlarmActivity.this, "Excel文件路径有问题!", Toast.LENGTH_LONG).show();
                return ;
            }
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
//                        for(int i = 0;i < typeLen; i++){
//                            LogUtils.e(TAG, "cellOrder[" + i + "]=" + cellOrder[i]);
//                        }
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
            if(countAlarm > 0){
                LogUtils.e(TAG, "一共添加了" + countAlarm + "个闹铃" );
                Toast.makeText(TimingAlarmActivity.this,
                        "一共添加了" + countAlarm + "个闹铃",
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private void bindSpeakerService(){
        Intent intent = new Intent(this, SpeakerService.class);
        bindService(intent, mconnection, Service.BIND_AUTO_CREATE);
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
        LogUtils.e( TAG, "startTime=" + startTime + ",continueTime=" + continueTime
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
            countAlarm++;
        }
    }

    private void deleteAlarm(){
        Intent intent =new Intent(this, AlarmReceiver.class);
        intent.setAction(Utils.REMINDERS);
        PendingIntent sender= PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarm=(AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.cancel(sender);
    }

    //创建监听权限的接口对象
    PermissionsUtils.IPermissionsResult permissionsResult = new PermissionsUtils.IPermissionsResult() {
        @Override
        public void passPermissons() {
            //权限通过执行的方法
            //权限通过验证
        }

        @Override
        public void forbitPermissons() {
            //这是没有通过权限的时候提示的内容，自定义即可
            Toast.makeText(mContext, "您没有允许部分权限，可能会导致部分功能不能正常使用，如需正常使用  请允许权限", Toast.LENGTH_SHORT).show();
            finish();
//            Tool.exitApp();
        }
    };
    private void getpermission() {
        //两个日历权限和一个数据读写权限
        String[] permissions = new String[]{
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.CAMERA
        };
        PermissionsUtils.showSystemSetting = true;//是否支持显示系统设置权限设置窗口跳转
        //这里的this不是上下文，是Activity对象！
        PermissionsUtils.getInstance().chekPermissions(this, permissions, permissionsResult);
    }

    private class MyTtsListener implements BaiduTtsListener.TtsListener{

        @Override
        public void speechStart(String s) {
            LogUtils.e(TAG, "speechStart");
        }

        @Override
        public void speechFinish(String s) {
            LogUtils.e(TAG, "speechFinish");
        }

        @Override
        public void speechProgress(String s, int i) {

        }

        @Override
        public void speechError(String s, SpeechError speechError) {

        }
    }

}
