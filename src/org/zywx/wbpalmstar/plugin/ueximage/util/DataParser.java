package org.zywx.wbpalmstar.plugin.ueximage.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ViewFrameVO;

public class DataParser {

    public static ViewFrameVO viewFrameVOParser(String viewFrame) {
        ViewFrameVO viewFrameVO = new ViewFrameVO();
        JSONObject json = null;
        try {
            json = new JSONObject(viewFrame);
            viewFrameVO.x = (int) Float
                    .parseFloat(json.optString(Constants.VIEW_FRAME_VO_X));
            viewFrameVO.y = (int) Float
                    .parseFloat(json.optString(Constants.VIEW_FRAME_VO_Y));
            viewFrameVO.width = (int) Float
                    .parseFloat(json.optString(Constants.VIEW_FRAME_VO_W));
            viewFrameVO.height = (int) Float
                    .parseFloat(json.optString(Constants.VIEW_FRAME_VO_H));
        } catch (JSONException e) {
            viewFrameVO = null;
            e.printStackTrace();
        }
        return viewFrameVO;
    }

}
