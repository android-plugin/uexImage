package org.zywx.wbpalmstar.plugin.ueximage.util;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ace.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.ace.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.ImageLoaderConfiguration;
import com.ace.universalimageloader.core.assist.QueueProcessingType;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.ace.universalimageloader.core.download.BaseImageDownloader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.plugin.ueximage.EUExImage;
import org.zywx.wbpalmstar.plugin.ueximage.R;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用的工具方法
 */
public class CommonUtil {
    /**
     * 初始化imageLoader
     * @param context
     */
    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY);
        config.denyCacheImageMultipleSizesInMemory();
        config.memoryCacheExtraOptions(480, 800);
        config.memoryCacheSize((int) Runtime.getRuntime().maxMemory() / 4);
        config.memoryCache(new LRULimitedMemoryCache(8 * 1024 * 1024));
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(30 * 1024 * 1024);
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        //修改连接超时时间5秒，下载超时时间20秒
        config.imageDownloader(new BaseImageDownloader(context, 5 * 1000, 20 * 1000));
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }



    //获取缩略图的uri.
    public static String getThumbnail(Context context, int id) {
        String[] THUMBNAIL_STORE_IMAGE = {
                MediaStore.Images.Thumbnails._ID,
                MediaStore.Images.Thumbnails.DATA
        };
        //获取缩略图
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                THUMBNAIL_STORE_IMAGE,
                MediaStore.Images.Thumbnails.IMAGE_ID + " = ?",
                new String[]{id + ""},
                null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int thumId = cursor.getInt(0);
            String uri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI.buildUpon().
                    appendPath(Integer.toString(thumId)).build().toString();
            cursor.close();
            return uri;
        }
        cursor.close();
        return null;
    }

    /**
     * 根据传入的路径加载图片，imgUrl只能是 widget/wgtRes/ 或 / 开头
     * @param ctx
     * @param imgUrl
     * @return
     */
    public static Bitmap getLocalImage(Context ctx, String imgUrl) {
        if (imgUrl == null || imgUrl.length() == 0) {
            return null;
        }

        Bitmap bitmap = null;
        InputStream is = null;
        try {
            if (imgUrl.startsWith(BUtility.F_Widget_RES_path)) {
                try {
                    is = ctx.getAssets().open(imgUrl);
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (imgUrl.startsWith("/")) {
                bitmap = BitmapFactory.decodeFile(imgUrl);
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    /**
     * 将bitmap写入到指定文件中，写入的文件会是jpg格式
     * @param bitmap
     * @param file
     * @return
     */
    public static boolean saveBitmap2File (Bitmap bitmap, File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * 将assets目录下的文件拷到指下目录下。
     * @param context
     * @param path
     * @param destFile
     * @return
     */
    public static boolean saveFileFromAssetsToSystem(Context context, String path, File destFile) {
        BufferedInputStream bis = null;
        BufferedOutputStream fos = null;
        try {
            InputStream is = context.getAssets().open(path);
            bis =  new BufferedInputStream(is);
            fos = new BufferedOutputStream(new FileOutputStream(destFile));
            byte [] buf = new byte [2048];
            int i;
            while ((i= bis.read(buf))!= -1) {
                fos.write(buf, 0, i);
            }
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
    public static boolean copyFile(Context context, File fromFile, File toFile) {
        BufferedInputStream bis = null;
        BufferedOutputStream fos = null;
        try {
            FileInputStream is = new FileInputStream(fromFile);
            bis =  new BufferedInputStream(is);
            fos = new BufferedOutputStream(new FileOutputStream(toFile));
            byte [] buf = new byte [2048];
            int i;
            while ((i= bis.read(buf))!= -1) {
                fos.write(buf, 0, i);
            }
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }
}
