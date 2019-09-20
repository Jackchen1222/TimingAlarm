package com.help.excel;

import android.util.Log;

public class ExcelRowDataAccept {
    private static final String TAG = "ExcelRowDataAccept";
    public int startTHour;
    public int startTMin;
    public int continueTMin;
    public int continueTSecond;
    public String screenShow;
    public String voicePlay;
    public String remarkContent;

    public ExcelRowDataAccept(){
        startTHour = 0;
        startTMin = 0;
        continueTMin = 0;
        continueTSecond = 0;
        screenShow = "";
        voicePlay = "";
        remarkContent = "";
    }

    public void show(){
        Log.e(TAG, "StartTime=" + startTHour + ":" + startTMin
                + ",continueTime=" + continueTMin + ":" + continueTSecond
                + ",screenShow=" + screenShow
                + ",voicePlay=" + voicePlay
                + ",remarkContent=" + remarkContent
            );
    }
}
