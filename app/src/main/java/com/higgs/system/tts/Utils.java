package com.higgs.system.tts;

public class Utils {
    public static final String REMINDERS = "jc.help.zx.alarm";
    public static final String BundleExtraFlag = "onceAlarm";
    public static final String excelFilePath = "excel_file_path";

    public static final int LowComerTime = 0;
    public static final int LateComerConute = 3;

    public enum ExcelOnceCellType{
        /** 发起闹铃的当天时间 */
        TO(0,"StartTime", "发起时间"),
        TL(1,"ContinueTime", "播放的时间长度"),
        SC(2,"ScreenContent", "屏幕提示内容"),
        VC(3,"VoiceContent", "语音提示内容"),
        RM(4,"RemarkStr", "备注");
        int type;
        String contentStr;
        String explain;
        ExcelOnceCellType(int ty, String cs, String ep){
            this.type = ty;
            this.contentStr = cs;
            this.explain = ep;
        }

        public String getContentStr(){
            return this.contentStr;
        }

        public String getExplain(){
            return explain;
        }

        public int getCellType(){
            return type;
        }

        public String getName(){
            return this.name();
        }
    }

}
