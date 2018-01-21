package com.liuxiangdong.netfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

public class Receiver extends BroadcastReceiver {
    /**
     *
     *开机启动vpnService
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (VpnService.prepare(context) == null)
            NetFilterService.start(context);
    }
}