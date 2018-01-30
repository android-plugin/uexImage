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
package org.zywx.wbpalmstar.plugin.ueximage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.ImageBaseView.ViewEvent;
import org.zywx.wbpalmstar.plugin.ueximage.crop.Crop;
import org.zywx.wbpalmstar.plugin.ueximage.model.LabelInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.DataParser;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;
import org.zywx.wbpalmstar.plugin.ueximage.vo.CompressImageVO;
import org.zywx.wbpalmstar.plugin.ueximage.vo.OpenCropperVO;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ViewFrameVO;
import org.zywx.wbpalmstar.plugin.ueximage.widget.LabelView;
import org.zywx.wbpalmstar.plugin.ueximage.widget.LabelViewContainer;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EUExImage extends EUExBase {
    private static final String TAG = "EUExImage";
    private static final int REQUEST_CHOOSE_IMAGE=100;
    private double cropQuality = 0.5;
    private boolean cropUsePng = false;
    // 当前Android只支持方型裁剪, 即cropMode为1
    // cropMode 为4: 矩型裁剪,比例为 4:3, cropMode为5: 矩形裁剪, 比例为16:9 cropMode 为6：自由缩放
    private int cropMode = 1;
    private File cropOutput = null;
    private Context context;
    private UEXImageUtil uexImageUtil;

    // openPicker对应的回调函数
    private String openPickerFuncId; // openBrowser对应的回调函数
    private String openBrowserFuncId;
    private String openCropperId;
    private String compressImageFunCbId = "";
    private RelativeLayout labelViewContainer;

    private OpenCropperVO tempOpenCropperVO;//临时保存crop相关参数

    /**
     * 保存添加到网页的view
     */
    private static ConcurrentHashMap<String, View> addToWebViewsMap = new ConcurrentHashMap<String, View>();
    private ImageAgent mImageAgent = null;

    public EUExImage(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
        this.context = context;
        // 创建缓存文件夹
        File f = new File(UEXImageUtil.getImageCacheDir(context));
        if (!f.exists()) {
            f.mkdirs();
        }
        File noMediaFile = new File(UEXImageUtil.getImageCacheDir(context)
                + File.separator + Constants.NO_MEDIA);
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        CommonUtil.initImageLoader(context);
        uexImageUtil = UEXImageUtil.getInstance();
        mImageAgent = ImageAgent.getInstance();
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
        String json = params[0];
        if (params.length == 2) {
            openPickerFuncId = params[1];
        }
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
            if (jsonObject.has("quality")) {
                double quality = jsonObject.getDouble("quality");
                EUEXImageConfig.getInstance().setQuality(quality);
            }
            if (jsonObject.has("usePng")) {
                Boolean usePng = jsonObject.getBoolean("usePng");
                EUEXImageConfig.getInstance().setIsUsePng(usePng);
            }
            if (jsonObject.has("detailedInfo")) {
                Boolean detailedInfo = jsonObject.getBoolean("detailedInfo");
                EUEXImageConfig.getInstance()
                        .setIsShowDetailedInfo(detailedInfo);
            }
            EUEXImageConfig.getInstance().setIsOpenBrowser(false);
            setUIConfigExtend(jsonObject);
            View albumListView = new AlbumListView(mContext, this,
                    Constants.REQUEST_IMAGE_PICKER, new ViewEvent() {

                @Override
                public void resultCallBack(int requestCode,
                                           int resultCode) {
                    callbackPickerResult(requestCode, resultCode);
                }
            });
            addViewToCurrentWindow(albumListView, AlbumListView.TAG,
                    EUEXImageConfig.getInstance().getPicGridFrame());
        } catch (JSONException e) {
            if (BDebug.DEBUG) {
                Log.i(TAG, e.getMessage());
            }
            Toast.makeText(context,
                    EUExUtil.getString("plugin_uex_image_json_format_error"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setUIConfigExtend(JSONObject jsonObject) {
        EUEXImageConfig config = EUEXImageConfig.getInstance();
        if (jsonObject.has(Constants.UI_STYLE)) {
            config.setUIStyle(jsonObject.optInt(Constants.UI_STYLE));
        }
        config.setPicPreviewFrame(
                getViewFrameVO(jsonObject, Constants.VIEW_FRAME_PIC_PREVIEW));
        config.setPicGridFrame(
                getViewFrameVO(jsonObject, Constants.VIEW_FRAME_PIC_GRID));
        if (Constants.UI_STYLE_NEW == config.getUIStyle()) {
            if (jsonObject.has(Constants.GRID_VIEW_BACKGROUND)) {
                config.setViewGridBackground(Color.parseColor(
                        jsonObject.optString(Constants.GRID_VIEW_BACKGROUND)));
            }
            if (jsonObject.has(Constants.GRID_BROWSER_TITLE)) {
                config.setGridBrowserTitle(
                        jsonObject.optString(Constants.GRID_BROWSER_TITLE));
            }
        }
    }

    public void openBrowser(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        if (params.length == 2) {
            openBrowserFuncId = params[1];
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            EUEXImageConfig config = EUEXImageConfig.getInstance();
            if (!jsonObject.has("data")) {
                Toast.makeText(context,
                        EUExUtil.getString("plugin_uex_image_data_cannot_null"),
                        Toast.LENGTH_SHORT).show();
                return;
            } else {
                JSONArray data = jsonObject.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    if (data.get(i) instanceof String) {
                        String path = data.getString(i);
                        String realPath = BUtility.makeRealPath(
                                BUtility.makeUrl(mBrwView.getCurrentUrl(),
                                        path),
                                mBrwView.getCurrentWidget().m_widgetPath,
                                mBrwView.getCurrentWidget().m_wgtType);
                        data.put(i, realPath);
                    } else {
                        JSONObject obj = data.getJSONObject(i);
                        if (!obj.has("src")) {
                            Toast.makeText(context,
                                    String.format(
                                            EUExUtil.getString(
                                                    "plugin_uex_image_data_src_cannot_null"),
                                            (i + 1)),
                                    Toast.LENGTH_SHORT).show();
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
                                    BUtility.makeUrl(mBrwView.getCurrentUrl(),
                                            thumb),
                                    mBrwView.getCurrentWidget().m_widgetPath,
                                    mBrwView.getCurrentWidget().m_wgtType);
                            obj.put("thumb", thumbPath);
                        }
                    }
                }
                config.setDataArray(data);
            }
            if (jsonObject.has("displayActionButton")) {
                boolean isDisplayActionButton = jsonObject
                        .getBoolean("displayActionButton");
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
                    Toast.makeText(context,
                            "startOnGrid为true时，enableGrid不能为false",
                            Toast.LENGTH_SHORT).show();
                }
            }
            // Android不支持
            // boolean isDisplayNavArrows =
            // jsonObject.getBoolean("displayNavArrows");

            if (jsonObject.has("startIndex")) {
                int startIndex = jsonObject.getInt("startIndex");
                if (startIndex < 0) {
                    startIndex = 0;
                }
                config.setStartIndex(startIndex);
            }
            setUIConfigExtend(jsonObject);
            config.setIsOpenBrowser(true);
            String viewTag = "";
            ViewFrameVO viewFrameVO = null;
            View imagePreviewView = null;
            if (config.isStartOnGrid()) {
                viewTag = PictureGridView.TAG;
                imagePreviewView = new PictureGridView(context, this, "",
                        Constants.REQUEST_IMAGE_BROWSER, new ViewEvent() {
                    @Override
                    public void resultCallBack(int requestCode,
                                               int resultCode) {
                        callbackPickerResult(requestCode, resultCode);
                    }
                });
                viewFrameVO = config.getPicGridFrame();
            } else {
                viewTag = ImagePreviewView.TAG;
                imagePreviewView = new ImagePreviewView(context, this, "",
                        0, Constants.REQUEST_IMAGE_BROWSER, new ViewEvent() {
                    @Override
                    public void resultCallBack(int requestCode,
                                               int resultCode) {
                        callbackPickerResult(requestCode, resultCode);
                    }
                });
                viewFrameVO = config.getPicPreviewFrame();
            }
            addViewToCurrentWindow(imagePreviewView, viewTag, viewFrameVO);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    EUExUtil.getString("plugin_uex_image_json_format_error"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private ViewFrameVO getViewFrameVO(JSONObject jsonObject, String key) {
        ViewFrameVO viewFrameVO = null;
        if (jsonObject.has(key)) {
            viewFrameVO = DataParser
                    .viewFrameVOParser(jsonObject.optString(key));
        }
        if (null == viewFrameVO) {
            viewFrameVO = UEXImageUtil.getFullScreenViewFrameVO(mContext,
                    mBrwView);
        }
        return viewFrameVO;
    }

    //打开一个可以添加Label的背景图片， 所有的Label都添加在这个图片上
    public void openLabelViewContainer(String[] params) {
        String json = params[0];
        try {
            JSONObject obj = new JSONObject(json);
            int w = obj.getInt("width");
            int h = obj.getInt("height");
            int x = obj.optInt("x", 0);
            int y = obj.optInt("y", 0);
            String imagePath = obj.getString("image");
            labelViewContainer = new RelativeLayout(mContext);
            Bitmap bitmap = ACEImageLoader.getInstance().getBitmapSync(imagePath);
            BitmapDrawable drawable = new BitmapDrawable(bitmap);
            labelViewContainer.setBackgroundDrawable(drawable);

            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(w, h);
            param.leftMargin = x;
            param.topMargin = y;
            addViewToCurrentWindow(labelViewContainer, param);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //添加label
    public void addLabelView(String[] params) {
        if (labelViewContainer == null) {
            Log.i(TAG, "please call openLabelView method first");
            return;
        }
        if (params.length < 0) {
            return;
        }
        try {
            //如果传过来的是Json对象，代表添加一个label.
            if (params[0].startsWith("{")) {
                JSONObject obj = new JSONObject(params[0]);
                addLabel(obj);
            } else {
                JSONArray array = new JSONArray(params[0]);
                int len = array.length();
                for (int i = 0; i < len; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    addLabel(obj);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addLabel(JSONObject obj) throws JSONException {
        String id = obj.getString("id");
        String content = obj.getString("content");
        if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(content)) {
            int x = obj.optInt("x", 0);
            int y = obj.optInt("y", 0);
            LabelView labelView = new LabelView(mContext, labelViewContainer, id, content);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = x;
            layoutParams.topMargin = y;
            labelViewContainer.addView(labelView, layoutParams);
        }
    }

    /**
     * 获取图片及label信息，数据格式如下:
     * {
     * "width": 600, // 外层图片（label 的载体）的宽度
     * "height": 333,  // 外层图片的高度
     * "labels": [     //所有的label
     * {
     * "id": "1",  //label id, 当用户点击图片时， 若点到了label所标识的热点，会触发onLabelClicked方法，该方法会返回id
     * "content": "content", //label内容
     * "left": "0.033", //label左侧距其外层图片左侧的距离和图片宽度的比值。
     * "right": "0.342", //label右侧距其外层图片左侧的距离和图片宽度的比值。
     * "top": "0.120", //label右侧距其外层图片顶部的距离和图片高度的比值。
     * "bottom": "0.276", //label底部距其外层图片顶部的距离和图片高度的比值。
     * "targetPointMode": 0  //label所指向的热点相对label的位置，0： 在label左侧， 1: 在label右侧
     * }
     * ]
     * }
     */
    public JSONObject getPicInfoWithLabelViews(String[] params) {
        if (labelViewContainer == null) {
            Log.i(TAG, "please call openLabelView method first");
            return null;
        }
        int childCount = labelViewContainer.getChildCount();
        int width = labelViewContainer.getWidth();
        int height = labelViewContainer.getHeight();
        JSONObject result = new JSONObject();

        try {
            result.put("width", width);
            result.put("height", height);

            JSONArray array = new JSONArray();
            for (int i = 0; i < childCount; i++) {
                LabelView view = (LabelView) labelViewContainer.getChildAt(i);
                String id = view.getLableId();
                String content = view.getContent();
                float left = view.getLeft() * 1.0f / labelViewContainer.getWidth();
                float right = view.getRight() * 1.0f / labelViewContainer.getWidth();
                float top = view.getTop() * 1.0f / labelViewContainer.getHeight();
                float bottom = view.getBottom() * 1.0f / labelViewContainer.getHeight();
                JSONObject obj = new JSONObject();
                DecimalFormat format = new DecimalFormat("##0.000");

                obj.put("id", id);
                obj.put("content", content);
                obj.put("left", format.format(left));
                obj.put("right", format.format(right));
                obj.put("top", format.format(top));
                obj.put("bottom", format.format(bottom));
                obj.put("targetPointMode", view.getTargetPointMode());
                array.put(obj);
            }
            result.put("labels", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;

    }

    public void showLabelViewPic(String params[]) {
        String json = params[0];
        try {
            JSONObject obj = new JSONObject(json);
            int w = obj.getInt("width");
            int h = obj.getInt("height");
            int x = obj.optInt("x", 0);
            int y = obj.optInt("y", 0);
            String imagePath = obj.getString("image");

            String labels = obj.getString("labels");
            List<LabelInfo> infoList = DataHelper.gson.fromJson(labels, new TypeToken<ArrayList<LabelInfo>>() {
            }.getType());
            LabelViewContainer container = new LabelViewContainer(mContext, infoList);
            Bitmap bitmap = ACEImageLoader.getInstance().getBitmapSync(imagePath);
            BitmapDrawable drawable = new BitmapDrawable(bitmap);
            container.setBackgroundDrawable(drawable);

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(w, h);
            layoutParams.leftMargin = x;
            layoutParams.rightMargin = y;
            container.setUexBaseObj(this);
            addViewToCurrentWindow(container, layoutParams);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openCropper(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        if (params.length == 2) {
            openCropperId = params[1];
        }
        OpenCropperVO openCropperVO=DataHelper.gson.fromJson(json,OpenCropperVO.class);
        if (!TextUtils.isEmpty(openCropperVO.src)){
            //开始裁剪
            cropImage(openCropperVO);
        }else{
            //选取照片后裁剪
            tempOpenCropperVO=openCropperVO;
            chooseImageInGallery();
        }

    }

    private void cropImage(OpenCropperVO cropperVO) {
        String src = cropperVO.src;
        String srcPath = BUtility.getRealPathWithCopyRes(mBrwView,src);
        if (cropperVO.quality!=0) {
            double qualityParam = cropperVO.quality;
            if (qualityParam < 0 || qualityParam > 1) {
                Toast.makeText(context,
                        EUExUtil.getString(
                                "plugin_uex_image_quality_range"),
                        Toast.LENGTH_SHORT).show();
            } else {
                cropQuality = qualityParam;
            }
        }
        cropUsePng = cropperVO.usePng;
        cropMode = cropperVO.mode;
        File file = new File(srcPath);
        updateGallery(file.getAbsolutePath());
        performCrop(file);
    }

    private void chooseImageInGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CHOOSE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        BDebug.d("requestCode",requestCode);
        if (requestCode==REQUEST_CHOOSE_IMAGE){
            if (resultCode==Activity.RESULT_OK){
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = mContext.getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                tempOpenCropperVO.src=picturePath;

                //引擎对onActivityResult 处理有点问题，此处用handler 异步处理来绕过
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        cropImage(tempOpenCropperVO);
                    }
                });
            }else{
                if (!TextUtils.isEmpty(openCropperId)) {
                    callbackToJs(Integer.parseInt(openCropperId), false, -1, "用户取消选择图片");
                }
            }
        }else if (requestCode == Constants.REQUEST_CROP) {
            cropCallBack(resultCode);
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    public void compressImage(String[] params) {
        if (params.length >= 1) {
            String imageVOStr = params[0];
            if (2 == params.length) {
                compressImageFunCbId = params[1];
            }
            CompressImageVO mCompressImageVO = DataHelper.gson
                    .fromJson(imageVOStr, CompressImageVO.class);
            mCompressImageVO.setSrcPath(BUtility
                    .makeRealPath(mCompressImageVO.getSrcPath(), mBrwView));
            if (mImageAgent != null) {
                mImageAgent.compressImage(mContext, this, mCompressImageVO);
            }
        }
    }

    private void performCrop(File imageFile) {
        try {
            String fileName = null;
            Long time = new Date().getTime();
            if (cropUsePng) {
                fileName = "crop_temp_" + time + ".png";
            } else {
                fileName = "crop_temp_" + time + ".jpg";
            }

            cropOutput = new File(UEXImageUtil.getImageCacheDir(mContext)
                    + File.separator + fileName);
            cropOutput.createNewFile();
            Uri destination = Uri.fromFile(cropOutput);
            registerActivityResult();
            Crop crop = Crop.of(Uri.fromFile(imageFile), destination,
                    cropQuality, cropUsePng);
            if (cropMode < 4) {
                crop.asSquare();
            } else if (cropMode == 4) {
                crop.withAspect(4, 3);
            } else if (cropMode == 5) {
                crop.withAspect(16, 9);
            }
            crop.start((Activity) mContext);
        } catch (Exception exception) {
            Toast.makeText(context,
                    EUExUtil.getString("plugin_uex_image_not_support_crop"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGallery(String filename) {
        MediaScannerConnection.scanFile(context, new String[]{filename},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        if (BDebug.DEBUG) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    }
                });
    }

    public void addViewToCurrentWindow(View view, String tag,
                                       ViewFrameVO viewFrameVO) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                viewFrameVO.width, viewFrameVO.height);
        lp.leftMargin = viewFrameVO.x;
        lp.topMargin = viewFrameVO.y;
        if (addToWebViewsMap.get(tag) != null) {
            removeViewFromCurWindow(tag);
        }
        addViewToCurrentWindow(view, lp);
        addToWebViewsMap.put(tag, view);
    }

    public void removeViewFromCurWindow(String viewTag) {
        View removeView = addToWebViewsMap.get(viewTag);
        if (removeView != null) {
            removeViewFromCurrentWindow(removeView);
            removeView.destroyDrawingCache();
            addToWebViewsMap.remove(viewTag);
        }
    }

    public static void onActivityResume(Context context) {
        Set<String> tagList = addToWebViewsMap.keySet();
        for (String tag : tagList) {
            if (!TextUtils.isEmpty(tag)) {
                ((ImageBaseView) addToWebViewsMap.get(tag)).onResume();
            }
        }
    }

    private void callbackPickerResult(int requestCode, int resultCode) {
        switch (requestCode) {
            case Constants.REQUEST_CROP:
                cropCallBack(resultCode);
                break;
            case Constants.REQUEST_IMAGE_PICKER:
                JSONObject jsonObject = null;
                int errorCode = 0;
                if (Constants.OPERATION_CONFIRMED == resultCode) {
                    jsonObject = uexImageUtil.getChoosedPicInfo(context);
                    errorCode = 0;
                } else if (Constants.OPERATION_CANCELLED == resultCode) {
                    jsonObject = new JSONObject();
                    try {
                        jsonObject.put("isCancelled", true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    errorCode = -1;
                }
                callBackPluginJs(JsConst.CALLBACK_ON_PICKER_CLOSED,
                        jsonObject.toString());
                if (openPickerFuncId != null) {
                    callbackToJs(Integer.parseInt(openPickerFuncId), false,
                            errorCode, jsonObject);
                }
                uexImageUtil.resetData();
                break;
            case Constants.REQUEST_IMAGE_BROWSER:
                callBackPluginJs(JsConst.CALLBACK_ON_BROWSER_CLOSED,
                        "pic browser closed");
                if (openBrowserFuncId != null) {
                    callbackToJs(Integer.parseInt(openBrowserFuncId), false);
                }
                break;
            default:
                break;
        }
    }

    private void cropCallBack(int resultCode) {
        // 如果是用户取消，则删除这个临时文件
        if (cropOutput.length() == 0) {
            cropOutput.delete();
        }
        updateGallery(cropOutput.getAbsolutePath());
        int error = 0;
        JSONObject result = new JSONObject();
        try {
            switch (Crop.cropStatus) {
                case 1:
                    error = 0;
                    result.put("isCancelled", false);
                    result.put("data", cropOutput.getAbsolutePath());
                    break;
                case 2:
                    error = 1;
                    result.put("isCancelled", false);
                    result.put("data",
                            EUExUtil.getString("plugin_uex_image_system_error"));
                    break;
                case 3:
                    error = -1;
                    result.put("isCancelled", true);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callBackPluginJs(JsConst.CALLBACK_ON_CROPPER_CLOSED, result.toString());
        if (null != openCropperId) {
            callbackToJs(Integer.parseInt(openCropperId), false, error, result);
        }
    }

    public void saveToPhotoAlbum(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        String funcId = null;
        if (params.length == 2) {
            funcId = params[1];
        }
        // 回调的结果
        JSONObject resultObject = new JSONObject();
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.has("localPath")
                    || TextUtils.isEmpty(jsonObject.getString("localPath"))) {
                Toast.makeText(context,
                        EUExUtil.getString(
                                "plugin_uex_image_localPath_cannot_null"),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (jsonObject.has("extraInfo")) {
                resultObject.put("extraInfo",
                        jsonObject.getString("extraInfo"));
            }
            String path = jsonObject.getString("localPath");
            String realPath = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), path),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            // 如果传的是res,则会复制一份到相册
            if (path.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
                // 获取文件名
                String fileName = path.replace(BUtility.F_Widget_RES_SCHEMA,
                        "");
                fileName = System.currentTimeMillis() + fileName;//文件名加时间戳前缀，防止重名文件无法加入相册
                String dcimPath = Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DCIM
                        + File.separator;
                File file = new File(dcimPath, fileName);
                if (file.exists()) {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", EUExUtil
                            .getString("plugin_uex_image_same_name_file"));
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                            resultObject.toString());
                    if (null != funcId) {
                        callbackToJs(Integer.parseInt(funcId), false, 1,
                                EUExUtil.getString(
                                        "plugin_uex_image_same_name_file"));
                    }
                    return;
                }
                file.createNewFile();
                if (realPath.startsWith("/data")) {
                    CommonUtil.copyFile(new File(realPath), file);
                    resultObject.put("isSuccess", true);
                    updateGallery(file.getAbsolutePath());
                } else if (CommonUtil.saveFileFromAssetsToSystem(context,
                        realPath, file)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(file.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", EUExUtil
                            .getString("plugin_uex_image_system_error"));
                }
            } else {// 如果傳的是別的路徑，也復制一份吧。
                File fromFile = new File(realPath);
                String fileName = fromFile.getName();
                fileName = System.currentTimeMillis() + fileName;//文件名加时间戳前缀，防止重名文件无法加入相册

                String dcimPath = Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DCIM
                        + File.separator;
                File destFile = new File(dcimPath, fileName);
                if (destFile.exists()) {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", EUExUtil
                            .getString("plugin_uex_image_same_name_file"));
                    callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                            resultObject.toString());
                    if (null != funcId) {
                        callbackToJs(Integer.parseInt(funcId), false, 1,
                                EUExUtil.getString(
                                        "plugin_uex_image_same_name_file"));
                    }
                    return;
                }

                if (CommonUtil.copyFile(new File(realPath), destFile)) {
                    resultObject.put("isSuccess", true);
                    updateGallery(destFile.getAbsolutePath());
                } else {
                    resultObject.put("isSuccess", false);
                    resultObject.put("errorStr", EUExUtil
                            .getString("plugin_uex_image_system_error"));
                }
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
            if (null != funcId) {
                callbackToJs(Integer.parseInt(funcId), false, 0,
                        EUExUtil.getString("plugin_uex_image_system_error"));
            }
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr", EUExUtil
                        .getString("plugin_uex_image_json_format_error"));
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
            if (null != funcId) {
                callbackToJs(Integer.parseInt(funcId), false, 1, EUExUtil
                        .getString("plugin_uex_image_json_format_error"));
            }
        } catch (IOException e) {
            Log.i(TAG, e.getMessage());
            try {
                resultObject.put("isSuccess", false);
                resultObject.put("errorStr",
                        EUExUtil.getString("plugin_uex_image_system_error"));
            } catch (JSONException e2) {
                Log.i(TAG, e2.getMessage());
            }
            callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM,
                    resultObject.toString());
            if (null != funcId) {
                callbackToJs(Integer.parseInt(funcId), false, 1,
                        EUExUtil.getString("plugin_uex_image_system_error"));
            }
        }
    }

    public void clearOutputImages(String[] params) {
        JSONObject jsonResult = new JSONObject();
        if (mImageAgent != null) {
            mImageAgent.clearOutputImages(mContext);
        }
        try {
            jsonResult.put(Constants.JK_STATUSE, Constants.JK_OK);
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
        callBackPluginJs(JsConst.CALLBACK_CLEAR_OUTPUT_IMAGES,
                jsonResult.toString());
    }

    public void onImageLongClick(String cbVO) {
        callBackPluginJs(JsConst.CALLBACK_ON_IAMGE_LONG_CLICKED, cbVO);
    }

    public void cbCompressImage(JSONObject cbJson, int errorCode) {
        callBackPluginJs(JsConst.CALLBACK_COMPRESS_IAMGE, cbJson.toString());
        if (!TextUtils.isEmpty(compressImageFunCbId)) {
            callbackToJs(Integer.parseInt(compressImageFunCbId), false,
                    errorCode, cbJson.toString());
        }
    }

    public void callBackPluginJs(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){" + methodName
                + "('" + jsonData + "');}";
        onCallback(js);
    }

}
