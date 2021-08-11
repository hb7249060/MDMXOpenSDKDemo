package com.hello.test;

public interface MDMXActions {

    String EXTRA_SLOT_ID = "EXTRA_SLOT_ID";
    String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    String ID = "ID";
    String PATH = "PATH";
    //通话广播相关
    String ACTION_INCOMING_CALL = "mdmx.intent.action.ACTION_INCOMING_CALL";
    String ACTION_OUTGOING_CALL = "mdmx.intent.action.ACTION_OUTGOING_CALL";
    String ACTION_CALL_CONNECTED = "mdmx.intent.action.ACTION_CALL_CONNECTED";
    String ACTION_CALL_DISCONNECTED = "mdmx.intent.action.ACTION_CALL_DISCONNECTED";
    String ACTION_CALL_REJECT = "mdmx.intent.action.ACTION_CALL_REJECT";

    //录音相关
    String ACTION_CALL_RECORD = "mdmx.intent.action.ACTION_CALL_RECORD";    //电话录音
    String ACTION_VOIP_RECORD = "mdmx.intent.action.ACTION_VOIP_RECORD";    //voip录音

    //备份广播
    String backup_src = "src";
    String backup_dest = "dest";
    String mdmx_backup_success = "mdmx.action.backup.app.data.success";
    String mdmx_backup_failed = "mdmx.action.backup.app.data.failed";
}
