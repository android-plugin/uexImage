package org.zywx.wbpalmstar.plugin.ueximage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;

public class EUExImage extends EUExBase {

    private static final String BUNDLE_DATA = "data";
    private static final int MSG_OPEN_PICKER = 1;
    private static final int MSG_OPEN_BROWSER = 2;
    private static final int MSG_OPEN_CROPPER = 3;
    private static final int MSG_SAVE_TO_PHOTO_ALBUM = 4;
    private static final int MSG_CLEAR_OUTPUT_IMAGES = 5;

    public EUExImage(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
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
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("", "");
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_OPEN_PICKER, jsonResult.toString());
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
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("", "");
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_OPEN_BROWSER, jsonResult.toString());
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
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("", "");
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_OPEN_CROPPER, jsonResult.toString());
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
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("", "");
        } catch (JSONException e) {
        }
        callBackPluginJs(JsConst.CALLBACK_SAVE_TO_PHOTO_ALBUM, jsonResult.toString());
    }

    public void clearOutputImages(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_CLEAR_OUTPUT_IMAGES;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void clearOutputImagesMsg(String[] params) {
        String json = params[0];
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("", "");
        } catch (JSONException e) {
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
