package org.zywx.wbpalmstar.plugin.ueximage;

import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class ImageBaseView extends RelativeLayout {
    protected Context mContext;
    protected EUExImage mEUExImage;
    protected int mRequestCode;
    protected ViewEvent mViewEvent;
    private String TAG = "";

    public ImageBaseView(Context context, EUExImage eUExImage,
            int requestCode, ViewEvent viewEvent, String tag) {
        super(context);
        mContext = context;
        mEUExImage = eUExImage;
        mRequestCode = requestCode;
        mViewEvent = viewEvent;
        TAG = tag;
        requestViewFocus();
    }

    protected void requestViewFocus() {
        if (isInTouchMode()) {
            setFocusableInTouchMode(true);
            requestFocusFromTouch();
        } else {
            setFocusable(true);
            requestFocus();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            finish(TAG, Constants.OPERATION_CANCELLED);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onResume() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * @param resultCode
     *            原来只有Activity.RESULT_OK,如不通过Activity.setResult设置resultCode，
     *            resultCode默认为Activity.RESULT_CANCELED，故原来未setResult的，
     *            添加默认值Activity.RESULT_CANCELED。
     */
    protected void finish(String viewTag, int resultCode) {
        if (mEUExImage != null) {
            mEUExImage.removeViewFromCurWindow(viewTag);
        }
        if (mViewEvent != null) {
            mViewEvent.resultCallBack(mRequestCode, resultCode);
        }
    }

    public static interface ViewEvent {
        /**
         * 界面关闭回调
         * 
         * @param requestCode
         *            标识这次界面的类型是选择还是浏览
         * 
         * @param resultCode
         *            标识这次关闭是否是完成操作（否，则是返回操作）
         */
        public void resultCallBack(int requestCode, int resultCode);
    };

}
