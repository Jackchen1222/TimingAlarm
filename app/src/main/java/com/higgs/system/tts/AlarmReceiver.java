package com.higgs.system.tts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Field;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Utils.REMINDERS)){
            Bundle bundle = intent.getBundleExtra(Utils.BundleExtraFlag);
            String common = bundle.getString("content");
            Log.e(TAG, "common=" + common );
            Toast.makeText(context, "" + common , Toast.LENGTH_LONG).show();
            if(BellControl.getInstance().isPlaying()){
                BellControl.getInstance().stopRing();
            }
            BellControl.getInstance().defaultAlarmMediaPlayer();
            TimingAlarmActivity.mSpeakerServiceBinder.speek(common);
        }
    }


}
