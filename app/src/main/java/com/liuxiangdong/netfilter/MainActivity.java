package com.liuxiangdong.netfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private boolean running = false;
    private AppInfoAdapter adapter = null;
    private MenuItem searchItem = null;

    private static final int REQUEST_VPN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        running = true;

        //设置标题栏
        View view = getLayoutInflater().inflate(R.layout.actionbar, null);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(view);

        //标题栏vpn开关
        SwitchCompat swEnabled = (SwitchCompat) view.findViewById(R.id.swEnabled);
        swEnabled.setChecked(prefs.getBoolean("enabled", false));
        swEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent prepare = VpnService.prepare(MainActivity.this);
                    if (prepare == null) {
                        //vpnService准备就绪，开启自定义VpnService
                        onActivityResult(REQUEST_VPN, RESULT_OK, null);
                    } else {
                        try {
                            //开启vpn服务
                            startActivityForResult(prepare, REQUEST_VPN);
                        } catch (Throwable ex) {
                            onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
                        }
                    }
                } else {
                    //网络过滤
                    prefs.edit().putBoolean("enabled", false).apply();
                    NetFilterService.stop(MainActivity.this);
               }
            }
        });

        // 各应用共享参数实时更新
        prefs.registerOnSharedPreferenceChangeListener(this);


        fillApplicationList();

        IntentFilter ifConnectivity = new IntentFilter();
        ifConnectivity.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangedReceiver, ifConnectivity);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(packageChangedReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        running = false;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(connectivityChangedReceiver);
        unregisterReceiver(packageChangedReceiver);
        super.onDestroy();
    }
    //vpn链接状态监听广播
    private BroadcastReceiver connectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            invalidateOptionsMenu();
        }
    };


    /**
     * 应用网络过滤状态监听广播
     */
    private BroadcastReceiver packageChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fillApplicationList();
        }
    };

    /**
     * 获取应用并展示
     */
    private void fillApplicationList() {
        final RecyclerView rvApplication = (RecyclerView) findViewById(R.id.rvApplication);
        rvApplication.setHasFixedSize(true);
        rvApplication.setLayoutManager(new LinearLayoutManager(this));

        //异步获取应用列表
        new AsyncTask<Object, Object, List<AppInfo>>() {
            @Override
            protected List<AppInfo> doInBackground(Object... arg) {
                return AppInfo.getAppInfos(MainActivity.this);
            }

            @Override
            protected void onPostExecute(List<AppInfo> result) {
                if (running) {
                    if (searchItem != null)
                        MenuItemCompat.collapseActionView(searchItem);
                    adapter = new AppInfoAdapter(result, MainActivity.this);
                    rvApplication.setAdapter(adapter);
                }
            }
        }.execute();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String name) {
        if ("enabled".equals(name)) {
            // Get enabled
            boolean enabled = prefs.getBoolean(name, false);

            // Check switch state
            SwitchCompat swEnabled = (SwitchCompat) getSupportActionBar().getCustomView().findViewById(R.id.swEnabled);
            if (swEnabled.isChecked() != enabled)
                swEnabled.setChecked(enabled);
        }
    }

  
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VPN) {
            // 更新vpn服务开关
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", resultCode == RESULT_OK).apply();

            // 开启vpnService
            if (resultCode == RESULT_OK)
                NetFilterService.start(this);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
