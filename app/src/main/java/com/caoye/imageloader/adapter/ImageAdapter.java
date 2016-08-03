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

import com.bumptech.glide.Glide;
import com.caoye.imageloader.MainActivity;
import com.caoye.imageloader.R;
import com.caoye.imageloader.util.CustomImageLoader;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.squareup.picasso.Picasso;

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

    private int mScreenWidth, mScreenHeight;

    private Context context;

    public ImageAdapter(Context context, List<String> data, String dirPath) {
        this.imgList = data;
        this.parentPath = dirPath;
        this.context = context;
        mInflater = LayoutInflater.from(context);

        WindowManager wm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;

        initImageLoader(context);
    }

    private void initImageLoader(Context context) {
        //ImageLoaderConfiguration configuration=ImageLoaderConfiguration.createDefault(this);

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .memoryCacheExtraOptions(mScreenWidth / 3, mScreenHeight)
                .threadPoolSize(3)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCacheSize(cacheMemory)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                .build();
        ImageLoader.getInstance().init(config);
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


        /**
         * CustomImageLoader to load images

        final String filePath = parentPath + File.separator + imgList.get(i);
        CustomImageLoader.getInstance(3, CustomImageLoader.Type.LIFO).
                loadImage(filePath, viewHolder.mImgView);
         */


        final String filePath = "file://" + parentPath + File.separator + imgList.get(i);
        /**
         * UIL to load images

        DisplayImageOptions options=new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.picture_no)
                .cacheInMemory(true)
                .build();
        ImageLoader.getInstance().displayImage(filePath,
                viewHolder.mImgView, options);
         */

        /**
         * Picasso to load images
         */
        Picasso.with(context)
                .load(filePath)
                .placeholder(R.drawable.picture_no)
                .error(R.drawable.picture_no)
                .into(viewHolder.mImgView);


        /**
         * Glide to load images

        Glide.with(context)
                .load(filePath)
                .placeholder(R.drawable.picture_no)
                .into(viewHolder.mImgView);
         */


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

    static class ViewHolder {
        ImageView mImgView;
        ImageButton mSelectBtn;
    }
}
