package com.tulingdemo;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

//listview的adapter类
public class TextAdapter extends BaseAdapter {

    private List<ListData> lists;
    private Context mContext;
    private RelativeLayout layout;

    public TextAdapter(List<ListData> lists, Context mContext) {
        this.lists = lists;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int position) {
        return lists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //获取或创建viewholder
        ViewHolder holder=null;
        if (convertView==null){//第一次肯定为RECEIVER
            holder=new ViewHolder();
            convertView=View.inflate(mContext,R.layout.leftitem,null);
            holder.content= (TextView) convertView.findViewById(R.id.tv);

            convertView.setTag(holder);
        }else {
            holder= (ViewHolder) convertView.getTag();
            if (lists.get(position).getFlag() == ListData.SEND){
                convertView=View.inflate(mContext,R.layout.rightitem,null);
                holder.content= (TextView) convertView.findViewById(R.id.tv);

                convertView.setTag(holder);
            }else {
                convertView=View.inflate(mContext,R.layout.leftitem,null);
                holder.content= (TextView) convertView.findViewById(R.id.tv);

                convertView.setTag(holder);
            }
        }

        //获取当前item数据
        ListData listData=lists.get(position);

        //显示数据
        if (listData.getFlag()==ListData.RECEIVER){
            holder.content.setText(listData.getContent());
        }else {
            holder.content.setText("\""+lists.get(position).getContent()+"\"");//如果是发送的信息，加上双引号
        }

        //返回view
        return convertView;
    }

    private class ViewHolder{
        private TextView content;
    }


}
