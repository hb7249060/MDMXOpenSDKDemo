package com.hello.test;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mdmx.opensdk.OXWService;
import com.mdmx.opensdk.OpenX;
import com.mdmx.opensdk.constant.Path;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    protected OpenX mOpenX;

    protected TextView textStatus;
    protected Button btnWifi;
    protected Button btnBluetooth;
    protected Button btnBackup;
    protected Button btnSetTimeAndDateSet, btnSetExternalStorage, btnSetNetTether, btnSetWifiAp;
    protected Button btnSetVpn, btnSetBluetoothPolicy, btnSetWifiPolicy;
    protected Button btnSetSuperWhiteList, btnSetAppPermission, btnOpenNotification;
    protected Button btnSetForwardCallSetting, btnClearApplicationData;
    protected Button btnEnableAccessibilityService, btnDisableAccessibilityService;

    protected String MM_PKG = "com.tencent.mm";
    protected String sourceFile = MM_PKG + "/MicroMsg/systemInfo.cfg";
    protected String backupFolder = "/storage/emulated/0/AAAOSTest";

    //对外服务
    protected OXWService oxwService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewInject();

        mOpenX = OpenX.get();
        mOpenX.getService(this, new OpenX.Callback() {
            @Override
            public void onService(OXWService oxService) {
                oxwService = oxService;
                if(oxwService.oxService != null) {
                    textStatus.setText("MDM服务已连接");
                } else {
                    textStatus.setText("MDM服务已断开");
                }
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
                if (intent.getAction().equals(MDMXActions.ACTION_CALL_RECORD)) {
                    Log.d(TAG, String.format("call record receiver action=%s,\nid=%s, \npath=%s",
                            intent.getAction(),
                            intent.getLongExtra(MDMXActions.ID, -1),
                            intent.getStringExtra(MDMXActions.PATH)));
                } else if (intent.getAction().equals(MDMXActions.ACTION_VOIP_RECORD)) {
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

        textStatus.setText(String.format("IMEI1:%s\nIMEI2:%s\nMEID:%s", IMEI1, IMEI2, MEID));
    }

    public void onMobileNumber(View view) {
        String num1 = oxwService.getMobileNumber(0);
        String num2 = oxwService.getMobileNumber(1);

        textStatus.setText(String.format("NUM1:%s\nNUM2:%s", num1, num2));
    }

    public void onSDKVersion(View view) {
        Toast.makeText(this, oxwService.getSDKVersion(), Toast.LENGTH_SHORT).show();
    }

    public void onAddDisallowedRunningApp(View view) {
        //添加禁止运行应用
        Toast.makeText(this, "禁止运行微信", Toast.LENGTH_SHORT).show();
        oxwService.addDisallowedRunningApp(new String[]{MM_PKG});
    }

    public void onClearDisallowedRunningApp(View view) {
        //清除禁止运行应用
        oxwService.clearDisallowedRunningApp();
    }

    public void onSetContactPolicy(View view) {
        //设置通讯录策略 0：普通模式，黑白名单均不生效模式
        //1：黑名单模式 2：白名单模式
        oxwService.setContactPolicy(1);
    }

    public void onAddContactPolicyNumber(View view) {
        //添加黑名单号码
        oxwService.addContactPolicyNumber(1, new String[]{"10000"});
    }

    public void onClearContactPolicyNumber(View view) {
        //清除通讯录策略下的名单
        oxwService.clearContactPolicyNumber(1);
    }

    public void onSetTimeAndDateSet(View view) {
        btnSetTimeAndDateSet.setSelected(!btnSetTimeAndDateSet.isSelected());
        String text = btnSetTimeAndDateSet.getText().toString();
        btnSetTimeAndDateSet.setText(btnSetTimeAndDateSet.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setTimeAndDateSetDisabled(btnSetTimeAndDateSet.isSelected());
    }

    public void onSetExternalStorage(View view) {
        btnSetExternalStorage.setSelected(!btnSetExternalStorage.isSelected());
        String text = btnSetExternalStorage.getText().toString();
        btnSetExternalStorage.setText(btnSetExternalStorage.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setExternalStorageDisabled(btnSetExternalStorage.isSelected());
    }

    public void onSetNetTether(View view) {
        btnSetNetTether.setSelected(!btnSetNetTether.isSelected());
        String text = btnSetNetTether.getText().toString();
        btnSetNetTether.setText(btnSetNetTether.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setNetTetherDisabled(btnSetNetTether.isSelected());
    }

    public void onSetWifiAp(View view) {
        //设置WiFi个人热点管控策略，可以设置如下几种状态：
        //0： 不管控；
        //1： 禁用且用户无法手动开启；
        //2： 开启且用户无法手动关闭；
        //3： 关闭且用户可以手动开启；
        //4： 打开且用户可以手动关闭。
        //注：对于状态2，当用户手机重启后，个人热点将切换成关闭状态。此时用户可以手动开启个人热点，且开启后无法手动关闭。
        btnSetWifiAp.setSelected(!btnSetWifiAp.isSelected());
        String text = btnSetWifiAp.getText().toString();
        btnSetWifiAp.setText(btnSetWifiAp.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setWifiApPolicy(btnSetWifiAp.isSelected() ? 1 : 0);
    }

    public void onSetVpn(View view) {
        btnSetVpn.setSelected(!btnSetVpn.isSelected());
        String text = btnSetVpn.getText().toString();
        btnSetVpn.setText(btnSetVpn.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setVpnDisabled(btnSetVpn.isSelected());
    }

    public void onSetBluetoothPolicy(View view) {
        //0：为强关模式；关闭蓝牙，用户无法打开蓝牙； 1：保留模式（无实际功能）； 2：正常模式；用户可以自己打开关闭蓝牙； 3：强开模式；用户无法手动关闭蓝牙；
        //4：关闭蓝牙，用户可以手动打开； 5：打开蓝牙，用户可以手动关闭。
        btnSetBluetoothPolicy.setSelected(!btnSetBluetoothPolicy.isSelected());
        String text = btnSetBluetoothPolicy.getText().toString();
        btnSetBluetoothPolicy.setText(btnSetBluetoothPolicy.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setBluetoothPolicy(btnSetBluetoothPolicy.isSelected() ? 0 : 2);
    }

    public void onSetWifiPolicy(View view) {
        //0:底层禁用WLAN，界面上置灰无法手动操作； 1:WLAN关闭且保持一直扫描，但界面上置灰用户无法手动操作； 2:仅界面上取消置灰，不修改WLAN当前状态，用户可以手动打开或关闭； 3:关闭WLAN，用户可以手动打开。 4:打开WLAN，用户可以手动关闭。 5:强制打开WLAN，用户无法手动关闭。
        btnSetWifiPolicy.setSelected(!btnSetWifiPolicy.isSelected());
        String text = btnSetWifiPolicy.getText().toString();
        btnSetWifiPolicy.setText(btnSetWifiPolicy.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setWifiPolicy(btnSetWifiPolicy.isSelected() ? 0 : 2);
    }

    public void onSetSuperWhiteList(View view) {
        //设置可信任应用列表
        //设置可信任应用列表，列表中的应用静默授予如下权限，用户无法手动修改该应用的权限：1，悬浮窗；2，应用通知；3，移动数据网络；4，WLAN无线网络；5，忽略耗电优化。
        oxwService.setSuperWhiteList(new String[]{BuildConfig.APPLICATION_ID});
    }

    public void onSetAppPermission(View view) throws JSONException {
        //设置应用权限
        //permission：权限类型，详见表格“权限类型定义”，不区分大小写。 mode：权限授权类型，详见表格“权限授权类型定义”，不区分大小写。 fixed：定义该权限是否允许用户手动修改，详见表格“是否允许修改定义”。
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("permission", "CAMERA");
        jsonObject.put("mode", "ALLOWED");
        jsonObject.put("fixed", "true");

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(jsonObject);
        oxwService.setAppPermission(BuildConfig.APPLICATION_ID, jsonArray.toString());
    }

    public void onOpenNotification(View view) {
        //开始通知
        btnOpenNotification.setSelected(!btnOpenNotification.isSelected());
        String text = btnOpenNotification.getText().toString();
        btnOpenNotification.setText(!btnOpenNotification.isSelected() ?
                text.endsWith("开启") ? text.replace("开启", "关闭") : text + "关闭" :
                text.endsWith("关闭") ? text.replace("关闭", "开启") : text + "开启");
        oxwService.openNotification(BuildConfig.APPLICATION_ID, btnOpenNotification.isSelected());
    }

    public void onSetForwardCallSetting(View view) {
        //呼叫转移管控
        btnSetForwardCallSetting.setSelected(!btnSetForwardCallSetting.isSelected());
        String text = btnSetForwardCallSetting.getText().toString();
        btnSetForwardCallSetting.setText(btnSetForwardCallSetting.isSelected() ?
                text.endsWith("正常") ? text.replace("正常", "禁用") : text + "禁用" :
                text.endsWith("禁用") ? text.replace("禁用", "正常") : text + "正常");
        oxwService.setForwardCallSettingDisabled(btnSetForwardCallSetting.isSelected());
    }

    public void onClearApplicationData(View view) {
        //清除应用数据，注意，清除自身会杀死应用自身进程
        oxwService.clearApplicationData(BuildConfig.APPLICATION_ID);
    }

    public void onEnableAccessibilityService(View view) {
        //启用辅助
        oxwService.enableAccessibility(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + ".MyAccessibilityService", true);
    }

    public void onDisableAccessibilityService(View view) {
        //禁用辅助
        oxwService.enableAccessibility(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + ".MyAccessibilityService", false);
    }


    private void viewInject() {
        textStatus = findViewById(R.id.textStatus);
        btnWifi = findViewById(R.id.btnWifi);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        btnBackup = findViewById(R.id.btnBackup);

        btnSetTimeAndDateSet = findViewById(R.id.btnSetTimeAndDateSet);
        btnSetExternalStorage = findViewById(R.id.btnSetExternalStorage);
        btnSetNetTether = findViewById(R.id.btnSetNetTether);
        btnSetWifiAp = findViewById(R.id.btnSetWifiAp);

        btnSetVpn = findViewById(R.id.btnSetVpn);
        btnSetBluetoothPolicy = findViewById(R.id.btnSetBluetoothPolicy);
        btnSetWifiPolicy = findViewById(R.id.btnSetWifiPolicy);

        btnSetSuperWhiteList = findViewById(R.id.btnSetSuperWhiteList);
        btnSetAppPermission = findViewById(R.id.btnSetAppPermission);
        btnOpenNotification = findViewById(R.id.btnOpenNotification);

        btnSetForwardCallSetting = findViewById(R.id.btnSetForwardCallSetting);
        btnClearApplicationData = findViewById(R.id.btnClearApplicationData);

        btnEnableAccessibilityService = findViewById(R.id.btnEnableAccessibilityService);
        btnDisableAccessibilityService = findViewById(R.id.btnDisableAccessibilityService);
    }
}