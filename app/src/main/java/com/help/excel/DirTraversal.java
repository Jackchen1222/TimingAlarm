package com.help.excel;

import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

public class DirTraversal {
    private static final String TAG = "DirTraversal";

    private static final String suffixStr = "xlsx";
    /** 没有递归 */
    public static LinkedList<File> listLinkedFiles(String strPath) {
        LinkedList<File> list = new LinkedList<File>();
        try {
            File dir = new File(strPath);
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    list.add(files[i]);
                }else {
//                    if(files[i].getName().toLowerCase().endsWith( suffixStr )){
//                        Log.e(TAG, files[i].getAbsolutePath());
//                        list.add(files[i]);
//                    }
                }
            }
            File tmp;
            while (!list.isEmpty()) {
                tmp = (File) list.removeFirst();
                if (tmp.isDirectory()) {
                    files = tmp.listFiles();
                    if (files == null) {
                        continue;
                    }
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
//                            list.add(files[i]);
                        }else {
                            if(files[i].getName().toLowerCase().endsWith( suffixStr )){
                                Log.e(TAG, files[i].getAbsolutePath());
                                list.add(files[i]);
                            }
                        }
                    }
                } else {
                    if(tmp.getName().toLowerCase().endsWith( suffixStr )){
                        Log.e(TAG, tmp.getAbsolutePath());
                        list.add(tmp);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }


    //recursion
    public ArrayList<File> listFiles(String strPath) {
        filelist = new ArrayList<File>();
        return refreshFileList(strPath);
    }

    private ArrayList<File> filelist;

    public ArrayList<File> refreshFileList(String strPath) {
        File dir = new File(strPath);
        File[] files = dir.listFiles();

        if (files == null) {
            return null;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                refreshFileList(files[i].getAbsolutePath());
            } else {
                if(files[i].getName().toLowerCase().endsWith( suffixStr )) {
                    filelist.add(files[i]);
                    Log.e(TAG, files[i].getAbsolutePath() );
                }
            }
        }
        return filelist;
    }

}
