package org.zywx.wbpalmstar.plugin.ueximage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class EUExImage extends EUExBase {
    private static final String TAG = "EUExImage";
    private static final String BUNDLE_DATA = "data";
    private static final int MSG_OPEN_PICKER = 1;
    private static final int MSG_OPEN_BROWSER = 2;
    private static final int MSG_OPEN_CROPPER = 3;
    private static final int MSG_SAVE_TO_PHOTO_ALBUM = 4;
    private static final int MSG_CLEAR_OUTPUT_IMAGES = 5;

    private double cropQuality = 0.5;
    private boolean cropUsePng = false;
    //当前Android只支持方型裁剪
    private int cropMode = 1;

    //裁剪操作的状态，1 代表成功， 2 代表失败，3代表用户取消。
    private int cropStatus = 1;

    public static final int REQUEST_CROP_IMAGE = 100;
    public static final int REQUEST_IMAGE_PICKER = 101;
    public static final int REQUEST_IMAGE_BROWSER = 102;
    private Context context;
    private UEXImageUtil uexImageUtil;

    private ResoureFinder finder;
    private final String FILE_SYSTEM_ERROR = "文件系统操作出错";
    private final String SAME_FILE_IN_DCIM = "系统相册中存在同名文件";
    private final String JSON_FORMAT_ERROR = "json格式错误";
    private final String NOT_SUPPORT_CROP = "你的设备不支持剪切功能！";


    public EUExImage(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
        this.context = context;
        //创建缓存文件夹
        File f = new File(Environment.getExternalStorageDirectory(),
                File.separator + UEXImageUtil.TEMP_PATH);
        if (!f.exists()) {
            f.mkdirs();
        }
        CommonUtil.initImageLoader(context);
        uexImageUtil = UEXImageUtil.getInstance();
        finder = ResoureFinder.getInstance(context);

    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void openPicker(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_OPEN_PICKER;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void openPickerMsg(String[] params) {
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            if (jsonObject.has("min")) {
                int min = jsonObject.getInt("min");
                EUEXImageConfig.getInstance().setMinImageCount(min);
            }
            if (jsonObject.has("max")) {
                int max = jsonObject.getInt("max");
                EUEXImageConfig.getInstance().setMaxImageCount(max);
            }
            if (jsonObject.has("quality")){
                double quality = jsonObject.getDouble("quality");
                EUEXImageConfig.getInstance().setQuality(quality);
            }
            if (jsonObject.has("usePng")) {
                Boolean usePng = jsonObject.getBoolean("usePng");
                EUEXImageConfig.getInstance().setIsUsePng(usePng);
            }
            if (jsonObject.has("detailedInfo")) {
                Boolean detailedInfo = jsonObject.getBoolean("detailedInfo");
                EUEXImageConfig.getInstance().setIsShowDetailedInfo(detailedInfo);
            }
            EUEXImageConfig.getInstance().setIsOpenBrowser(false);
            Intent intent = new Intent(context, AlbumListActivity.class);
            startActivityForResult(intent, REQUEST_IMAGE_PICKER);

        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }

    }

    public void openBrowser(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_OPEN_BROWSER;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void openBrowserMsg(String[] params) {
        String json = params[0];
        try {
            JSONObject jsonObject = new JSONObject(json);
            EUEXImageConfig config = EUEXImageConfig.getInstance();
            if (!jsonObject.has("data")) {
                Toast.makeText(context, "data不能为空", Toast.LENGTH_SHORT).show();
                return;
            } else {
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i< data.length(); i ++) {
                    if(data.get(i) instanceof  String) {
                        String path = data.getString(i);
                        String realPath = BUtility.makeRealPath(
                                BUtility.makeUrl(mBrwView.getCurrentUrl(), path),
                                mBrwView.getCurrentWidget().m_widgetPath,
                                mBrwView.getCurrentWidget().m_wgtType);
                        data.put(i, realPath);
                    } else {
                        JSONObject obj = data.getJSONObject(i);
                        if (!obj.has("src")) {
                            Toast.makeText(context, "data中第"+ (i + 1)+"个元素的src不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String src = obj.getString("src");
                        String srcPath = BUtility.makeRealPath(
                                BUtility.makeUrl(mBrwView.getCurrentUrl(), src),
                                mBrwView.getCurrentWidget().m_widgetPath,
                                mBrwView.getCurrentWidget().m_wgtType);
                        obj.put("src", srcPath);
                        if (obj.has("thumb")) {
                            String thumb = obj.getString("thumb");
                            String thumbPath = BUtility.makeRealPath(
                                    BUtility.makeUrl(mBrwView.getCurrentUrl(), thumb),
                                    mBrwView.getCurrentWidget().m_widgetPath,
                                    mBrwView.getCurrentWidget().m_wgtType);
                            obj.put("thumb", thumbPath);
                            Log.i(TAG, "thumb:" + thumb);
                        }
                    }
                }
                config.setDataArray(data);
            }
            if (jsonObject.has("displayActionButton")) {
                boolean isDisplayActionButton = jsonObject.getBoolean("displayActionButton");
                config.setIsDisplayActionButton(isDisplayActionButton);
            }
            if (jsonObject.has("enableGrid")) {
                boolean enableGrid = jsonObject.getBoolean("enableGrid");
                config.setEnableGrid(enableGrid);
            }
            if (jsonObject.has("startOnGrid")) {
                boolean isStartOnGrid = jsonObject.getBoolean("startOnGrid");
                config.setIsStartOnGrid(isStartOnGrid);
                if (!config.isEnableGrid() && isStartOnGrid) {
                    Toast.makeText(context, "startOnGrid为true时，enableGrid不能为false", Toast.LENGTH_SHORT).show();
                }
            }
            //Android不支持
            //boolean isDisplayNavArrows = jsonObject.getBoolean("displayNavArrows");

            if (jsonObject.has("startIndex")) {
                int startIndex = jsonObject.getInt("startIndex");
                if (startIndex < 0) {
                    startIndex = 0;
                }
                config.setStartIndex(startIndex);
            }
            JSONArray data = config.getDataArray();

            config.setIsOpenBrowser(true);
            Intent intent;
            if (config.isStartOnGrid()) {
                intent = new Intent(context, PictureGridActivity.class);
            } else {
                intent = new Intent(context, ImagePreviewActivity.class);
            }
            startActivityForResult(intent, REQUEST_IMAGE_BROWSER );
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
    }

    public void openCropper(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_OPEN_CROPPER;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void openCropperMsg(String[] params) {
        String json = params[0];
        String src = "";
        String srcPath = "";
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.has("src") || TextUtils.isEmpty(jsonObject.getString("src"))) {
                Toast.makeText(context, "src不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            src = jsonObject.getString("src");
            srcPath = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), src),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            if (jsonObject.has("quality")) {
                double qualityParam = jsonObject.getDouble("quality");
                if (qualityParam < 0 || qualityParam > 1) {
                    Toast.makeText(context, "quality 只能在0-1之间", Toast.LENGTH_SHORT).show();
                } else {
                    cropQuality = qualityParam;
                }
            }
            if (jsonObject.has("usePng")) {
                cropUsePng = jsonObject.getBoolean("usePng");
            }
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            Toast.makeText(context, "JSON解析错误", Toast.LENGTH_SHORT).show();
        }
        File file;
        //先将assets文件写入到临时文件夹中
        if (src.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
            String fileName = ".png";
            if (!src.endsWith("PNG") && !src.endsWith("png")) {
                fileName = ".jpg";
            }
            //为res对应的文件生成一个临时文件到系统中
            File destFile = new File(Environment.getExternalStorageDirectory(),
                    File.separator + UEXImageUtil.TEMP_PATH + File.separator + "crop_res_temp" +fileName);
            try {
                destFile.deleteOnExit();
                destFile.createNewFile();
            } catch (IOException e) {
                Toast.makeText(context, FILE_SYSTEM_ERROR, Toast.LENGTH_SHORT).show();
                return;
            }
            if(CommonUtil.saveFileFromAssetsToSystem(context, srcPath, destFile)) {
                file = destFile;
            } else {
                Toast.makeText(context, FILE_SYSTEM_ERROR, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            file = new File(srcPath);
        }
        updateGallery(file.getAbsolutePath());
        performCrop(file);
    }

    private void performCrop(File imageFile) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(Uri.fromFile(imageFile), "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("return-data", true);
            startActivityForResult(cropIntent, REQUEST_CROP_IMAGE);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, NOT_SUPPORT_CROP, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGallery(String filename) {
        MediaScannerConnection.scanFile(context,
                new String[]{filename}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //裁剪图片
        if (requestCode == REQUEST_CROP_IMAGE) {
            cropCallBack(resultCode, data);
        }
        //选择图片
        if (requestCode == REQUEST_IMAGE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject jsonObject= uexImageUtil.getChoosedPicInfo(context);
                callBackPluginJs(JsConst.CALLBACK_ON_PICKER_CLOSED, jsonObject.toString());
            } else if (resultCode == Constants.OPERATION_CANCELLED) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("isCancelled", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                callBackPluginJs(JsConst.CALLBACK_ON_PICKER_CLOSED, jsonObject.toString());
            }
            uexImageUtil.resetData();
        }
        //浏览图片
        if (requestCode == REQUEST_IMAGE_BROWSER) {
            callBackPluginJs(JsConst.CALLBACK_ON_BROWSER_CLOSED, "pic browser closed");
        }
    }

    public void cropCallBack(int resultCode, Intent data) {
        File f = null;
        if (resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null){
                Bitmap bmp = extras.getParcelable("data");
                String fileName;
                if (cropUsePng) {
                    fileName = "crop_temp.png";
                } else {
                    fileName = "crop_temp.jpg";
                }
                f = new File(Environment.getExternalStorageDirectory(),
                        File.separator + UEXImageUtil.TEMP_PATH + File.separator + fileName);
                f.deleteOnExit();
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    if(cropUsePng) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    } else {
                        bmp.compress(Bitmap.CompressFormat.JPEG, (int) (cropQuality * 100), fos);

                    }
                    fos.flush();
                } catch (IOException e) {
                    Log.i(TAG, e.getMessage());
                    cropStatus = 2;
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e1) {
                            Log.i(TAG, e1.getMessage());
                        }
                    }
                }
                updateGallery(f.getAbsolutePath());
                cropStatus = 1;
                Log.i(TAG, "crop success -------->");
            }
        } else {
            cropStatus = 3;
            Log.i(TAG, "crop Fail -------->");
        }
        JSONObject result = new JSONObject();
        try {
            if (cropStatus == 1) {
                result.put("isCancelled", false);
                result.put("data", f.getAbsolutePath());
            }
            if(cropStatus == 3) {
                result.put("isCancelled", true);
            }
            if (cropStatus == 2) {
                result.put("isCancelled", false);
                result.put("data", "系统错误");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callBackPluginJs(JsConst.CALLBACK_ON_CROPPER_CLOSED, result.toString());
    }
    public void saveToPhotoAlbum(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_SAVE_TO_PHOTO_ALBUM;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void saveToPhotoAlbumMsg(String[] params) {
        String json = params[0];
        //回调的结果
        JSONObject resultObject = new JSONObject();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.has("localPath") || TextUtils.isEmpty(jsonObject.getString("localPath"))) {
                Toast.makeText(context, "localPath不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (jsonObject.has("extraInfo")) {
                resultObject.put("extraInfo", jsonObject.getString("extraInfo"));
            }
            String path = jsonObject.getString("localPath");
            String realPath = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), path),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            //如果传的是res,则会复制一份到相册
            if (path.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
                //获取文件名
                String fileName = path.replace(BUtility.F_Widget_RES_SCHEMA, "");
                String dcimPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
                File file = new File(dcimPath, fileName);
                if (file.exists()) {
                    resultObject.put("isSuccess", "false");
                    resultObject.put("errorStr", SAME_FILE_IN_DCIM);
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, resultObject.toString());
                    return;
                }
                file.createNewFile();

                if(CommonUtil.saveFileFromAssetsToSystem(context, realPath, file)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(file.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", FILE_SYSTEM_ERROR);
                }
            } else {//如果傳的是別的路徑，也復制一份吧。
                Log.i(TAG, "Path:" + realPath);
                File fromFile = new File(realPath);
                String fileName = fromFile.getName();

                String dcimPath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator;
                File destFile = new File(dcimPath, fileName);
                if (destFile.exists()) {
                    resultObject.put("isSuccess", "false");
                    resultObject.put("errorStr", SAME_FILE_IN_DCIM);
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, resultObject.toString());
                    return;
                }

                if (CommonUtil.copyFile(context, new File(realPath), destFile)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(destFile.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", FILE_SYSTEM_ERROR);
                }
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, resultObject.toString());
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr", JSON_FORMAT_ERROR);
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, resultObject.toString());
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr", FILE_SYSTEM_ERROR);
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, resultObject.toString());
        }
    }


    public void clearOutputImages(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_CLEAR_OUTPUT_IMAGES;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void clearOutputImagesMsg(String[] params) {
        JSONObject jsonResult = new JSONObject();
        File directory = new File(Environment.getExternalStorageDirectory(),
                File.separator + UEXImageUtil.TEMP_PATH);
        for (File file : directory.listFiles()) {
            file.delete();
        }
        try {
            jsonResult.put("status", "ok");
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
        callBackPluginJs(JsConst.CALLBACK_CLEAR_OUTPUT_IMAGES, jsonResult.toString());
    }

    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_OPEN_PICKER:
                openPickerMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_OPEN_BROWSER:
                openBrowserMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_OPEN_CROPPER:
                openCropperMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_SAVE_TO_PHOTO_ALBUM:
                saveToPhotoAlbumMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_CLEAR_OUTPUT_IMAGES:
                clearOutputImagesMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
        }
    }

    private void callBackPluginJs(String methodName, String jsonData){
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

}
