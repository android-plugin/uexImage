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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureFolder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.vo.PicSizeVO;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ViewFrameVO;
import org.zywx.wbpalmstar.widgetone.dataservice.WWidgetData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.zywx.wbpalmstar.plugin.ueximage.util.Constants.TEMP_PATH;

/**
 * 用来缓存各种业务数据
 */
public class UEXImageUtil {
    private static final String TAG = "CommonUtil";
    private static int TOTAL_COUNT = 0;
    private volatile static UEXImageUtil instance = null;
    // 系统中存放图片的文件目录
    private List<PictureFolder> pictureFolderList = new ArrayList<PictureFolder>();
    // 临时辅助类，防止该文件夹被多次扫描
    private HashSet<String> tempDir = new HashSet<String>();
    // 系统中的所有图片
    private List<PictureInfo> allPictureList = new ArrayList<PictureInfo>();
    // 当前选中的图片集合
    private List<String> checkedItems = new ArrayList<String>();

    // 当前正在操作的图片集合
    private List<PictureInfo> currentPicList = new ArrayList<PictureInfo>();
    private static String imageCacheDir = "";
    private Bitmap bitmap;

    private UEXImageUtil() {
    }

    public List<PictureFolder> getPictureFolderList() {
        return pictureFolderList;
    }

    public List<PictureInfo> getCurrentPicList() {
        return currentPicList;
    }

    public void setCurrentPicList(List<PictureInfo> currentPicList) {
        this.currentPicList = currentPicList;
    }

    public static UEXImageUtil getInstance() {
        if (instance == null) {
            synchronized (CommonUtil.class) {
                if (instance == null) {
                    instance = new UEXImageUtil();
                }
            }
        }
        return instance;
    }

    public void resetData() {
        pictureFolderList.clear();
        allPictureList.clear();
        checkedItems.clear();
        currentPicList.clear();

    }

