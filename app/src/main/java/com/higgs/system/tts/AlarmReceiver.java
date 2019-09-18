package com.higgs.system.tts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.widget.Toast;
import java.lang.reflect.Field;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "short alarm", Toast.LENGTH_LONG).show();
        defaultAlarmMediaPlayer( context );
    }

    //反射设置闹铃重复播放
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

    public void defaultAlarmMediaPlayer(Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        setRingtoneRepeat(r);
        r.play();
    }
}
