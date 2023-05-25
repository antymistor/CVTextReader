package com.example.utils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import java.util.List;

/**
 * Created by aizhiqiang on 2023/5/25
 *
 * @author aizhiqiang@bytedance.com
 */
public class NovelNameAdapter extends ArrayAdapter<String> {
    private int resourceId;
    public NovelNameAdapter(@NonNull Context context,
                            int textViewResourceId,
                            List<String> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前项的Fruit实例
        String text = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null){
            // inflate出子项布局，实例化其中的图片控件和文本控件
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);

            viewHolder = new ViewHolder();
            // 通过id得到文本空间实例
            //viewHolder.NovelName = (TextView)view.findViewById(R.layout.text_item);
            // 缓存图片控件和文本控件的实例
            view.setTag(viewHolder);
        }else{
            view = convertView;
            // 取出缓存
            viewHolder = (ViewHolder) view.getTag();
        }
        // 文本控件设置文本内容
        viewHolder.NovelName.setText(text);
        return view;
    }

    // 内部类
    class ViewHolder{
        TextView NovelName;
    }
}
