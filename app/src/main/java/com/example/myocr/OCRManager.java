package com.example.myocr;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OCRManager {
    private static final String TAG = "OCRManager";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess/";
    public static final String lang = "eng";
    private TessBaseAPI tessBaseAPI;

    /**
     * Initial the data
     * @param
     * @return
     */
    public void prepareTessData(Context context){
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };
        Log.d(TAG, "prepareTessData: " + paths);
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.d(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.d(TAG, "Created directory " + path + " on storage");
                }
            }

        }
        if (!(new File(DATA_PATH + "tessdata/"   + lang + ".traineddata")).exists()) {
            try {
                AssetManager assetManager = context.getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.d(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.d(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }
    }

    public String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH, lang);
        tessBaseAPI.setImage(bitmap);
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ1234567890',.?;/ ");
        tessBaseAPI.setDebug(true);
//        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);

        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
            Log.d(TAG, "getText: " + retStr);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }

    public void onDestroy() {
        if (tessBaseAPI != null)
            tessBaseAPI.end();
    }
}
