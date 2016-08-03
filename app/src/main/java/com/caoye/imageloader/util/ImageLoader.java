package com.caoye.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by admin on 8/1/16.
 */
public class ImageLoader {
    /**
     * Singleton pattern for ImageLoader instance
     */
    private static ImageLoader mInstance;
    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    private static final int DEFAULT_THREAD_COUNT = 1;
    public enum Type { FIFO, LIFO; }
    private Type mType = Type.LIFO;

    private LruCache<String, Bitmap> mLruCache;
    private ExecutorService mThreadPool;
    private LinkedList<Runnable> mTaskQueue;
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    private Handler mUIHandler;

    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    /**
     * Initialization
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        // Backend thread
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // Get a task from ThreadPool
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                // Release a Semaphore
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        // Get maximum available memory
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        // Create thread pool
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * Set ImageView from path
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // Get image from path and set to ImageView
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;

                    // Compare path with ImageView tag
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            // Get bitmap from LRUCache
            refreshBitmap(bm, path, imageView);
        } else {
           addTasks(new Runnable(){
               @Override
               public void run() {
                   // 1. Get imageView size
                   ImageSize imageSize = getImageViewSize(imageView);
                   // 2. Compress image
                   Bitmap bm = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);
                   // 3. add bitmap to LRUCache
                   addBitmapToLruCache(path, bm);

                   refreshBitmap(bm, path, imageView);
                   mSemaphoreThreadPool.release();
               }
           }); 
        }
    }

    private void refreshBitmap(Bitmap bm, String path, ImageView imageView) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        // if (mPoolThreadHandler == null) wait();
        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * Get width and height from ImageView
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth(); // Get actual width of imageView
        if (width <= 0) {
            width = lp.width; // Get declard width of imageView in layout
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check maximum width
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels; // Screen width
        }

        int height = imageView.getHeight(); // Get actual width of imageView
        if (height <= 0) {
            height = lp.width; // Get declard width of imageView in layout
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check maximum
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * Using reflection to get imageView field value
     * @param object
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * Compress bitmap from size required
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Get size WITHOUT cache to memory
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, width, height);
        // Use new options decode bitmap AGAIN
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        return bitmap;
    }

    /**
     * Calculate sampleSize from required size and actual size
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // actual size
        int height = options.outHeight;
        int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Ratio between actual and required size
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = Math.max(heightRatio, widthRatio);
        }
        return inSampleSize;
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }


    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    private class ImageSize {
        int width;
        int height;
    }
}
