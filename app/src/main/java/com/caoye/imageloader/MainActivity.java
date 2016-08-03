 package com.caoye.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.caoye.imageloader.adapter.ImageAdapter;
import com.caoye.imageloader.bean.ImageFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

 public class MainActivity extends AppCompatActivity {
     private GridView mGirdView;
     private List<String> imgPathList;
     private ImageAdapter imgAdapter;

     private RelativeLayout mBottomLy;

     private TextView mDirName;
     private TextView mDirCount;
     private File mCurrentDir;
     private int mMaxCount;

     private List<ImageFolder> folderList = new ArrayList<>();
     private ProgressDialog mProgressDialog;

     private static final int DATA_LOADED = 0X110;

     private ImageDirPopupWindow mPopupWindow;


     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);

         initView();
         initData();
         initEvent();
     }

     private void initView() {
         mGirdView = (GridView) findViewById(R.id.id_gridView);
         mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
         mDirName = (TextView) findViewById(R.id.id_choose_dir);
         mDirCount = (TextView) findViewById(R.id.id_total_count);
     }

     /**
      * Use ContentProvider scan images in new Thread asynchronously
      */
     private void initData() {
         if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
             Toast.makeText(this, "Current SD Card is unavailable", Toast.LENGTH_SHORT).show();
             return;
         }

         mProgressDialog = mProgressDialog.show(this, null, "Loading images");
         new Thread() {
             @Override
             public void run() {
                 Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                 ContentResolver resolver = MainActivity.this.getContentResolver();
                 Cursor cursor = resolver.query(mImgUri, null,
                         MediaStore.Images.Media.MIME_TYPE + "=? or "
                                 + MediaStore.Images.Media.MIME_TYPE + "=?",
                         new String[] { "image/jpeg", "image/png" },
                         MediaStore.Images.Media.DATE_MODIFIED);

                 Set<String> parentPathSet = new HashSet<String>();
                 while (cursor.moveToNext()) {
                     String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                     File parentFile = new File(imagePath).getParentFile();
                     // No available images in the folder
                     if (parentFile == null || parentFile.list() == null) { continue; }
                     String parentPath = parentFile.getAbsolutePath();

                     ImageFolder folder = null;
                     if ( parentPathSet.contains(parentPath)) {
                         // Same parent folder
                         continue;
                     }
                     parentPathSet.add(parentPath);
                     folder = new ImageFolder();
                     folder.setDir(parentPath);
                     folder.setFirstImagePath(imagePath);

                     int picSize = parentFile.list(new FilenameFilter() {
                         @Override
                         public boolean accept(File file, String name) {
                             if (name.endsWith(".jpg")
                                     || name.endsWith(".jpeg")
                                     || name.endsWith(".png")) {
                                 return true;
                             }
                             return false;
                         }
                     }).length;
                     folder.setCount(picSize);
                     folderList.add(folder);

                     if (picSize > mMaxCount) {
                         mMaxCount = picSize;
                         mCurrentDir = parentFile;
                     }
                 }
                 cursor.close();

                 // Notify handler for scanning finish
                 handler.sendEmptyMessage(DATA_LOADED);
             }
         }.start();

     }

     private void initEvent() {
         /**
          * onClickListener for bottom layout -> popupWindow
          */
         mBottomLy.setOnClickListener(new View.OnClickListener()
         {
             @Override
             public void onClick(View v)
             {
                 mPopupWindow.setAnimationStyle(R.style.anim_popup_dir);
                 mPopupWindow.showAsDropDown(mBottomLy, 0, 0);
                 lightOff();
             }
         });
     }

     private Handler handler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
             if (msg.what == DATA_LOADED) {
                 mProgressDialog.dismiss();;
                 bindData2View();
                 initPopupWindow();
             }
         }
     };

     private void bindData2View() {
         if (mCurrentDir == null) {
             Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
             return;
         }

         imgPathList = Arrays.asList(mCurrentDir.list());
         imgAdapter = new ImageAdapter(this, imgPathList, mCurrentDir.getAbsolutePath());
         mGirdView.setAdapter(imgAdapter);

         mDirCount.setText(mMaxCount + "");
         mDirName.setText(mCurrentDir.getName());
     }

     private void initPopupWindow() {
         mPopupWindow = new ImageDirPopupWindow(this, folderList);
         mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
             @Override
             public void onDismiss() {
                 lightOn();
             }
         });
         mPopupWindow.setOnDirSelectedListener(new ImageDirPopupWindow.OnDirSelectedListener() {
             @Override
             public void onSelected(ImageFolder folder) {
                 mCurrentDir = new File(folder.getDir());
                 imgPathList = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                     @Override
                     public boolean accept(File file, String name) {
                         if (name.endsWith(".jpg")
                                 || name.endsWith(".jpeg")
                                 || name.endsWith(".png")) {
                             return true;
                         }
                         return false;
                     }
                 }));

                 imgAdapter = new ImageAdapter(MainActivity.this, imgPathList,
                         mCurrentDir.getAbsolutePath());
                 mGirdView.setAdapter(imgAdapter);

                 mDirCount.setText(imgPathList.size() + "");
                 mDirName.setText(folder.getName());
                 mPopupWindow.dismiss();
             }
         });
     }

     private void lightOn() {
         WindowManager.LayoutParams lp = getWindow().getAttributes();
         lp.alpha = 1.0f;
         getWindow().setAttributes(lp);
     }
     private void lightOff() {
         WindowManager.LayoutParams lp = getWindow().getAttributes();
         lp.alpha = .3f;
         getWindow().setAttributes(lp);
     }
 }
