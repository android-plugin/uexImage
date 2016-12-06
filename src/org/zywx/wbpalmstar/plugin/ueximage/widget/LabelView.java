package org.zywx.wbpalmstar.plugin.ueximage.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;

/**
 * Created by fred on 16/12/1.
 */
public class LabelView extends LinearLayout {
    private String content;
    private int mLastX;
    private int mLastY;
    private RelativeLayout wraperView;
    private int wraperViewLeft;
    private int wraperViewTop;
    private int wraperViewRight;
    private int wraperViewBottom;
    private int width;
    private int height;
    private String id;
    private int targetPointMode;
    private LabelView self;
    public LabelView(Context context) {
        super(context);
    }

    public LabelView(Context context, RelativeLayout wraperView, String id, String content) {
        super(context);
        this.wraperView = wraperView;
        this.content = content;
        this.id = id;
        initView(context);
    }
    public LabelView(Context context, RelativeLayout wraperView, String id, String content, int targetPointMode) {
        this(context, wraperView, id, content);
        this.targetPointMode = targetPointMode;
    }

    public LabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }
    public String getContent() {
        return this.content;
    }
    public String getLableId() {
        return this.id;
    }
    public int getTargetPointMode() {return this.targetPointMode;}

    private ImageView leftDelete;
    private ImageView rightDelete;
    private View leftDivider;
    private View rightDivider;
    private TextView tvContent;
    private void initView(Context context) {

        View view = LayoutInflater.from(context).inflate(EUExUtil.getResLayoutID("plugin_uex_image_labelview"), this);
        tvContent = (TextView) view.findViewById(EUExUtil.getResIdID("tv_uexImage_labelView_content"));
        leftDelete = (ImageView) view.findViewById(EUExUtil.getResIdID("iv_uexImage_labelView_delete_left"));
        rightDelete = (ImageView) view.findViewById(EUExUtil.getResIdID("iv_uexImage_labelView_delete_right"));
        leftDivider =  view.findViewById(EUExUtil.getResIdID("uexImage_labelView_left_divider"));
        rightDivider = view.findViewById(EUExUtil.getResIdID("uexImage_labelView_right_divider"));

        self = this;
        leftDelete.setOnClickListener(deleteListener);
        rightDelete.setOnClickListener(deleteListener);
        tvContent.setText(content);
        System.out.println("wraperview height:" + wraperView.getLeft() + "   " + wraperView.getRight() + "   " + wraperView.getTop() + "   " + wraperView.getBottom());

    }
    private View.OnClickListener deleteListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            self.setVisibility(View.GONE);

        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor((Color.BLACK));
    }

    //采用相对坐标
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        width = getWidth();
        height = getHeight();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int offsetX = x - mLastX;
                int offsetY = y - mLastY;
                int left = getLeft() + offsetX;
                int top = getTop() + offsetY;
                int right = getRight() + offsetX;
                int bottom = getBottom() + offsetY;
                //左右可以出边界
//                if (left < 0) {
//                    left  = 0;
//                    right = left + width;
//                }
//                if (right > wraperView.getWidth()) {
//                    right = wraperView.getWidth();
//                    left = right - width;
//                }
                if (top < 0) {
                    top = 0;
                    bottom = top + height;
                }
                if (bottom > wraperView.getHeight()) {
                    bottom = wraperView.getHeight();
                    top = bottom - height;
                }
                System.out.println("left:" + left + "    right:" + right);
                layout(left, top, right, bottom);
                break;
            case MotionEvent.ACTION_UP:
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
                params.topMargin = getTop();
                params.leftMargin = getLeft();

                if (getLeft() < 10) {
                    //靠近左侧，但此时箭头在右边, 需要显示右侧的删除按钮
                    if (targetPointMode != 0) {
                        targetPointMode = 0;
                        leftDelete.setVisibility(View.GONE);
                        leftDivider.setVisibility(View.GONE);
                        rightDelete.setVisibility(View.VISIBLE);
                        rightDivider.setVisibility(View.VISIBLE);
                        tvContent.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uex_image_tag_right"));
                        params.leftMargin = getLeft() + getWidth();
//
//                        if (params.leftMargin > wraperView.getWidth() - getWidth()) {
//                            params.leftMargin = wraperView.getWidth() - getWidth();
//                        }
                    }
                }
                if (getRight() > wraperView.getWidth() - 10){
                    //靠近右侧，但此时箭头在左边
                    if (targetPointMode != 1) {
                        targetPointMode = 1;
                        leftDelete.setVisibility(View.VISIBLE);
                        leftDivider.setVisibility(View.VISIBLE);
                        rightDelete.setVisibility(View.GONE);
                        rightDivider.setVisibility(View.GONE);
                        tvContent.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uex_image_tag_left"));
//                        if (getLeft() - getWidth() < 0) {
//                            params.leftMargin = 0;
//                        } else {
//                            params.leftMargin = getLeft() - getWidth();
//                        }
                        params.leftMargin = getLeft() - getWidth();
                    }
                }
                System.out.println("up left:" + getLeft() + "    right:" + getRight() + "    width:" + (getRight() - getLeft()) );
                setLayoutParams(params);
                break;
            default:
                break;
        }
        return true;
    }

}

