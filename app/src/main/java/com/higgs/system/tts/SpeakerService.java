package com.higgs.system.tts;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;
import com.higgs.system.tts.BaiduTtsListener.TtsListener;

public class SpeakerService extends Service {
    private static final String TAG = "SpeakerService";

    private final String appId = "11589368";
    private final String appKey = "FHTDvybZS3dheAEoqDBGG92R";
    private final String secretKey = "Kb1Kz1TkKCP4s7rGjIRCHTDhG6ab0TNj";

    private SpeechSynthesizer mSpeechSynthesizer;
    private TtsMode mTtsMode = TtsMode.MIX;
    private OperationHandler mOperationHandler;
    private TtsListener mTtsListener = null;

    @Override
    public void onCreate() {
        super.onCreate();
        initValue();
    }

    private void initValue(){
        mOperationHandler = new OperationHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private enum OperationType{
        /** 说话 */
        SPEAK ,
        /** 暂停 */
        PAUSE,
        /** 继续 */
        RESUME,
        /** 暂停 */
        STOP,
    }

    private class OperationHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            if(what == OperationType.SPEAK.ordinal()){
                if(mSpeechSynthesizer != null){
                    mSpeechSynthesizer.speak((String)msg.obj);
                }
            }else if(what == OperationType.RESUME.ordinal()){
                if(mSpeechSynthesizer != null){
                    mSpeechSynthesizer.resume();
                }
            }else if(what == OperationType.PAUSE.ordinal()){
                if(mSpeechSynthesizer != null){
                    mSpeechSynthesizer.pause();
                }
            }else if(what == OperationType.STOP.ordinal()){
                if(mSpeechSynthesizer != null){
                    mSpeechSynthesizer.stop();
                }
            }

        }
    }

    public class SpeakerServiceBinder extends Binder{
        public void initListener(TtsListener tl){
            mTtsListener = tl;
            baiduTtsInit();
        }

        public void speek(String content){
            Message msg = new Message();
            msg.what = OperationType.SPEAK.ordinal();
            msg.obj = content;
            mOperationHandler.sendMessage(msg);
        }

        public void resume(){
            mOperationHandler.sendEmptyMessage(OperationType.RESUME.ordinal());
        }

        public void pause(){
            mOperationHandler.sendEmptyMessage(OperationType.PAUSE.ordinal());
        }

        public void stop(){
            mOperationHandler.sendEmptyMessage(OperationType.STOP.ordinal());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new SpeakerServiceBinder();
    }

    public void baiduTtsInit() {

        boolean isMix = mTtsMode.equals(TtsMode.MIX);
        boolean isSuccess = false;

//        if(isMix){
//            isSuccess = checkOfflineResources();
//            if(isSuccess)
//                Log.i(TAG, "离线资源存在并可读,目录:" + bd_Config.TEMP_DIR );
//        }

        // 1. 获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);

        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener( new BaiduTtsListener(mTtsListener) );

        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setApiKey");
        // 4. 支持离线的话，需要设置离线模型

//        if (isMix) {
//            // 检查离线授权文件是否下载成功，离线授权文件联网时SDK自动下载管理，有效期3年，3年后的最后一个月自动更新。
//            isSuccess = checkAuth();
//            if (!isSuccess) {
//                return;
//            }
        // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
//        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, null);
        // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
//        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, null);
//        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "4" );
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "15" );
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "4");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "9");

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        mSpeechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL);
        // 不使用压缩传输
        // mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, SpeechSynthesizer.AUDIO_ENCODE_PCM);
        // mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, SpeechSynthesizer.AUDIO_BITRATE_PCM);

        // 6. 初始化
        result = mSpeechSynthesizer.initTts(mTtsMode);
//        checkResult(result, "initTts");

    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.e( TAG, "error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }else{
            Log.i(TAG, "Success , " + method );
        }
    }

}
