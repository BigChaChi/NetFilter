package com.liuxiangdong.netfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppInfo implements Comparable<AppInfo> {
    public PackageInfo info;            //应用包详细信息
    public String name;                 //应用名称
    public boolean system;              //是否为系统应用
    public boolean disabled;            //是否禁用
    public boolean wifi_blocked;        //Wi-Fi网络过滤
    public boolean other_blocked;       //2g，3g，4g网络过滤
    public boolean changed;             //过滤状态变化

    private AppInfo(PackageInfo info, boolean wifi_blocked, boolean other_blocked, boolean changed, Context context) {
        PackageManager pm = context.getPackageManager();
        this.info = info;
        this.name = info.applicationInfo.loadLabel(pm).toString();
        this.system = ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

        int setting = pm.getApplicationEnabledSetting(info.packageName);
        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            this.disabled = !info.applicationInfo.enabled;
        else
            this.disabled = (setting != PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        this.wifi_blocked = wifi_blocked;
        this.other_blocked = other_blocked;
        this.changed = changed;
    }

    public static List<AppInfo> getAppInfos(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences wifi = context.getSharedPreferences("wifi", Context.MODE_PRIVATE);
        SharedPreferences other = context.getSharedPreferences("other", Context.MODE_PRIVATE);

        boolean wlWifi = prefs.getBoolean("whitelist_wifi", true);      //（白名单）wifi锁，true表示锁定，默认为true
        boolean wlOther = prefs.getBoolean("whitelist_other", true);    //（白名单）数据锁，true表示锁定，默认为true

        List<AppInfo> appInfos = new ArrayList<>();
        for (PackageInfo info : context.getPackageManager().getInstalledPackages(0)) {
            boolean blWifi = wifi.getBoolean(info.packageName, wlWifi);
            boolean blOther = other.getBoolean(info.packageName, wlOther);
            boolean changed = (blWifi != wlWifi || blOther != wlOther);
            appInfos.add(new AppInfo(info, blWifi, blOther, changed, context));
        }

        Collections.sort(appInfos);

        return appInfos;
    }

    /**
     * 获取指定应用图标
     * @param context
     * @return
     */
    public Drawable getIcon(Context context) {
        return info.applicationInfo.loadIcon(context.getPackageManager());
    }

    @Override
    public int compareTo(AppInfo other) {
        if (changed == other.changed) {
            int i = name.compareToIgnoreCase(other.name);
            return (i == 0 ? info.packageName.compareTo(other.info.packageName) : i);
        }
        return (changed ? -1 : 1);
    }
}