    public void initAlbumList(Context context) {
        if (pictureFolderList.size() > 0) {
            return;
        }
        //初始化时候重置图片总数
        TOTAL_COUNT=0;
        String[] STORE_IMAGES = {MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION};
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // 大图URI
                STORE_IMAGES, // 字段
                null, // No where clause
                null, // No where clause
                MediaStore.Images.Media.DATE_TAKEN + " DESC"); // 根据时间升序
        if (cursor == null)
            return;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);// 大图ID
            String path = cursor.getString(1);// 大图路径
            if (path == null) {
                continue;
            }
            File file = new File(path);
            // 获取目录名
            File parentFile = file.getParentFile();
            // 判断大图是否存在
            // note: 某vivo手机相册获取到了一个path只是一个文件名的图片，导致parentFile为null，暂时不明原因，但是这种照片显然是无法处理的，找不到此路径，因此此处做一个保护，防止闪退，保证逻辑正常进行。
            if (file.exists() && parentFile != null && parentFile.exists()) {
                String folderName = parentFile.getName();
                String dirPath = parentFile.getAbsolutePath();
                // 判断文件夹是否已经存在
                if (tempDir.contains(dirPath)) {
                    continue;
                }
                tempDir.add(dirPath);
                String uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        .buildUpon().appendPath(Integer.toString(id)).build()
                        .toString();
                PictureFolder pictureFolder = new PictureFolder();
                pictureFolder.setFolderName(folderName);
                pictureFolder.setFirstImagePath(uri);
                pictureFolder.setFolderPath(dirPath);
                int picCount = parentFile.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return CommonUtil.isPicture(filename);
                    }
                }).length;
                TOTAL_COUNT = TOTAL_COUNT + picCount;
                pictureFolder.setCount(picCount);
                pictureFolderList.add(pictureFolder);
            }else{
                BDebug.e(TAG, "initAlbumList got an error path: " + path);
            }
        }
        cursor.close();
        tempDir.clear();
        //如果传进来的max为0代表无限制
        if (EUEXImageConfig.getInstance().getMaxImageCount() == 0) {
            EUEXImageConfig.getInstance().setMaxImageCount(TOTAL_COUNT);
        }
    }

    public List<String> getCheckedItems() {
        return checkedItems;
    }

    private void cleanHistoryTempDir() {
        deleteDirectory(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + TEMP_PATH);
    }

    private boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    private boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    //此处会将选择的图片先复制一份到指定位置，再返回选择的图片的基本信息。
    public JSONObject getChoosedPicInfo(Context context) {
        cleanHistoryTempDir();
        File f;
        JSONArray filePathArray = new JSONArray();
        FileOutputStream fos = null;
        FileInputStream in = null;
        JSONObject result = new JSONObject();
        JSONArray detailedInfoArray = new JSONArray();
        for (String picPath : checkedItems) {
            String orginPicPath = ImageFilePath.getPath(context,
                    Uri.parse(picPath));
            if (EUEXImageConfig.getInstance().getIsUsePng()) {
                f = new File(
                        UEXImageUtil.getImageCacheDir(context) + File.separator
                                + "temp_" + new Date().getTime() + ".png");
            } else {
                f = new File(
                        UEXImageUtil.getImageCacheDir(context) + File.separator
                                + "temp_" + new Date().getTime() + ".jpg");
            }

            try {
                int angle = 0;
                File file = new File(orginPicPath);
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                Log.i("tag", "读取角度-" + orientation);
                switch (orientation){
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        angle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        angle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        angle = 180;
                        break;
                    default:
                        angle = ExifInterface.ORIENTATION_UNDEFINED;
                        break;
                }
                Matrix matrix = new Matrix();
                matrix.postRotate(angle);
                Bitmap bitmap2 = BitmapFactory.decodeFile(orginPicPath);
                Bitmap resizedBitmap = Bitmap.createBitmap(bitmap2, 0, 0,
                        bitmap2.getWidth(), bitmap2.getHeight(), matrix, true);
                File parent=f.getParentFile();
                if (!parent.exists()){
                    parent.mkdirs();
                }
                f.createNewFile();
                fos = new FileOutputStream(f);
                if ((int) (EUEXImageConfig.getInstance().getQuality()) == 1) {
                    in = new FileInputStream(new File(orginPicPath));
                    byte[] buffer = new byte[1024];
                    int byteRead = 0;
                    while ((byteRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, byteRead);
                    }
                } else {
                    //   ACEImageLoader.getInstance() .getBitmapSync(picPath)
                   Bitmap bitmap = resizedBitmap;
                    if (bitmap != null) {
                        if (EUEXImageConfig.getInstance().getIsUsePng()) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                    fos);
                        } else {
                            //(int)EUEXImageConfig.getInstance().getQuality()
                            bitmap.compress(Bitmap.CompressFormat.JPEG,
                                    (1*100),
                                    fos);
                        }
                    } else {
                        Toast.makeText(context,
                                EUExUtil.getString(
                                        "plugin_uex_image_image_laoad_error"),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                fos.flush();
                filePathArray.put(f.getAbsolutePath());
                if (EUEXImageConfig.getInstance().isShowDetailedInfo()) {
                    JSONObject detailedInfo = getExifData(orginPicPath,
                            String.valueOf(f.getAbsolutePath()));
                    detailedInfo.put("orginPicPath",orginPicPath);
                    detailedInfoArray.put(detailedInfo);
                }
            } catch (IOException e) {
                Log.i(TAG, "file copy error");
                return result;
            } catch (JSONException e) {
                Log.i(TAG, e.getMessage());
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            result.put("isCancelled", false);
            result.put("data", filePathArray);
            result.put("detailedImageInfo", detailedInfoArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;

    }

    private JSONObject getExifData(String orginPicPath, String tempPath)
            throws IOException, JSONException {
        ExifInterface exif = new ExifInterface(orginPicPath);
        float[] latLong = new float[2];
        exif.getLatLong(latLong);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("localPath", tempPath);
        if (latLong[0] > 0 && latLong[1] > 0) {
            jsonObject.put("latitude", latLong[0]);
            jsonObject.put("longitude", latLong[1]);
        }
        double altitude = exif.getAltitude(0.0);
        if (altitude > 0) {
            jsonObject.put("altitude", altitude);
        }
        String timeStr = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (!TextUtils.isEmpty(timeStr)) {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy:MM:dd HH:mm:ss");
            try {
                Date date = format.parse(timeStr);
                jsonObject.put("timestamp", date.getTime() / 1000);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    public List<PictureInfo> transformData(JSONArray imageDataArray) {
        int len = imageDataArray.length();
        List<PictureInfo> imageDataList = new ArrayList<PictureInfo>();
        for (int i = 0; i < len; i++) {
            try {
                PictureInfo picInfo = new PictureInfo();
                // 针对只传一个字符串这种情况
                if (imageDataArray.get(i) instanceof String) {
                    String realPath = imageDataArray.getString(i);
                    picInfo.setSrc(realPath);
                } else {
                    JSONObject jsonObject = imageDataArray.getJSONObject(i);
                    picInfo.setSrc(jsonObject.getString("src"));
                    if (jsonObject.has("thumb") && !TextUtils
                            .isEmpty(jsonObject.getString("thumb"))) {
                        picInfo.setThumb(jsonObject.getString("thumb"));
                    }
                    if (jsonObject.has("desc") && !TextUtils
                            .isEmpty(jsonObject.getString("desc"))) {
                        picInfo.setDesc(jsonObject.getString("desc"));
                    }
                    picInfo.setShowBigPic(
                            jsonObject.optBoolean("showBigPic", false));
                }
                imageDataList.add(picInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return imageDataList;
    }

    /**
     * getCustomScale：引擎中添加的获取x5内核网页scale的方法，为兼容旧引擎，故使用反射调用
     *
     * @param mBrwView
     * @return
     */
    private static float getWebScale(EBrowserView mBrwView) {
        float scale = 1.0f;
        try {
            Method gatScale = EBrowserView.class.getMethod("getScaleWrap",
                    new Class[0]);
            try {
                scale = (Float) gatScale.invoke(mBrwView, new Object[]{});
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            scale = getWebScaleOld(mBrwView);
        }

        return scale;
    }

    private static float getWebScaleOld(EBrowserView mBrwView) {
        float nowScale = 1.0f;
        int versionA = Build.VERSION.SDK_INT;
        if (versionA <= 18) {
            nowScale = mBrwView.getScale();
        }
        return nowScale;
    }

    public static ViewFrameVO getFullScreenViewFrameVO(Context context,
                                                       EBrowserView mBrwView) {
        ViewFrameVO viewFrameVO = new ViewFrameVO();
        float nowScale = getWebScale(mBrwView);
        final View contextView = ((Activity) context).getWindow()
                .getDecorView();
        viewFrameVO = new ViewFrameVO();
        viewFrameVO.x = 0;
        viewFrameVO.y = 0;
        viewFrameVO.width = (int) (contextView.getMeasuredWidth() / nowScale);
        /** 去掉状态栏的高度 */
        int measuredHeight = contextView.getMeasuredHeight();
        int statusBarHeight = getStatusBarHeight(context);
        int navigationBarHeight = getNavigationBarHeight(context);
        BDebug.d(TAG, "measuredHeight = " + measuredHeight
                + " statusBarHeight = " + statusBarHeight
                + " navigationBarHeight = " + navigationBarHeight);
        viewFrameVO.height = (int) Math.ceil(
                (contextView.getMeasuredHeight() - getStatusBarHeight(context)-getNavigationBarHeight(context))
                        / nowScale);
        return viewFrameVO;
    }

    /**获取导航栏的高度 */
    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Navi height:" + height);
        return height;
    }

    /**
     * 获取状态栏高度
     *
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 判断是否是沉浸式状态栏
     *
     * @param activity
     * @return
     */
    public static boolean isTranslucentStatusBar(Activity activity){
        boolean isTranslucentStatusBar = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // 如果activity的windowflag中存在透明状态栏的配置，并且config.xml中也配置了statusbar为透明。则认为是沉浸式状态栏
                isTranslucentStatusBar = hasFlag(activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) && isEngineConfigStatusBarTranlucent();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return isTranslucentStatusBar;
    }

    /**
     * 判断config.xml中是否设置了沉浸式状态栏
     *
     * @return
     */
    public static boolean isEngineConfigStatusBarTranlucent(){
        return WWidgetData.sStatusBarColor == 0;
    }

    private static boolean hasFlag(Activity activity, int flag) {
        return (activity.getWindow().getAttributes().flags & flag) == flag;
    }

    public static int getInSampleSize(double scale) {
        double log = Math.log(scale) / Math.log(2);
        double logCeil = Math.ceil(log);
        return ((int) Math.pow(2, logCeil) + 1);
    }

    public static PicSizeVO getPicSizeVOList(int desLength) {
        PicSizeVO mPicSizeVO = null;
        switch (desLength) {
            case Constants.DES_FILE_LENGTH_10K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_10K,
                        Constants.WIDTH_10K);
                break;
            case Constants.DES_FILE_LENGTH_30K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_30K,
                        Constants.WIDTH_30K);
                break;
            case Constants.DES_FILE_LENGTH_100K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_100K,
                        Constants.WIDTH_100K);
                break;
            case Constants.DES_FILE_LENGTH_200K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_200K,
                        Constants.WIDTH_200K);
                break;
            case Constants.DES_FILE_LENGTH_300K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_300K,
                        Constants.WIDTH_300K);
                break;
            case Constants.DES_FILE_LENGTH_500K:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_500K,
                        Constants.WIDTH_500K);
            default:
                mPicSizeVO = new PicSizeVO(Constants.HEIGHT_300K,
                        Constants.WIDTH_300K);
                break;
        }
        return mPicSizeVO;
    }

    /**
     * 用于保证能够得到一个缓存目录 by yipeng
     *
     * @param context
     * @return
     */
    private static File getExternalCacheDir(Context context) {
        File path = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            path = context.getExternalCacheDir();
            // In some case, even the sd card is mounted,
            // getExternalCacheDir will return null
            // may be it is nearly full.
        }
        if (null == path) {
            // Before Froyo or the path is null,
            // we need to construct the external cache folder ourselves
            final String cacheDir = "/Android/data/" + context.getPackageName()
                    + "/cache/";
            path = new File(Environment.getExternalStorageDirectory().getPath()
                    + cacheDir);
        }
        return path;
    }

    public static String getImageCacheDir(Context context) {
        if (TextUtils.isEmpty(imageCacheDir)) {
            String cacheDir = "";
            if (android.os.Environment.getExternalStorageState()
                    .equals(android.os.Environment.MEDIA_MOUNTED)) {
                cacheDir = getExternalCacheDir(context).getAbsolutePath();
            } else {
                cacheDir = context.getFilesDir().getAbsolutePath();
            }
            imageCacheDir = cacheDir + File.separator + TEMP_PATH;
        }
        return imageCacheDir;
    }
}
