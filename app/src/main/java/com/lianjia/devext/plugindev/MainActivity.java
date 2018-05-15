package com.lianjia.devext.plugindev;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ImageView mIvBg;
    private List<Map<String, String>> mPluginList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        mIvBg = findViewById(R.id.iv_bg);
        findViewById(R.id.bt_plugin).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_plugin:
                disposeBtnClick(v);
                break;
        }
    }

    private void disposeBtnClick(View view) {
        View contentView = getLayoutInflater().inflate(R.layout.popwindow_layout, null);
        PopupWindow popupWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
        // 点击提示框以外部分提示框会消失
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);

        // 查找插件
        mPluginList = findPluginList();
        if (mPluginList == null || mPluginList.size() == 0) {
            Toast.makeText(this, "目前没有皮肤, 请下载皮肤", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示皮肤
        ListView lvPlugins = contentView.findViewById(R.id.lv_plugin);
        lvPlugins.setOnItemClickListener(this);
        SimpleAdapter adapter = new SimpleAdapter(this, mPluginList, android.R.layout.simple_list_item_1,
                new String[]{"label"}, new int[]{android.R.id.text1});
        lvPlugins.setAdapter(adapter);

        popupWindow.setWidth(150 * 2);
        popupWindow.setHeight(mPluginList.size() * 80);

        popupWindow.showAsDropDown(view);
    }

    // 查找插件列表
    private List<Map<String, String>> findPluginList() {
        ArrayList<Map<String, String>> list = new ArrayList<>();
        // 获取包管理器
        PackageManager packageManager = getPackageManager();
        // 获取手机已安装的 app 的包信息
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        try {
            // 获取当前 app 的包信息
            PackageInfo curPackageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            for (PackageInfo packageInfo : packages) {
                String packageName = packageInfo.packageName;
                String sharedUserId = packageInfo.sharedUserId;
                if (sharedUserId == null || !TextUtils.equals(sharedUserId, curPackageInfo.sharedUserId)
                        || TextUtils.equals(packageName, getPackageName())) {
                    continue;
                }
                HashMap<String, String> pluginMap = new HashMap<>();
                // 获取插件程序的名称
                String label = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                pluginMap.put("packageName", packageName);
                pluginMap.put("label", label);
                // 添加插件信息
                list.add(pluginMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // 加载插件资源(
        // 1、获取插件上下文(获取插件程序的资源加载器)
        // 获取当前要加载的插件
        Map<String, String> map = mPluginList.get(position);
        // 获取插件上下文
        Context pluginContext = findPluginContext(map);
        // 2、根据插件资源加载器, 加载图片资源)
        int resId = findResourceId(pluginContext, map);

        if (resId != 0) {
            // 直接设置是不能加载到资源的
            // 解决办法：必须使用插件上下文来帮助加载资源
            Drawable drawable = pluginContext.getResources().getDrawable(resId);
            mIvBg.setImageDrawable(drawable);
        }
    }

    /**
     * 根据插件资源加载器, 加载图片资源
     */
    private int findResourceId(Context pluginContext, Map<String, String> map) {
        if (pluginContext == null) {
            return 0;
        }
        String packageName = map.get("packageName");


        try {
            PackageManager pm = getPackageManager();
            Resources res = pm.getResourcesForApplication(packageName);
            int resId = res.getIdentifier("icon_main_bg","mipmap", packageName); // 根据名字取id
            return resId;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        try {
//            PathClassLoader classLoader = new PathClassLoader(pluginContext.getPackageResourcePath(),
//                    PathClassLoader.getSystemClassLoader());
//            // 获取资源类
//            Class<?> forName = Class.forName(packageName + ".R$mipmap", true, classLoader);
//            Field[] fields = forName.getFields();
//            for (Field field :fields) {
//                // 找到想要的资源 (所有的插件的背景图片的名称: icon_main_bg) --- 协议: main_bg
//                String name = field.getName();
//                if (name.equals("icon_main_bg")) {
//                    return field.getInt(R.mipmap.class);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return 0;
    }

    private Context findPluginContext(Map<String, String> map) {
        try {
            return createPackageContext(map.get("packageName"), Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
