package com.higgs.system.tts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;
import com.higgs.system.tts.BaiduTtsListener.TtsListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpeakerService extends Service {
    private static final String TAG = "SpeakerService";

    private final String appId = "11589368";
    private final String appKey = "FHTDvybZS3dheAEoqDBGG92R";
    private final String secretKey = "Kb1Kz1TkKCP4s7rGjIRCHTDhG6ab0TNj";

    private SpeechSynthesizer mSpeechSynthesizer;
    private TtsMode mTtsMode = TtsMode.MIX;
    private OperationHandler mOperationHandler;
    private TtsListener mTtsListener = null;

    private static final String TEMP_DIR = "/sdcard/baiduTTS";
    private static final String TEXT_NAME = "bd_etts_text.dat";
    private static final String TEXT_FILENAME = TEMP_DIR + "/" + TEXT_NAME;
    private static final String MODEL_NAME = "bd_etts_common_speech_yyjw_mand_eng_high_am-mix_v3.0.0_20170512.dat";
    private static final String MODEL_FILENAME = TEMP_DIR + "/" + MODEL_NAME;

    @Override
    public void onCreate() {
        super.onCreate();
        initValue();
    }

    private void initValue(){
        mOperationHandler = new OperationHandler();
        copyAssetFile(TEXT_NAME);
        copyAssetFile(MODEL_NAME);
    }

    private void copyAssetFile(String srcFileName){
        try {
            String destPath = createTmpDir(this);
            String destFileName = destPath + "/" + srcFileName;
            copyFromAssets(getApplicationContext().getAssets(), srcFileName, destFileName, true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            return file.mkdirs();
        } else {
            return true;
        }
    }

    public String createTmpDir(Context context) {
        String sampleDir = "baiduTTS";
        String tmpDir = Environment.getExternalStorageDirectory().toString() + "/" + sampleDir;
        if (!makeDir(tmpDir)) {
            tmpDir = context.getExternalFilesDir(sampleDir).getAbsolutePath();
            if (!makeDir(sampleDir)) {
                throw new RuntimeException("create model resources dir failed :" + tmpDir);
            }
        }
        return tmpDir;
    }

    public void copyFromAssets(AssetManager assets, String source, String dest, boolean isCover)
            throws IOException {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = assets.open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }
        }
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

        if(isMix){
            isSuccess = checkOfflineResources();
            if (!isSuccess) {
                return;
            } else {
                print("离线资源存在并且可读, 目录：" + TEMP_DIR );
            }
        }

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

        if (isMix) {
            // 检查离线授权文件是否下载成功，离线授权文件联网时SDK自动下载管理，有效期3年，3年后的最后一个月自动更新。
            isSuccess = checkAuth();
            if (!isSuccess) {
                return;
            }
            // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME );
            // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME );
        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "4" );
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9" );
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "3");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "8");

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

    /**
     * 检查appId ak sk 是否填写正确，另外检查官网应用内设置的包名是否与运行时的包名一致。本demo的包名定义在build.gradle文件中
     *
     * @return
     */
    private boolean checkAuth() {
        AuthInfo authInfo = mSpeechSynthesizer.auth(mTtsMode);
        if (!authInfo.isSuccess()) {
            // 离线授权需要网站上的应用填写包名。本demo的包名是com.baidu.tts.sample，定义在build.gradle中
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            print("【error】鉴权失败 errorMsg=" + errorMsg);
            return false;
        } else {
            print("验证通过，离线正式授权文件存在。");
            return true;
        }
    }
    /**
     * 检查 TEXT_FILENAME, MODEL_FILENAME 这2个文件是否存在，不存在请自行从assets目录里手动复制
     *
     * @return
     */
    private boolean checkOfflineResources() {
        String[] filenames = {TEXT_FILENAME, MODEL_FILENAME};
        for (String path : filenames) {
            File f = new File(path);
            if (!f.canRead()) {
                print("[ERROR] 文件不存在或者不可读取，请从assets目录复制同名文件到：" + path);
                print("[ERROR] 初始化失败！！！");
                return false;
            }
        }
        return true;
    }

    private void print(String message) {
        Log.i(TAG, message);
    }

}
