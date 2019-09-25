package com.help.excel;

import com.tencent.bugly.crashreport.BuglyLog;

public class LogUtils {
    public static final String appId = "810eb7f9c8";
    public static final String appKey = "15105ee2-d15c-4bd9-9bec-72cdd224c985";

    public static void v(String tag, String content){
        BuglyLog.v(tag, content);
    }

    public static void e(String tag, String content){
        BuglyLog.e(tag, content);
    }

    public static void w(String tag, String content){
        BuglyLog.w(tag, content);
    }

    public static void i(String tag, String content){
        BuglyLog.i(tag, content);
    }

    public static void d(String tag, String content){
        BuglyLog.d(tag, content);
    }

}
