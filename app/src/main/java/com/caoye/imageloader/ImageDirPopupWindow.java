package com.caoye.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import com.caoye.imageloader.adapter.ListDirAdapter;
import com.caoye.imageloader.bean.ImageFolder;
import java.util.List;

/**
 * Created by admin on 8/1/16.
 */
public class ImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View popupWindowView;
    private ListView folderListView;
    private List<ImageFolder> folderList;

    public interface OnDirSelectedListener {
        void onSelected(ImageFolder folder);
    }
    public OnDirSelectedListener mListener;
    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    public ImageDirPopupWindow(Context context, List<ImageFolder> folderList) {
        calWidthAndHeight(context);
        popupWindowView = LayoutInflater.from(context).inflate(R.layout.list_dir, null);
        this.folderList = folderList;

        setContentView(popupWindowView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() ==  MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(context);
        initEvent();
    }

    private void initView(Context context) {
        folderListView = (ListView) popupWindowView.findViewById(R.id.id_list_dir);
        folderListView.setAdapter(new ListDirAdapter(context, folderList));
    }

    private void initEvent() {
        folderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mListener != null) {
                    mListener.onSelected(folderList.get(i));
                }
            }
        });
    }

    /**
     * Calculate Popup Window size
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.6);
    }
}
