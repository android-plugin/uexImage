/*
 * Copyright (c) 2015.  The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package org.zywx.wbpalmstar.plugin.ueximage.util;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;

import com.ace.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.ace.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.ImageLoaderConfiguration;
import com.ace.universalimageloader.core.assist.QueueProcessingType;
import com.ace.universalimageloader.core.download.BaseImageDownloader;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        config.imageDownloader(new BaseImageDownloader(
                context.getApplicationContext(), 5 * 1000, 20 * 1000));
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
     * 获取指定路径下的图片信息列表
     * @param dir
     * @return 返回图片信息列表
     */
    public static List<PictureInfo> getPictureInfo(Context context, String dir) {

        List<PictureInfo> list = new ArrayList<PictureInfo>();
        File [] files = new File(dir).listFiles();
        String path = null;
        for (File f: files) {
            path = f.getAbsolutePath();
            if(CommonUtil.isPicture(path)) {
                PictureInfo pictureInfo = new PictureInfo();
                pictureInfo.setLastModified(f.lastModified());
                pictureInfo.setSrc("file://" + path);
                list.add(pictureInfo);
            }
        }
        return list;
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
            file.deleteOnExit();
            file.createNewFile();
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
    public static boolean copyFile(File fromFile, File toFile) {
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
    //判断文件是否是图片
    public static boolean isPicture(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".png")
                || fileName.endsWith(".jpeg") || fileName.endsWith(".JPG")
                || fileName.endsWith(".PNG") || fileName.endsWith(".JPEG")
                || fileName.endsWith(".bmp") || fileName.endsWith(".gif")
                || fileName.endsWith(".webp");
    }
}
