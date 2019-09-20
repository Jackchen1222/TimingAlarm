package com.higgs.system.tts;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
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
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.baidu.tts.client.SpeechError;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.help.excel.DirTraversal;
import com.help.excel.PermissionsUtils;
import com.help.excel.SaveParameter;
import com.higgs.system.tts.ExcelFileOperate.FileLocation;
import com.higgs.system.tts.SpeakerService.SpeakerServiceBinder;
import com.higgs.system.tts.Utils.ExcelOnceCellType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
    private SaveParameter mSaveParameter;
    private boolean forceModifyExcelFile;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;

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

        mSaveParameter = new SaveParameter(this);
        mHandler = new Handler();
        mReadExcelFileContentThread = new ReadExcelFileContentThread();
//        mHandler.post(mReadExcelFileContentThread);
        getpermission();
        bindSpeakerService();
        BellControl.context = this;
    }

    private String[] dialogItemts;

    private class getExcelFileList implements Runnable{
        @Override
        public void run() {
            DirTraversal dirTraversal = new DirTraversal();
            List<File> dialogFileList = dirTraversal.listFiles("/sdcard/tencent/");
//            List<File> dialogFileList = dirTraversal.listFiles("/sdcard/tencent/QQfile_recv/");
//            List<File> tempList = dirTraversal.listFiles("/sdcard/tencent/MicroMsg/download/");
            if(dialogFileList != null){

            }else{
                dialogItemts = null;
            }
        }
    }


    private class ReadExcelFileContentThread implements Runnable{
        private int dialogYourChoice;
        private String dialogExcelFilePath;
        private List<File> dialogFileList;
        private String[] dialogItemts;
        private void showListDialog(){

            dialogItemts = new String[dialogFileList.size()];
            dialogYourChoice = 0;
            for(File file : dialogFileList ){
                dialogItemts[dialogYourChoice++] = file.getName();
            }
            dialogYourChoice = 0;
            AlertDialog.Builder sigleChoiceDialog = new Builder(TimingAlarmActivity.this);
            sigleChoiceDialog.setTitle("请选择Excel文件");
            sigleChoiceDialog.setSingleChoiceItems(dialogItemts, 0, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogYourChoice = i;
                    for(File file : dialogFileList){
                        if(file.getName().equals(dialogItemts[i])){
                            dialogExcelFilePath = file.getPath();
                            break;
                        }
                    }
                }
            });
            sigleChoiceDialog.setPositiveButton("确认", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.e(TAG, "choice excel file path:" + dialogExcelFilePath );
                    Toast.makeText(TimingAlarmActivity.this,
                            "你选择了" + dialogItemts[dialogYourChoice],
                            Toast.LENGTH_SHORT ).show();
                    mSaveParameter.setEditorValue(Utils.excelFilePath, dialogExcelFilePath );
                    dialogInterface.dismiss();
                }
            });
            sigleChoiceDialog.setNegativeButton("取消", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.e(TAG, "取消了选择!");
                    dialogExcelFilePath = null;
                    dialogInterface.dismiss();
                }
            });
            sigleChoiceDialog.show();
        }

        @Override
        public void run() {
            int typeLen = Utils.ExcelOnceCellType.values().length;
            int[] cellOrder = new int[typeLen];
            ExcelFileOperate efo = new ExcelFileOperate();
            String readExcelFilePath = mSaveParameter.getEditorValue(Utils.excelFilePath);
            if(forceModifyExcelFile || readExcelFilePath == null){
                showListDialog();
            }
            if(readExcelFilePath == null){
                Toast.makeText(TimingAlarmActivity.this, "没有Excel文件!", Toast.LENGTH_LONG).show();
                return ;
            }
            excelMap = efo.readExcelFile(readExcelFilePath, FileLocation.PATH );
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
//                            Log.e(TAG, "cellOrder[" + i + "]=" + cellOrder[i]);
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
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
                break;
            case R.id.deleteAlarm:
                BellControl.getInstance().stopRing();
                mSaveParameter.deleteAllValue();
                break;
                default:
        }
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
