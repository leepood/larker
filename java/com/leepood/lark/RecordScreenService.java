package com.leepood.lark;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Surface;

import com.leepood.lark.encoder.EncoderThread;
import com.leepood.lark.models.RecordInfo;
import com.leepood.lark.utils.Common;
import com.leepood.lark.utils.LarkerNative;
import com.leepood.lark.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by leepood on 6/18/15.
 */
public class RecordScreenService extends Service implements EncoderThread.EncoderListener {

    private MediaProjectionManager projectionManager;
    private static final String DISPLAY_NAME = "lark_display";
    private MediaProjection projection;
    private VirtualDisplay display;

    private MediaCodec encoder = null;
    private EncoderThread encoderThread = null;
    private FileOutputStream fos = null;

    private LarkerNative mLarkerNative = null;

    private RecordInfo recordInfo;

    private static final int DEFAULT_FRAME_RATE = 15;
    private static final int DEFAULT_RTSP_PORT = 8554;
    private static final int DEFAULT_BIT_RATE = 1024;
    private static final int DEFAULT_I_FRAME = 2;
    private static final int DEFAULT_VIDEO_WIDTH = 480;
    private static final int DEFAULT_VIDEO_HEIGHT = 640;

    private int frameRate = DEFAULT_FRAME_RATE;
    private int port = DEFAULT_RTSP_PORT;
    private int bitRate = DEFAULT_BIT_RATE;
    private int iFrame = DEFAULT_I_FRAME;

    private int mVideoWidth = DEFAULT_VIDEO_WIDTH;
    private int mVideoHeight = DEFAULT_VIDEO_HEIGHT;

    public static final int STATE_STARTED = 0x00;
    public static final int STATE_STOPED = 0x01;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void loadSettings() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        frameRate = sp.getInt("frame_rate", DEFAULT_FRAME_RATE);
        port = sp.getInt("rtsp_port", DEFAULT_RTSP_PORT);
        bitRate = sp.getInt("bit_rate", DEFAULT_BIT_RATE);
        iFrame = sp.getInt("frame_i_interval", DEFAULT_I_FRAME);
        String videoSize = sp.getString("video_size", null);
        if (videoSize != null) {
            try {
                String[] size = videoSize.split("x");
                mVideoWidth = Integer.valueOf(size[1].trim());
                mVideoHeight = Integer.valueOf(size[0].trim());
            } catch (Exception ex) {
                Log.e("larker", "video_size error!ignore!");
            }
        }

        recordInfo = RecordInfo.createRecordInfo(this);
        recordInfo.setVideoWidth(mVideoWidth);
        recordInfo.setVideoHeight(mVideoHeight);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            if (intent.getAction() != null && intent.getAction().equals("stop")) {
                if (encoder != null) {
                    projection.stop();
                    encoder.stop();
                    encoder.release();
                    display.release();
                    mLarkerNative.stop();
                    mLarkerNative.destory();
                    stopService(new Intent(this, RecordScreenService.class));
                    notifyStatusChange(STATE_STOPED);
                }
            } else {
                try {
                    loadSettings();
                    mLarkerNative = new LarkerNative();
                    if (mLarkerNative.init(frameRate,port)) {
                        mLarkerNative.doLoop();
                        createRecordSession(intent.getIntExtra("resultCode", 0),
                                (Intent) intent.getParcelableExtra("data"));
                        notifyStatusChange(STATE_STARTED);
                    } else {
                        Log.e("leepood", "create transport error!");
                    }

                } catch (Exception ex) {
                    // create not record
                    ex.printStackTrace();
                    Log.e("leepood", "create transport error!");
                }
            }
        }
        return START_STICKY;
    }


    private void notifyStatusChange(int status){
        Intent mIntent = new Intent(Common.ACTION_STATUS_CHANGED);
        mIntent.putExtra("status",status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
    }



    private void createRecordSession(int resultCode, Intent data) throws Exception {


        MediaFormat mMediaFormat = MediaFormat.createVideoFormat("video/avc", recordInfo.getVideoWidth(),
                recordInfo.getVideoHeight());


        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate * 1024);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);

        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrame);

        encoder = MediaCodec.createEncoderByType("video/avc");

        encoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = encoder.createInputSurface();

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = projectionManager.getMediaProjection(resultCode, data);
        display = projection.createVirtualDisplay(DISPLAY_NAME, recordInfo.getVideoWidth(),
                recordInfo.getVideoHeight(),
                recordInfo.getDpi(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface,
                null,
                null);
        encoderThread = new EncoderThread(encoder);
        encoderThread.setEncoderListener(this);
        encoder.start();
        encoderThread.start();
    }



    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    @Override
    public void onEncoderStart() {
    }

    @Override
    public void onEncoderStop() {
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEncodeAFrame(byte[] frame) {
        mLarkerNative.feedH264Data(frame);
    }
}
