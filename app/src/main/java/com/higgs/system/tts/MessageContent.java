package com.higgs.system.tts;

public class MessageContent {
    String speakContent;
    String screenContent;
    int durationTime;
    boolean iscontinueSpeaker;

    public MessageContent(){
        speakContent = "";
        screenContent = "";
        durationTime = 0;
        iscontinueSpeaker = false;
    }

}
