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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.base.cache.DiskCache;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureFolder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


/**
 * 用来缓存各种业务数据
 */
public class UEXImageUtil {
    private static final String TAG = "CommonUtil";
    private static int TOTAL_COUNT = 0;
    private volatile static UEXImageUtil instance = null;
    //系统中存放图片的文件目录
    private List<PictureFolder> pictureFolderList= new ArrayList<PictureFolder>();
    //临时辅助类，防止该文件夹被多次扫描
    private HashSet<String> tempDir = new HashSet<String>();
    //系统中的所有图片
    private List<PictureInfo> allPictureList = new ArrayList<PictureInfo>();
    //当前选中的图片集合
    private List<String> checkedItems = new ArrayList<String>();

    //当前正在操作的图片集合
    private List<PictureInfo> currentPicList = new ArrayList<PictureInfo>();

    //图片临时保存的位置
    public static final String TEMP_PATH = "uex_image_temp";

    private UEXImageUtil() {}

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
            synchronized(CommonUtil.class) {
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
        String[] STORE_IMAGES = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION
        };
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,  // 大图URI
                STORE_IMAGES,   // 字段
                null,         // No where clause
                null,         // No where clause
                MediaStore.Images.Media.DATE_TAKEN + " DESC"); //根据时间升序
        if (cursor == null)
            return ;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);//大图ID
            String path = cursor.getString(1);//大图路径
            File file = new File(path);
            //获取目录名
            File parentFile = file.getParentFile();
            String folderName = parentFile.getName();
            String dirPath = parentFile.getAbsolutePath();
            //判断大图是否存在
            if (file.exists()) {
                //判断文件夹是否已经存在
                if (tempDir.contains(dirPath)) {
                    continue;
                }
                tempDir.add(dirPath);
                String uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().
                        appendPath(Integer.toString(id)).build().toString();
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
            }
        }
        cursor.close();
        tempDir.clear();
        //如果传进来的max为0代表无限制
        if ( EUEXImageConfig.getInstance().getMaxImageCount() == 0) {
            EUEXImageConfig.getInstance().setMaxImageCount(TOTAL_COUNT);
        }
    }

    public List<String> getCheckedItems() {
        return checkedItems;
    }

    private void cleanHistoryTempDir(){
        deleteDirectory(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator + TEMP_PATH);
    }

    public boolean deleteDirectory(String filePath) {
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

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    //此处会将选择的图片先复制一份到指定位置，再返回选择的图片的基本信息。
    public JSONObject getChoosedPicInfo(Context context) {

        //删除之前创建的临时目录
        cleanHistoryTempDir();

        File f;
        JSONArray filePathArray = new JSONArray();
        FileOutputStream fos = null;
        FileInputStream in = null;
        JSONObject result = new JSONObject();
        JSONArray detailedInfoArray = new JSONArray();
        for (String picPath : checkedItems) {
            String orginPicPath = ImageFilePath.getPath(context, Uri.parse(picPath));
            Bitmap bitmap = ACEImageLoader.getInstance().getBitmapSync(picPath);
            if (EUEXImageConfig.getInstance().getIsUsePng()) {
                f = new File(DiskCache.cacheFolder,
                        File.separator + TEMP_PATH + File.separator + "temp_" + new Date().getTime() +".png");
            } else {
                f = new File(DiskCache.cacheFolder,
                        File.separator + TEMP_PATH + File.separator + "temp_" + new Date().getTime() +".jpg");
            }
            try {
                File parent=f.getParentFile();
                if (!parent.exists()){
                    parent.mkdirs();
                }
                f.createNewFile();
                fos = new FileOutputStream(f);
                if ((int)(EUEXImageConfig.getInstance().getQuality()) == 1) {
                    in = new FileInputStream(new File(orginPicPath));
                    byte[] buffer = new byte[1024];
                    int byteRead = 0;
                    while ((byteRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, byteRead);
                    }
                } else {
                    if (bitmap!=null){
                        if ( EUEXImageConfig.getInstance().getIsUsePng()) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } else {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, (int)(EUEXImageConfig.getInstance().getQuality() * 100), fos);
                        }
                    }
                }

                fos.flush();
                filePathArray.put(f.getAbsolutePath());
                if( EUEXImageConfig.getInstance().isShowDetailedInfo()) {
                    JSONObject detailedInfo = getExifData(orginPicPath, f.getAbsolutePath());
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

    private JSONObject getExifData(String orginPicPath, String tempPath) throws IOException, JSONException {
        ExifInterface exif = new ExifInterface(orginPicPath);
        float [] latLong = new float[2];
        exif.getLatLong(latLong);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("localPath", tempPath);
        if (latLong[0] > 0 && latLong[1] >0) {
            jsonObject.put("latitude", latLong[0]);
            jsonObject.put("longitude", latLong[1]);
        }
        double altitude = exif.getAltitude(0.0);
        if (altitude > 0) {
            jsonObject.put("altitude", altitude);
        }
        String timeStr = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (!TextUtils.isEmpty(timeStr)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
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
        for (int i = 0; i< len; i ++) {
            try {
                PictureInfo picInfo = new PictureInfo();
                //针对只传一个字符串这种情况
                if(imageDataArray.get(i) instanceof String) {
                    String realPath = imageDataArray.getString(i);
                    picInfo.setSrc(realPath);
                } else {
                    JSONObject jsonObject = imageDataArray.getJSONObject(i);
                    picInfo.setSrc(jsonObject.getString("src"));
                    if (jsonObject.has("thumb") && !TextUtils.isEmpty(jsonObject.getString("thumb"))) {
                        picInfo.setThumb(jsonObject.getString("thumb"));
                    }
                    if (jsonObject.has("desc") && !TextUtils.isEmpty(jsonObject.getString("desc"))) {
                        picInfo.setDesc(jsonObject.getString("desc"));
                    }
                    picInfo.setShowBigPic(jsonObject.optBoolean("showBigPic", false));
                }
                imageDataList.add(picInfo);
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return imageDataList;
    }
}
