package com.hello.test;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mdmx.opensdk.OXWService;
import com.mdmx.opensdk.OpenX;
import com.mdmx.opensdk.constant.Path;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    protected OpenX mOpenX;

    protected TextView textStatus;
    protected TextView btnWifi;
    protected TextView btnBluetooth;
    protected TextView btnBackup;
    protected TextView btnBackupSystemInfoWithRename;

    protected String MM_PKG = "com.tencent.mm";
    protected String sourceFile = MM_PKG + "/MicroMsg/systemInfo.cfg";
    protected String backupFolder = "/storage/emulated/0/AAAOSTest";

    //对外服务
    protected OXWService oxwService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = findViewById(R.id.textStatus);
        btnWifi = findViewById(R.id.btnWifi);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnBackup = findViewById(R.id.btnBackup);

        mOpenX = OpenX.get();
        mOpenX.getService(this, new OpenX.Callback() {
            @Override
            public void onService(OXWService oxService) {
                oxwService = oxService;

                textStatus.setText("已连接");
            }
        });

        //backup data receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MDMXActions.mdmx_backup_success);
        intentFilter.addAction(MDMXActions.mdmx_backup_failed);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, String.format("backup receiver action=%s,\nsrc=%s, \ndest=%s",
                        intent.getAction(),
                        intent.getStringExtra(MDMXActions.backup_src),
                        intent.getStringExtra(MDMXActions.backup_dest)));
            }
        }, intentFilter);

        //通话广播
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(MDMXActions.ACTION_INCOMING_CALL);
        intentFilter2.addAction(MDMXActions.ACTION_OUTGOING_CALL);
        intentFilter2.addAction(MDMXActions.ACTION_CALL_CONNECTED);
        intentFilter2.addAction(MDMXActions.ACTION_CALL_DISCONNECTED);
        intentFilter2.addAction(MDMXActions.ACTION_CALL_REJECT);
        intentFilter2.addAction(MDMXActions.ACTION_CALL_RECORD);

        intentFilter2.addAction(MDMXActions.ACTION_VOIP_RECORD);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(MDMXActions.ACTION_CALL_RECORD)) {
                    Log.d(TAG, String.format("call record receiver action=%s,\nid=%s, \npath=%s",
                            intent.getAction(),
                            intent.getLongExtra(MDMXActions.ID, -1),
                            intent.getStringExtra(MDMXActions.PATH)));
                } else if(intent.getAction().equals(MDMXActions.ACTION_VOIP_RECORD)) {
                    Log.d(TAG, String.format("voip record receiver action=%s,\npath=%s",
                            intent.getAction(),
                            intent.getStringExtra(MDMXActions.PATH)));
                } else {
                    Log.d(TAG, String.format("call receiver action=%s,\nslotId=%s, \nphoneNumber=%s",
                            intent.getAction(),
                            intent.getIntExtra(MDMXActions.EXTRA_SLOT_ID, -1),
                            intent.getStringExtra(MDMXActions.EXTRA_PHONE_NUMBER)));
                }
            }
        }, intentFilter2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean aBoolean) throws Exception {
                        if (!aBoolean) return;
                        File destFolder = new File(backupFolder);
                        if (!destFolder.exists()) {
                            destFolder.mkdir();
                        }
                        Log("backupFolder:%s", destFolder.exists());
                    }
                });
    }

    protected void Log(String f, Object... os) {
        Log.d("OPENX-fromClient", String.format(f, os));
    }

    public void setVoipRecordPath(View view) {
        oxwService.setVoipRecordPath("/sdcard/abctest");
    }

    public void onBackup(View view) {
        Log("onBackup");
        //src 输入相对路径，根据选择的Path进行自动补全
        // /data/data/
        oxwService.backupData(Path.INTERNAL, sourceFile, backupFolder);

        // !!! 备份目的地文件路径需提前创建
        File androidFolder = new File(String.format("%s/android_cache/", backupFolder));
        if (!androidFolder.exists()) {
            androidFolder.mkdirs();
        }
        // /storage/emulated/0/Android/data/com.tencent.mm
        oxwService.backupData(Path.ANDROID, MM_PKG + "/cache/", androidFolder.getAbsolutePath());

        File sdFolder = new File(String.format("%s/sdcard_tencent/", backupFolder));
        if (!sdFolder.exists()) {
            sdFolder.mkdirs();
        }
        // /storage/emulated/0/tencent
        oxwService.backupData(Path.EXTERNAL, "tencent/", sdFolder.getAbsolutePath());

        // ---------------------分身------------------
        File cloneFolder = new File(String.format("%s/WeChatClone/", backupFolder));
        if (!cloneFolder.exists()) {
            cloneFolder.mkdirs();
        }
        // 微信分身/com.tencent.mm
        oxwService.backupData(Path.CLONER_INTERNAL, sourceFile, cloneFolder.getAbsolutePath());

        File cloneAndroidFolder = new File(String.format("%s/WeChatClone/android_cache/", backupFolder));
        if (!cloneAndroidFolder.exists()) {
            cloneAndroidFolder.mkdirs();
        }
        // 微信分身SDCard/Android/data/com.tencent.mm
        oxwService.backupData(Path.CLONER_ANDROID, MM_PKG + "/cache/", cloneAndroidFolder.getAbsolutePath());

        File cloneSdFolder = new File(String.format("%s/WeChatClone/sdcard_tencent/", backupFolder));
        if (!cloneSdFolder.exists()) {
            cloneSdFolder.mkdirs();
        }
        // 微信分身SDCard/tencent
        oxwService.backupData(Path.CLONER_EXTERNAL, "tencent/", cloneSdFolder.getAbsolutePath());
    }

    public void onWifi(View view) {
        btnWifi.setSelected(!btnWifi.isSelected());
        btnWifi.setText(btnWifi.isSelected() ? "WiFi禁用" : "WiFi正常");
        oxwService.setWiFiDisable(btnWifi.isSelected());
    }

    public void onBluetooth(View view) {
        btnBluetooth.setSelected(!btnBluetooth.isSelected());
        btnBluetooth.setText(btnBluetooth.isSelected() ? "蓝牙禁用" : "蓝牙正常");
        oxwService.setBluetoothDisable(btnBluetooth.isSelected());
    }

    public void onIMEI(View view) {
        String IMEI1 = oxwService.imei(0);
        String IMEI2 = oxwService.imei(1);
        String MEID = oxwService.meid();

        textStatus.setText(String.format("IMEI1:%s\nIMEI2:%s\nMEID:%s",IMEI1,IMEI2,MEID));
    }

    public void onMobileNumber(View view) {
        String num1 = oxwService.getMobileNumber(0);
        String num2 = oxwService.getMobileNumber(1);

        textStatus.setText(String.format("NUM1:%s\nNUM2:%s",num1,num2));
    }

}