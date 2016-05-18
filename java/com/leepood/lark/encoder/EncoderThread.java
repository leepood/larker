package com.leepood.lark.encoder;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by leepood on 6/19/15.
 * A Thread which encoder video with h264 format
 */
public class EncoderThread extends Thread {

    public interface EncoderListener {

        void onEncoderStart();

        void onEncoderStop();

        void onEncodeAFrame(byte[] frame);
    }

    private boolean finished = true;
    private MediaCodec mediaCodec = null;
    private EncoderListener mListener = null;
    public static final int TIMEOUT = 60 * 1000;

    public EncoderThread(MediaCodec codec) {
        mediaCodec = codec;
    }

    public void setEncoderListener(EncoderListener listener) {
        this.mListener = listener;
    }


    @Override
    public void run() {
        finished = false;
        if (mListener != null) mListener.onEncoderStart();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();

        while (!finished) {
            int encoderStatus;
            try {
                encoderStatus = mediaCodec.dequeueOutputBuffer(info, TIMEOUT);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                break;
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                //Log.d(TAG, "no output from encoder available");
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
//                MediaFormat newFormat = mediaCodec.getOutputFormat();
            } else if (encoderStatus < 0) {
                break;
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (info.size != 0) {
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
                }
                if (null != mListener) {
                    byte[] tempBuffer = new byte[info.size];
                    encodedData.get(tempBuffer, 0, info.size);
                    mListener.onEncodeAFrame(tempBuffer);
                }
                finished = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                try {
                    mediaCodec.releaseOutputBuffer(encoderStatus, false);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mListener != null) mListener.onEncoderStop();
    }


}
