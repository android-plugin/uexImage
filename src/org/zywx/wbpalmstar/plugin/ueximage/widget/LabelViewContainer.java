package org.zywx.wbpalmstar.plugin.ueximage.widget;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.EUExImage;
import org.zywx.wbpalmstar.plugin.ueximage.JsConst;
import org.zywx.wbpalmstar.plugin.ueximage.model.LabelInfo;

import java.util.List;

/**
 * Created by fred on 16/12/1.
 */
public class LabelViewContainer extends RelativeLayout{
    private List<LabelInfo> labels;
    private EUExImage uexBaseObj;
    public LabelViewContainer(Context context, List<LabelInfo> labels) {
        super(context);
        this.labels = labels;
    }
    public void setUexBaseObj(EUExImage uexBaseObj) {
        this.uexBaseObj = uexBaseObj;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                handleClick(event.getX(), event.getY());
                break;
        }
        return true;
    }

    //x, y都是相对坐标
    public void handleClick(float x, float y) {

        for (LabelInfo info: labels) {
            int offset = EUExUtil.dipToPixels(20);
            float centerX;
            //如果箭头在左边
            if (info.getTargetPointMode() == 0) {
                //将labelView左边的border中点视为热点区域的中心
                centerX = info.getLeft() * getWidth();
            } else {
                //将labelView右边的border中点视为热点区域的中心
                centerX = info.getRight() * getWidth();
            }
            float centerY = (info.getTop() + (info.getBottom()- info.getTop()) / 2)* getHeight();
            //扩大点击区域
            float left = centerX - offset;
            float right = centerX + offset;
            float top = centerY - offset;
            float bottom = centerY + offset;
//
//
            //以标签的区域作为点击热点
//            float left = info.getLeft() * getWidth();
//            float right = info.getRight() * getWidth();
//            float top = info.getTop() * getHeight();
//            float bottom = info.getBottom() * getHeight();
//            System.out.println("left:" + left + "   right:" + right + "    top:" + top +"   bottom:" + bottom);
            if (x > left && x < right && y > top && y < bottom) {
                System.out.println("label:" + info.getId() + "   clicked");
                uexBaseObj.callBackPluginJs(JsConst.ON_LABEL_CLICKED, info.getId());
                break;
            }



        }

    }

}

