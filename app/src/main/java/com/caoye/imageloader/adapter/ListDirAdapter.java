package com.caoye.imageloader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.caoye.imageloader.R;
import com.caoye.imageloader.bean.ImageFolder;
import com.caoye.imageloader.util.CustomImageLoader;

import java.util.List;

/**
 * Created by admin on 8/2/16.
 */
public class ListDirAdapter extends ArrayAdapter<ImageFolder> {
    private LayoutInflater mInflater;

    public ListDirAdapter(Context context, List<ImageFolder> objects) {
        super(context, 0, objects);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_dir_item, parent, false);
            viewHolder.mImgView = (ImageView) convertView.findViewById(R.id.id_dir_item_image);
            viewHolder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
            viewHolder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ImageFolder folder = getItem(position);


        // Reset
        viewHolder.mImgView.setImageResource(R.drawable.picture_no);

        /**
         * CustomImageLoader to load images
         */
        CustomImageLoader.getInstance().loadImage(folder.getFirstImagePath(), viewHolder.mImgView);
        viewHolder.mDirCount.setText(folder.getCount() + "");
        viewHolder.mDirName.setText(folder.getName());

        return convertView;
    }

    private class ViewHolder {
        ImageView mImgView;
        TextView mDirName;
        TextView mDirCount;
    }
}


