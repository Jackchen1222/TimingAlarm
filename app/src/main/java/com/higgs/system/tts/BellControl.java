package com.higgs.system.tts;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import java.lang.reflect.Field;

public class BellControl {
    private static final String TAG = "BellControl";
    public static Context context;
    private Uri notification;
    private Ringtone ring;
    private static BellControl bellControl;

    public static BellControl getInstance(){
        if(bellControl == null){
            bellControl = new BellControl();
        }
        return bellControl;
    }

    public void defaultAlarmMediaPlayer() {
        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ring = RingtoneManager.getRingtone(context, notification);
        setRingtoneRepeat(ring);
        ring.play();
    }

    /**
     * 反射设置闹铃重复播放
     */
    private void setRingtoneRepeat(Ringtone ringtone) {
        Class<Ringtone> clazz =Ringtone.class;
        try {
            Field field = clazz.getDeclaredField("mLocalPlayer");//返回一个 Field 对象，它反映此 Class 对象所表示的类或接口的指定公共成员字段（※这里要进源码查看属性字段）
            field.setAccessible(true);
            MediaPlayer target = (MediaPlayer) field.get(ringtone);//返回指定对象上此 Field 表示的字段的值
            target.setLooping(true);//设置循环
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying(){
        if(ring != null){
            return ring.isPlaying();
        }
        return false;
    }

    public void stopRing(){
        if(ring != null){
            ring.stop();
        }
    }

}
