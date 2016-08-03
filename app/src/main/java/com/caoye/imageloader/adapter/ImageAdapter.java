package com.caoye.imageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.caoye.imageloader.R;
import com.caoye.imageloader.util.CustomImageLoader;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by admin on 8/1/16.
 */
public class ImageAdapter extends BaseAdapter {
    private static Set<String> selectedImgSet = new HashSet<>();

    private List<String> imgList = null;
    private String parentPath = null;
    private LayoutInflater mInflater;

    private int mScreenWidth;

    public ImageAdapter(Context context, List<String> data, String dirPath) {
        this.imgList = data;
        this.parentPath = dirPath;
        mInflater = LayoutInflater.from(context);

        WindowManager wm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
    }
    @Override
    public int getCount() {
        return imgList.size();
    }

    @Override
    public Object getItem(int i) {
        return imgList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.grid_item, parent, false);
            viewHolder = new ViewHolder();
            convertView.setTag(viewHolder);
            viewHolder.mImgView = (ImageView) convertView.findViewById(R.id.id_item_image);
            viewHolder.mSelectBtn = (ImageButton) convertView.findViewById(R.id.id_item_select);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Reset viewHolder state
        viewHolder.mImgView.setImageResource(R.drawable.picture_no);
        viewHolder.mSelectBtn.setImageResource(R.drawable.picture_unselected);
        viewHolder.mImgView.setColorFilter(null);
        viewHolder.mImgView.setMaxWidth(mScreenWidth / 3);

        CustomImageLoader.getInstance(3, CustomImageLoader.Type.LIFO).
                loadImage(parentPath + File.separator + imgList.get(i), viewHolder.mImgView);

        final String filePath = parentPath + File.separator + imgList.get(i);
        viewHolder.mImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImgSet.contains(filePath)) {
                    // Deselect
                    selectedImgSet.remove(filePath);
                    viewHolder.mImgView.setColorFilter(null);
                    viewHolder.mSelectBtn.setImageResource(R.drawable.picture_unselected);
                } else {
                    // Select
                    selectedImgSet.add(filePath);
                    viewHolder.mImgView.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.mSelectBtn.setImageResource(R.drawable.picture_selected);
                }
            }
        });

        if (selectedImgSet.contains(filePath)) {
            viewHolder.mImgView.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.mSelectBtn.setImageResource(R.drawable.picture_selected);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView mImgView;
        ImageButton mSelectBtn;
    }
}
