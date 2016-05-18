package com.leepood.lark;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;


import com.leepood.lark.utils.Common;
import com.leepood.lark.utils.ToastManager;
import com.melnykov.fab.FloatingActionButton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;


public class LarkMainActivity extends Activity {

    private boolean isRunning = false;
    private BroadcastReceiver serverStatusReceiver = null;
    private BroadcastReceiver wifiReceiver = null;

    @InjectView(R.id.fab)
    FloatingActionButton btnStart;

    @InjectView(R.id.txtNotice)
    TextView txtNotice;

    @InjectView(R.id.txtServer)
    TextView txtServer;


    @OnClick(R.id.fab)
    void onBtnStartClick() {
        if (isRunning) {
            stopRecordScreen();
        } else {
            requestRecordScreen();
        }
    }

    @OnLongClick(R.id.txtServer)
    boolean copyAddress2Clipboard() {

        String serverAddress = txtServer.getText().toString().trim();
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        ClipData data = ClipData.newPlainText(serverAddress, serverAddress);

        manager.setPrimaryClip(data);
        vibrator.vibrate(40);
        ToastManager.show(this, getText(R.string.has_copyed_address));
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lark_main);
        ButterKnife.inject(this);
        registerBroadcast();

        displayWifiInfo();
        playAnimation(btnStart);

        if (getActionBar() != null) {
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    private void displayWifiInfo() {
        int port = PreferenceManager.getDefaultSharedPreferences(this).getInt("rtsp_port", 8554);
        String ipAddr = getWifiAddr();
        if (ipAddr == null) {
            txtNotice.setText(getText(R.string.need_connection));
            txtServer.setVisibility(View.INVISIBLE);
        } else {
            txtNotice.setText(getText(R.string.open_notice));
            txtServer.setVisibility(View.VISIBLE);
            String rtsp = String.format("rtsp://%s:%d/larker", ipAddr, port);
            txtServer.setText(rtsp);
        }
    }


    private void playAnimation(View view) {
        int duration = 800;
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("scaleX", 0.0f, 1),
                PropertyValuesHolder.ofFloat("scaleY", 0.0f, 1));
        scaleDown.setInterpolator(new AccelerateInterpolator());
        scaleDown.setDuration(duration);
        scaleDown.start();
    }


    private String getWifiAddr() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        WifiInfo info = wifi.getConnectionInfo();
        if (info != null && wifi.isWifiEnabled() && info.getIpAddress() != 0) {
            int infoIpAddress = info.getIpAddress();

            return String.valueOf(infoIpAddress & 0xFF) + "." +
                    String.valueOf((infoIpAddress >> 8) & 0xFF) + "." +
                    String.valueOf((infoIpAddress >> 16) & 0xFF) + "." +
                    String.valueOf(((infoIpAddress >> 24) & 0xFF));
        }
        return null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lark_main, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_settings);
        if (isRunning) {
            item.setEnabled(false);
        } else {
            item.setEnabled(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        handleRecordScreenRequest(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestRecordScreen() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        startActivityForResult(intent, Common.CREATE_SCREEN_CAPTURE);
    }


    private void stopRecordScreen() {
        Intent mIntent = new Intent(this, RecordScreenService.class);
        mIntent.setAction("stop");
        startService(mIntent);
    }


    private void handleRecordScreenRequest(int requestCode, int resultCode, Intent data) {
        if (requestCode != Common.CREATE_SCREEN_CAPTURE) return;
        if (resultCode != RESULT_OK) return;

        // start background service
        Intent serviceIntent = new Intent(this, RecordScreenService.class);
        serviceIntent.putExtra("resultCode", resultCode);
        serviceIntent.putExtra("data", data);
        startService(serviceIntent);
    }


    private void registerBroadcast() {

        serverStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (null != intent.getAction()) {
                    String action = intent.getAction();
                    if (action.equals(Common.ACTION_STATUS_CHANGED)) {
                        handleServerStatus(intent);
                    }
                }

            }
        };

        IntentFilter mIntentFilter = new IntentFilter(Common.ACTION_STATUS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(serverStatusReceiver, mIntentFilter);


        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (null != intent.getAction()) {
                    String action = intent.getAction();
                    if (action.equals(Common.ACTION_CONNECTION_CHANGED)) {
                        handleWifiStatus();
                    }
                }
            }
        };
        IntentFilter mWifiIntentFilter = new IntentFilter(Common.ACTION_CONNECTION_CHANGED);
        registerReceiver(wifiReceiver,mWifiIntentFilter);
    }


    private void handleWifiStatus() {
        ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            //连接上了
            showConnectionInfo();
        } else {
            //断开了
            showNeedConnection();
        }
    }


    private void handleServerStatus(Intent intent) {
        int status = intent.getIntExtra("status", -1);
        if (status == RecordScreenService.STATE_STARTED) {
            // 开启
            isRunning = true;
            showConnectionInfo();
        } else if (status == RecordScreenService.STATE_STOPED) {
            // 关闭
            isRunning = false;
            showNeedConnection();
        }
    }


    private void showNeedConnection() {
        txtNotice.setText(getText(R.string.need_connection));
        txtServer.setVisibility(View.INVISIBLE);
    }

    private void showConnectionInfo() {
        txtNotice.setText(getText(R.string.open_notice));
        txtServer.setVisibility(View.VISIBLE);
        displayWifiInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverStatusReceiver);
        unregisterReceiver(wifiReceiver);
    }
}
