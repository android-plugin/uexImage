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
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.plugin.ueximage.EUExImage;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 用来缓存各种业务数据
 */
public class UEXImageUtil {
    private static final String TAG = "CommonUtil";
    private volatile static UEXImageUtil instance = null;
    //系统中存放图片的文件目录
    private Map<String, List<PictureInfo>> folderList= new HashMap<String, List<PictureInfo>>();
    //系统中的所有图片
    private List<PictureInfo> allPictureList = new ArrayList<PictureInfo>();
    //当前选中的图片集合
    private List<PictureInfo> checkedItems = new ArrayList<PictureInfo>();

    //当前正在操作的图片集合
    private List<PictureInfo> currentPicList = new ArrayList<PictureInfo>();

    public EUExImage euExImage;
    //图片临时保存的位置
    public static final String TEMP_PATH = "uex_image_temp";

    private UEXImageUtil() {}

    public Map<String, List<PictureInfo>> getFolderList() {
        return folderList;
    }

    public List<PictureInfo> getAllPictureList() {
        return allPictureList;
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
        folderList.clear();
        allPictureList.clear();
        checkedItems.clear();
        currentPicList.clear();

    }

    public void initAlbumList(Context context) {
        if (folderList.size() > 0) {
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
            //判断大图是否存在
            if (file.exists()) {
                //小图URI
                String thumbUri = CommonUtil.getThumbnail(context, id);
                //获取大图URI
                String uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().
                        appendPath(Integer.toString(id)).build().toString();
                if(TextUtils.isEmpty(uri))
                    continue;
                if (TextUtils.isEmpty(thumbUri))
                    thumbUri = uri;
                //获取目录名
                String folder = file.getParentFile().getName();

                PictureInfo pictureInfo = new PictureInfo();
                pictureInfo.setSrc(uri);
                pictureInfo.setThumb(thumbUri);

                allPictureList.add(pictureInfo);
                //判断文件夹是否已经存在
                if (folderList.containsKey(folder)) {
                    folderList.get(folder).add(pictureInfo);
                } else {
                    List<PictureInfo> files = new ArrayList<PictureInfo>();
                    files.add(pictureInfo);
                    folderList.put(folder, files);
                }
            }
        }
        folderList.put("所有图片", allPictureList);
        cursor.close();
        //如果传进来的max为0代表无限制
        if ( EUEXImageConfig.getInstance().getMaxImageCount() == 0) {
            EUEXImageConfig.getInstance().setMaxImageCount(allPictureList.size());
        }
    }

    public List<PictureInfo> getCheckedItems() {
        return checkedItems;
    }

    //此处会将选择的图片先复制一份到指定位置，再返回选择的图片的基本信息。
    public JSONObject getChoosedPicInfo(Context context) {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(false)
                .cacheOnDisk(false)
                .displayer(new SimpleBitmapDisplayer()).build();
        File f;
        List<String> filePathList = new ArrayList<String>();
        FileOutputStream fos = null;
        JSONObject result = new JSONObject();
        JSONArray detailedInfoArray = new JSONArray();
        for (PictureInfo picInfo : checkedItems) {
            String orginPicPath = ImageFilePath.getPath(context, Uri.parse(picInfo.getSrc()));
            Log.i(TAG, "orginPath:" + orginPicPath);
            Bitmap bitmap = ImageLoader.getInstance().loadImageSync(picInfo.getSrc(),options);
            if (EUEXImageConfig.getInstance().getIsUsePng()) {
                f = new File(Environment.getExternalStorageDirectory(),
                        File.separator + TEMP_PATH + File.separator + "temp_" + new Date().getTime() +".png");
            } else {
                f = new File(Environment.getExternalStorageDirectory(),
                        File.separator + TEMP_PATH + File.separator + "temp_" + new Date().getTime() +".jpg");
            }
            try {
                f.createNewFile();
                fos = new FileOutputStream(f);
                if ( EUEXImageConfig.getInstance().getIsUsePng()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, (int)  EUEXImageConfig.getInstance().getQuality() * 100, fos);
                }
                fos.flush();
                filePathList.add(f.getAbsolutePath());
                if( EUEXImageConfig.getInstance().isShowDetailedInfo()) {
                    JSONObject detailedInfo = getExifData(orginPicPath, f.getAbsolutePath());
                    detailedInfoArray.put(detailedInfo);
                }
            } catch (IOException e) {
                Log.i(TAG, "file copy error");
                Toast.makeText(context, "文件导出失败，请重新选择", Toast.LENGTH_SHORT).show();
                return result;
            } catch (JSONException e) {
                Log.i(TAG, e.getMessage());
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            result.put("isCancelled", false);
            result.put("data", filePathList);
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

//    public List<PictureInfo> transformData(JSONArray imageDataArray) {
//        int len = imageDataArray.length();
//        List<PictureInfo> imageDataList = new ArrayList<PictureInfo>();
//        for (int i = 0; i< len; i ++) {
//            try {
//                PictureInfo data = new PictureInfo();
//                JSONObject jsonObject = imageDataArray.getJSONObject(i);
//                data.setSrc(jsonObject.getString("src"));
//                if (jsonObject.has("thumb") && !TextUtils.isEmpty(jsonObject.getString("thumb"))) {
//                    data.setThumb(jsonObject.getString("thumb"));
//                }
//                if (jsonObject.has("desc") && !TextUtils.isEmpty(jsonObject.getString("desc"))) {
//                    data.setDesc(jsonObject.getString("desc"));
//                }
//                imageDataList.add(data);
//            }catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//        return imageDataList;
//    }

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
                }
                imageDataList.add(picInfo);
            }catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return imageDataList;
    }
}
