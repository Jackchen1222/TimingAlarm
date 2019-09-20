package com.help.excel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SaveParameter {
    private static final String TAG = "UpdateParameter";
    private Context mContext;
    private SharedPreferences.Editor editor = null;
    private SharedPreferences preferences = null;
    private String parameterFileName = "alarm_param";

    public SaveParameter(Context context){
        mContext = context;
        InitParameter(null);
    }

    public SaveParameter(Context context, String fileName){
        mContext = context;
        InitParameter(fileName);
    }

    private void InitParameter(String fn){
        if( fn == null ) {
            preferences = mContext.getSharedPreferences( parameterFileName , Context.MODE_PRIVATE );
        }else{
            preferences = mContext.getSharedPreferences(fn, Context.MODE_PRIVATE );
        }
        if(preferences != null) {
            editor = preferences.edit();
        }
    }

    /** 删除 */
    public boolean deleteEditorValue(String key){
        if(editor != null) {
            Log.i(TAG, "delete Editor [" + key + "]");
            editor.remove(key);
            return editor.commit();
        }
        return false;
    }

    /** 添加与修改 */
    public boolean setEditorValue(String key , String value ){
        if(editor != null) {
            Log.i(TAG, "set Editor [" + key + "]=[" + value + "]");
            editor.putString(key, value);
            return editor.commit();
        }
        return false;
    }

    public boolean setEditorValue(String key , float value ){
        if(editor != null) {
            Log.i(TAG, "set Editor [" + key + "]=[" + value + "]");
            editor.putFloat(key, value);
            return editor.commit();
        }
        return false;
    }

    public boolean setEditorValue(String key , long value ){
        if(editor != null) {
            Log.i(TAG, "set Editor [" + key + "]=[" + value + "]");
            editor.putLong(key, value);
            return editor.commit();
        }
        return false;
    }

    public boolean setEditorValue(String key , boolean value ){
        if(editor != null) {
            Log.i(TAG, "set Editor [" + key + "]=[" + value + "]");
            editor.putBoolean(key, value);
            return editor.commit();
        }
        return false;
    }

    public boolean setEditorValue(String key , int value ){
        if(editor != null) {
            Log.i(TAG, "set Editor [" + key + "]=[" + value + "]");
            editor.putInt(key, value);
            return editor.commit();
        }
        return false;
    }

    /** 查找 */
    public String getEditorValue(String key){
        if(preferences != null) {
            Log.i(TAG, "get Eidtor [" + key + "]");
            return preferences.getString(key, null);
        }
        return null;
    }

    public void deleteAllValue(){
        editor.clear();
        editor.commit();
    }

}
