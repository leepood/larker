package com.leepood.lark.utils;

/**
 * Created by leepood on 6/24/15.
 */
public class LarkerNative implements Runnable{

    private long mNativeInstance = 0;

    private Thread mThread = null;
    private volatile boolean isRun = false;

    static{
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("larker");
    }

    public LarkerNative(){
        mThread = new Thread(this);
        mThread.setName("larker-native-thread");
    }

    public native boolean init(int fps,int port);

    public void doLoop(){
        mThread.start();
    }

    private native void loopNative();

    public void stop(){
        stopNative();
    }

    private native void stopNative();

    public native void feedH264Data(byte[] data);

    public native void destory();

    @Override
    public void run() {
        loopNative();
    }
}
