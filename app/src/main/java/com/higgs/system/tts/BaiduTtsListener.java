package com.higgs.system.tts;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizerListener;

public class BaiduTtsListener implements SpeechSynthesizerListener {
    private static final String TAG = "BaiduTtsListener";
    private TtsListener listener;

    public interface TtsListener{
        void speechStart(String s);
        void speechFinish(String s);
        void speechProgress(String s, int i);
        void speechError(String s, SpeechError speechError);
    }

    public BaiduTtsListener(TtsListener ttsl){
        listener = ttsl;
    }

    @Override
    public void onSynthesizeStart(String s) {

    }

    @Override
    public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {

    }

    @Override
    public void onSynthesizeFinish(String s) {

    }

    @Override
    public void onSpeechStart(String s) {
        if(listener != null){
            listener.speechStart(s);
        }
    }

    @Override
    public void onSpeechProgressChanged(String s, int i) {
        if(listener != null){
            listener.speechProgress(s,i);
        }
    }

    @Override
    public void onSpeechFinish(String s) {
        if(listener != null){
            listener.speechFinish(s);
        }else{
            if(TimingAlarmActivity.isContinueSpeaker){
                TimingAlarmActivity.mSpeakerServiceBinder.speek(TimingAlarmActivity.speakerContentStr);
            }
        }
    }

    @Override
    public void onError(String s, SpeechError speechError) {
        if(listener != null){
            listener.speechError( s, speechError);
        }
    }
}
