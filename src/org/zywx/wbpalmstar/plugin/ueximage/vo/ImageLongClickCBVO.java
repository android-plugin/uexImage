package org.zywx.wbpalmstar.plugin.ueximage.vo;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;

public class ImageLongClickCBVO {
    private String imagePath;

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String toStr() {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LONG_CLICK_CB_IMAGE_PATH, imagePath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }
}
