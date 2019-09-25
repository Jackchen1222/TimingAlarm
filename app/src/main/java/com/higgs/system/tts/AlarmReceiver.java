package com.higgs.system.tts;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import com.help.excel.ExcelRowDataAccept;
import com.higgs.system.tts.Utils.ExcelOnceCellType;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private ExcelRowDataAccept mExcelRowDataAccept;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Utils.REMINDERS)){
            mExcelRowDataAccept = getAcceptData(intent.getBundleExtra(Utils.BundleExtraFlag));
            if(whetherTurnOnAlarm()){
                if(BellControl.getInstance().isPlaying()){
                    BellControl.getInstance().stopRing();
                }

                BellControl.getInstance().defaultAlarmMediaPlayer(
                        mExcelRowDataAccept.continueTMin * 60
                                + mExcelRowDataAccept.continueTSecond);

//                TimingAlarmActivity.mSpeakerServiceBinder.speek(mExcelRowDataAccept.voicePlay);
                TimingAlarmActivity.mvRollScreenContent.setText(mExcelRowDataAccept.screenShow);
                TimingAlarmActivity.mvRollScreenContent.startScroll();

            }
        }
    }

    /**
     * 判断是否响铃
     * @return
     */
    private boolean whetherTurnOnAlarm(){
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMin = calendar.get(Calendar.MINUTE);
        int currentTotalMin = currentHour * 60 + currentMin;
        int configTotalMin = mExcelRowDataAccept.startTHour * 60 + mExcelRowDataAccept.startTMin;
        int differenceValue = currentTotalMin - configTotalMin;
        if( differenceValue <= Utils.LateComerConute ){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 解析获取到的数据
     * @param bundle
     * @return
     */
    private ExcelRowDataAccept getAcceptData(Bundle bundle){
        ExcelRowDataAccept erda = new ExcelRowDataAccept();
        try {
            String startTiemStr = bundle.getString(ExcelOnceCellType.TO.contentStr);
            if(startTiemStr != null ){
                if(!startTiemStr.trim().equals("")){
                    String[] startTimeArray = startTiemStr.split(":");
                    erda.startTHour = Integer.valueOf(startTimeArray[0]);
                    erda.startTMin = Integer.valueOf(startTimeArray[1]);
                }
            }
            String continueTimeStr = bundle.getString(ExcelOnceCellType.TL.contentStr);
            if(continueTimeStr != null){
                if(!continueTimeStr.trim().equals("")){
                    String[] continueTimeArray = continueTimeStr.split(":");
                    erda.continueTMin = Integer.valueOf(continueTimeArray[0]);
                    erda.continueTSecond = Integer.valueOf(continueTimeArray[1]);
                }
            }
            erda.screenShow = bundle.getString(ExcelOnceCellType.SC.contentStr);
            erda.voicePlay = bundle.getString(ExcelOnceCellType.VC.contentStr);
            erda.remarkContent = bundle.getString(ExcelOnceCellType.RM.contentStr);
            erda.show();
        }catch (Exception e){
            e.printStackTrace();
        }
        return erda;
    }

}
