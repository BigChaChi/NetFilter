package com.liuxiangdong.netfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppInfoAdapter extends RecyclerView.Adapter<AppInfoAdapter.ViewHolder> implements Filterable {


    private Context context;
    private int colorText;      //非系统应用颜色
    private int colorAccent;   //系统应用颜色
    private List<AppInfo> listAll;
    private List<AppInfo> listSelected;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public ImageView ivIcon;
        public TextView tvName;
        public TextView tvPackage;
        public CheckBox cbWifi;
        public CheckBox cbOther;

        public ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvPackage = (TextView) itemView.findViewById(R.id.tvPackage);
            cbWifi = (CheckBox) itemView.findViewById(R.id.cbWifi);
            cbOther = (CheckBox) itemView.findViewById(R.id.cbOther);
        }
    }

    public AppInfoAdapter(List<AppInfo> listappInfo, Context context) {
        this.context = context;
        colorAccent = ContextCompat.getColor(context, R.color.colorAccent);
        TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
        try {
            colorText = ta.getColor(0, 0);
        } finally {
            ta.recycle();
        }
        listAll = listappInfo;
        listSelected = new ArrayList<>();
        listSelected.addAll(listappInfo);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final AppInfo appInfo = listSelected.get(position);

        // 过滤监听
        CompoundButton.OnCheckedChangeListener cbListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String network;
                if (buttonView == holder.cbWifi) {
                    network = "wifi";
                    appInfo.wifi_blocked = isChecked;
                } else {
                    network = "other";
                    appInfo.other_blocked = isChecked;
                }
                //保存当前应用过滤状态
                SharedPreferences prefs = context.getSharedPreferences(network, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(appInfo.info.packageName, isChecked).apply();

                //重启网络过滤服务
                NetFilterService.reload(network, context);
            }
        };

        int color = appInfo.system ? colorAccent : colorText;
        if (appInfo.disabled)
            color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));

        holder.ivIcon.setImageDrawable(appInfo.getIcon(context));
        holder.tvName.setText(appInfo.name);
        holder.tvName.setTextColor(color);
        holder.tvPackage.setText(appInfo.info.packageName);
        holder.tvPackage.setTextColor(color);

        holder.cbWifi.setOnCheckedChangeListener(null);
        holder.cbWifi.setChecked(appInfo.wifi_blocked);
        holder.cbWifi.setOnCheckedChangeListener(cbListener);

        holder.cbOther.setOnCheckedChangeListener(null);
        holder.cbOther.setChecked(appInfo.other_blocked);
        holder.cbOther.setOnCheckedChangeListener(cbListener);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                List<AppInfo> listResult = new ArrayList<>();
                if (query == null)
                    listResult.addAll(listAll);
                else {
                    query = query.toString().toLowerCase();
                    for (AppInfo appInfo : listAll)
                        if (appInfo.name.toLowerCase().contains(query))
                            listResult.add(appInfo);
                }

                FilterResults result = new FilterResults();
                result.values = listResult;
                result.count = listResult.size();
                return result;
            }

            @Override
            protected void publishResults(CharSequence query, FilterResults result) {
                listSelected.clear();
                if (result == null)
                    listSelected.addAll(listAll);
                else
                    for (AppInfo appInfo : (List<AppInfo>) result.values)
                        listSelected.add(appInfo);
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.app_info, parent, false));
    }

    @Override
    public int getItemCount() {
        return listSelected.size();
    }
}
