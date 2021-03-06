package me.li2.android.li2launcher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Li2LauncherFragment extends ListFragment {

    private final static String TAG = "Li2LauncherFragment";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent startupIntent = new Intent(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                
        // 前面的CriminalIntent应用中，为使用隐式intent触发activity选择器来发送crime报告，实现方式是：
        // 创建一个隐式intent并将其封装在一个选择器intent中，然后调用startActivity(Intent)方法。
        // 而操作系统会悄然地将Intent.CATEGORY_DEFAULT类别添加给目标intent，
        // 但MAIN/LAUNCHER intent过滤器并不一定包含CATEGORY_DEFAULT，
        // 所以并不一定能启动与MAIN/LAUNCHER intent相匹配的activity.
        // 通过下面两行代码测试，弹出的app列表确实不同：
        // startupIntent = Intent.createChooser(startupIntent, "launcher");
        // startActivity(startupIntent);
        
        PackageManager pm = getActivity().getPackageManager();
        // 采用另一种方法：向PackageManager查询具有MAIN/LAUNCHER intent过滤器的activity.
        // ResolveInfo对象可以获取activity标签和其它一些元数据
        List<ResolveInfo> activities = pm.queryIntentActivities(startupIntent, 0);
        
        Log.d(TAG, "I've found " + activities.size() + " activities.");
        
        // 按字母顺序排序activity
        Collections.sort(activities, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                PackageManager pm = getActivity().getPackageManager();
                return String.CASE_INSENSITIVE_ORDER.compare(
                        lhs.loadLabel(pm).toString(),
                        rhs.loadLabel(pm).toString());
            }
            
        });
        
        ArrayAdapter<ResolveInfo> adapter = new ArrayAdapter<ResolveInfo>(
                getActivity(), android.R.layout.simple_list_item_1, activities) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        // Documentation says that simple_list_item_1 is a TextView. 所以没有必要用下面的方式。
                        // if (convertView == null) {
                        //     convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
                        // }
                        TextView textView = (TextView)view;
                        
                        PackageManager pm = getActivity().getPackageManager();
                        textView.setText(getItem(position).loadLabel(pm));
                        return view;
                    }
        };
        
        setListAdapter(adapter);
    }

    @Override
    // 点击列表项时，创建显示intent，以启动对应的activity.
    public void onListItemClick(ListView l, View v, int position, long id) {
        ResolveInfo resolveInfo = (ResolveInfo)l.getAdapter().getItem(position);
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo == null) {
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        // 使用包名和类名创建一个显式intent： setClassName(String packageName, String className)
        // 不同于之前的方式：Intent(Context packageContext, Class<?> cls)
        intent.setClassName(activityInfo.applicationInfo.packageName, activityInfo.name);
        // Android都使用task来跟踪用户的状态。task是用户比较关心的 a stack of activities.
        // 默认情况下，新activity都在当前task中启动，用户就可以在任务内而不是在应用层级间导航返回；
        // 而添加如下flag，可在新任务中启动activity，用户就可以在运行的应用间自由切换。
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }    
}
