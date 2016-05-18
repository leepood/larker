package com.leepood.lark.models;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by leepood on 6/18/15.
 */
public class RecordInfo {

    private Context mContext;

    public static final int QUALITY_LOW     =  1 << 0;
    public static final int QUALITY_MEDIUM  =  1 << 1;
    public static final int QUALITY_HIGH    =  1 << 2;

    private int rate;
    private int dpi;
    private int videoWidth;
    private int videoHeight;

    private int quality = QUALITY_MEDIUM;

    /**
     * Create a new RecordInfo according to the perference
     * @param ctx
     * @return
     */
    public  static RecordInfo createRecordInfo(Context ctx){
        RecordInfo info = new RecordInfo(ctx);

        return info;
    }

    public RecordInfo(Context ctx){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService(ctx.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);

        dpi         =  displayMetrics.densityDpi;
        videoWidth  =  400;//displayMetrics.widthPixels;
        videoHeight =  800;//displayMetrics.heightPixels;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }
}
