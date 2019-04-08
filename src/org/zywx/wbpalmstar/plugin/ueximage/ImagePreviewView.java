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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.assist.FailReason;
import com.ace.universalimageloader.core.assist.ImageScaleType;
import com.ace.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.ace.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ImageLongClickCBVO;
import org.zywx.wbpalmstar.plugin.ueximage.widget.CirclePgBar;
import org.zywx.wbpalmstar.plugin.ueximage.widget.PhotoView;

import java.io.File;
import java.util.List;

public class ImagePreviewView extends ImageBaseView {

    public final static String TAG = "ImagePreviewView";
    private ViewPager viewPager;
    private ImageView ivGoBack;
    private TextView tvTitle;
    private Button btnFinishInTitle;
    private CheckBox cbChoose;
    private TextView tvCheckbox;
    private List<String> checkedItems;
    private UEXImageUtil uexImageUtil;
    // private ImageView imageView;
    private List<PictureInfo> picList;
    private int picIndex;
    private boolean isOpenBrowser;
    // 仅在浏览图片时有用。
    private TextView tvShare;
    private TextView tvToGrid;
    /** * 切换到Grid浏览模式 */
    private static ImageView ivToGrid;
    private RelativeLayout rlTitle;
    private RelativeLayout rlBottom;
    private AlphaAnimation fadeInAnim;
    private AlphaAnimation fadeOutAnim;
    private ImageBaseView mImagePreviewActivity = null;

    private CirclePgBar mProgressView;

    /** *单张浏览模式下，3s没有任何操作，隐藏切换到Grid浏览模式的ImageView */
    private static Handler hideIvToGridHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Constants.WHAT_HIDE_IV_TO_GRID:
                if (ivToGrid != null) {
                    ivToGrid.setVisibility(View.INVISIBLE);
                }
                break;
            case Constants.WHAT_SHOW_IV_TO_GRID:
                if (ivToGrid != null) {
                    ivToGrid.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    };

    private OnClickListener toGridClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mRequestCode == Constants.REQUEST_IMAGE_BROWSER_FROM_GRID) {
                finish(TAG, Constants.OPERATION_CANCELLED);
            } else {
                startPictureGridView(mContext, mEUExImage, "",
                        Constants.REQUEST_IMAGE_BROWSER);
            }
        }
    };

    private PagerAdapter adapter = new PagerAdapter() {

        @Override
        public int getCount() {
            return picList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {

            final PhotoView imageView = new PhotoView(mContext);
            ViewPager.LayoutParams layoutParams = new ViewPager.LayoutParams();
            layoutParams.height = container.getMeasuredHeight();
            layoutParams.width = container.getMeasuredWidth();
            imageView.setLayoutParams(layoutParams);
            imageView.enable();
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // 显示图片的配置
            DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder()
                    .considerExifParams(true);// 考虑Exif旋转
            //显示大图
            if (picList.get(position).isShowBigPic()) {
                builder.imageScaleType(ImageScaleType.NONE);
            } else { // 显示处理后的图片，占内存小
                builder.cacheInMemory(true);
                builder.imageScaleType(ImageScaleType.EXACTLY);
                builder.bitmapConfig(Bitmap.Config.RGB_565);
            }
            builder.cacheOnDisk(true);
            DisplayImageOptions options = builder.build();
            final String src = picList.get(position).getSrc();
            if (!isOpenBrowser) {
                ACEImageLoader.getInstance().displayImageWithOptions(src,
                        imageView, true);
            } else {// 浏览图片：对于传入的图片的加载
                if (src.substring(0, 4).equalsIgnoreCase(Constants.HTTP)) {
                    // 如果是从网上下载图片，需要将下载后的图片存到缓存中
                    ACEImageLoader.getInstance();
                    ImageLoader.getInstance().displayImage(src, imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            super.onLoadingComplete(imageUri, view, loadedImage);
                            picList.get(position).progress=-1;
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            super.onLoadingCancelled(imageUri, view);
                            picList.get(position).progress=-1;
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            super.onLoadingFailed(imageUri, view, failReason);
                            picList.get(position).progress=-1;
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String s, View view, int current, int total) {
                            int progress=current*100/total;
                            picList.get(position).progress=progress;
                            BDebug.d(progress);
                            if (viewPager.getCurrentItem()==position&&progress!=100) {
                                showProgress(progress);
                            }else{
                                cancelProgress();
                            }
                        }
                    });
                       ACEImageLoader.getInstance().displayImageWithOptions(src,
                            imageView, true);
                } else {
                    Bitmap bitmap = CommonUtil
                            .getLocalImage(mContext, src);
                    imageView.setImageBitmap(bitmap);
                }
            }
             imageView.setOnClickListener(imageClickListener);
            imageView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    ImageLongClickCBVO cbVO = new ImageLongClickCBVO();
                    cbVO.setImagePath(src);
                    mEUExImage.onImageLongClick(DataHelper.gson.toJson(cbVO));
                    return false;
                }
            });
            container.addView(imageView);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position,
                Object object) {
            container.removeView((View) object);
        }
    };

    private OnClickListener imageClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (EUEXImageConfig.getInstance().getUIStyle()) {
            case Constants.UI_STYLE_OLD:
                toogleView();
                break;
            case Constants.UI_STYLE_NEW:
                if (hideIvToGridHandler
                        .hasMessages(Constants.WHAT_SHOW_IV_TO_GRID)) {
                    hideIvToGridHandler
                            .removeMessages(Constants.WHAT_SHOW_IV_TO_GRID);
                }
                finish(TAG, Constants.OPERATION_CANCELLED);
                break;
            default:
                break;
            }
        }
    };

    private void showProgress(int progress){
        if (progress==-1){
            cancelProgress();
            return;
        }
        if (mProgressView==null){
            mProgressView=new CirclePgBar(mContext);
            RelativeLayout.LayoutParams layoutParams=new RelativeLayout.LayoutParams(EUExUtil.dipToPixels(60), EUExUtil
                    .dipToPixels(60));
            layoutParams.addRule(CENTER_IN_PARENT,TRUE);
            addView(mProgressView,layoutParams);
        }
        mProgressView.setProgress(progress);
    }

    private void cancelProgress(){
        if (mProgressView!=null){
            removeView(mProgressView);
            mProgressView=null;
        }
    }

    /**
     * @param context
     * @param mEUExImage
     * @param folderName
     *            isOpenBrowser为false时，才使用此值
     * @param picIndex
     *            isOpenBrowser为false时，才使用此值
     * @param requestCode
     *            this code will be returned in finish() when the view exits.
     */
    public ImagePreviewView(Context context, EUExImage mEUExImage,
                            String folderName, int picIndex, int requestCode,
                            ViewEvent viewEvent) {
        super(context, mEUExImage, requestCode, viewEvent, TAG);
        mContext = context;
        onCreate(context, folderName, picIndex);
        mImagePreviewActivity = this;
    }

    private void onCreate(Context context, String folderName, int picIndex) {
        LayoutInflater.from(context)
                .inflate(
                        EUExUtil.getResLayoutID(
                                "plugin_uex_image_activity_image_preview"),
                        this, true);
        uexImageUtil = UEXImageUtil.getInstance();
        isOpenBrowser = EUEXImageConfig.getInstance().getIsOpenBrowser();
        rlTitle = (RelativeLayout) findViewById(
                EUExUtil.getResIdID("title_layout"));
        ivGoBack = (ImageView) findViewById(
                EUExUtil.getResIdID("iv_left_on_title"));
        tvTitle = (TextView) findViewById(EUExUtil.getResIdID("tv_title"));
        ivToGrid = (ImageView) findViewById(EUExUtil.getResIdID("iv_to_grid"));
        btnFinishInTitle = (Button) findViewById(
                EUExUtil.getResIdID("btn_finish_title"));
        viewPager = (ViewPager) findViewById(EUExUtil.getResIdID("vp_picture"));
        cbChoose = (CheckBox) findViewById(EUExUtil.getResIdID("checkbox"));
        rlBottom = (RelativeLayout) findViewById(
                EUExUtil.getResIdID("rl_bottom"));
        rlTitle.setAlpha(0.9f);
        rlBottom.setAlpha(0.9f);

        initData(folderName, picIndex);
        if (isOpenBrowser) {
            initViewForBrowser(context);
        } else {
            initViewForPicker(context);
        }
        initAnimation();
    }

    private void initData(String folder, int index) {
        if (isOpenBrowser) {
            JSONArray imageDataArray = EUEXImageConfig.getInstance()
                    .getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
            picIndex = EUEXImageConfig.getInstance().getStartIndex();
        } else {
            picIndex = index;
            checkedItems = uexImageUtil.getCheckedItems();
            picList = uexImageUtil.getCurrentPicList();
        }
    }

    private void setImageTitle() {
        tvTitle.setText((picIndex + 1) + "/" + picList.size());
    }

    private void initViewForPicker(final Context context) {
        setImageTitle();
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(TAG, Constants.OPERATION_CANCELLED);
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (checkedItems.size() > 0) {
            btnFinishInTitle
                    .setText(EUExUtil.getString("plugin_uex_image_crop_done")
                            + "(" + checkedItems.size() + "/"
                            + EUEXImageConfig.getInstance().getMaxImageCount()
                            + ")");
            btnFinishInTitle.setEnabled(true);
        }
        PictureInfo pictureInfo = picList.get(picIndex);
        cbChoose.setTag(pictureInfo.getSrc());
        cbChoose.setChecked(checkedItems.contains(pictureInfo.getSrc()));
        cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkedItems.size() >= EUEXImageConfig.getInstance()
                        .getMinImageCount()) {
                    finish(TAG, Constants.OPERATION_CONFIRMED);
                } else {
                    String str = String.format(
                            EUExUtil.getString(
                                    "plugin_uex_image_at_least_choose"),
                            EUEXImageConfig.getInstance().getMinImageCount());
                    Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if ((Constants.UI_STYLE_NEW == EUEXImageConfig.getInstance()
                    .getUIStyle()) && (ivToGrid != null)) {
                showIvToGridDelayed();
                if (hideIvToGridHandler
                        .hasMessages(Constants.WHAT_HIDE_IV_TO_GRID)) {
                    hideIvToGridHandler
                            .removeMessages(Constants.WHAT_HIDE_IV_TO_GRID);
                }
                hideIvToGridDelayed();
            }
            break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void startPictureGridView(Context context, EUExImage euExImage,
            String filePath, int requestCode) {
        // 由起始的单图浏览进入多图浏览，没有pick的情况
        View imagePreviewView = new PictureGridView(context, euExImage, "",
                requestCode, new ViewEvent() {
                    @Override
                    public void resultCallBack(int requestCode,
                            int resultCode) {
                        mImagePreviewActivity.requestViewFocus();
                    }
                });
        euExImage.addViewToCurrentWindow(imagePreviewView, PictureGridView.TAG,
                EUEXImageConfig.getInstance().getPicGridFrame());
    }

    private void initViewForBrowser(final Context context) {

        ivGoBack.setVisibility(View.INVISIBLE);
        tvCheckbox = (TextView) findViewById(
                EUExUtil.getResIdID("tv_checkbox"));
        cbChoose.setVisibility(View.INVISIBLE);
        tvCheckbox.setVisibility(View.INVISIBLE);

        switch (EUEXImageConfig.getInstance().getUIStyle()) {
        case Constants.UI_STYLE_OLD:
            tvShare = (TextView) findViewById(EUExUtil.getResIdID("tv_share"));
            tvToGrid = (TextView) findViewById(
                    EUExUtil.getResIdID("tv_to_grid"));
            tvToGrid.setVisibility(View.VISIBLE);
            tvToGrid.setOnClickListener(toGridClickListener);

            btnFinishInTitle.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish(TAG, Constants.OPERATION_CONFIRMED);
                }
            });
            if (EUEXImageConfig.getInstance().isDisplayActionButton()) {
                tvShare.setVisibility(View.VISIBLE);
                tvShare.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        final String src = picList.get(picIndex).getSrc();
                        Bitmap bitmap;
                        if (src.substring(0, 4)
                                .equalsIgnoreCase(Constants.HTTP)) {
                            bitmap = ACEImageLoader.getInstance()
                                    .getBitmapSync(src);
                        } else {
                            bitmap = CommonUtil.getLocalImage(context, src);
                        }
                        File file = new File(
                                UEXImageUtil.getImageCacheDir(context)
                                        + File.separator
                                        + "uex_image_to_share.jpg");
                        if (bitmap == null) {
                            Toast.makeText(context,
                                    EUExUtil.getString(
                                            "plugin_uex_image_retry_load_image"),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (CommonUtil.saveBitmap2File(bitmap, file)) {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM,
                                    Uri.fromFile(file));
                            shareIntent.setType("image/*");
                        } else {
                            Toast.makeText(context,
                                    EUExUtil.getString(
                                            "plugin_uex_image_image_error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                if (!EUEXImageConfig.getInstance().isEnableGrid()) {
                    ((View) tvShare.getParent()).setVisibility(View.INVISIBLE);
                }
            }
            break;
        case Constants.UI_STYLE_NEW:
            rlTitle.setVisibility(View.GONE);
            rlBottom.setVisibility(View.GONE);
            ivToGrid.setVisibility(View.VISIBLE);
            ivToGrid.setOnClickListener(toGridClickListener);
            hideIvToGridDelayed();
            break;
        default:
            BDebug.e(TAG, "EUExImage UIStyle is error.");
            break;
        }
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
    }

    private void showIvToGridDelayed() {
        hideIvToGridHandler.sendEmptyMessageDelayed(
                Constants.WHAT_SHOW_IV_TO_GRID,
                Constants.SHOW_IV_TO_GRID_TIMEOUT);
    }

    private void hideIvToGridDelayed() {
        hideIvToGridHandler.sendEmptyMessageDelayed(
                Constants.WHAT_HIDE_IV_TO_GRID,
                Constants.HIDE_IV_TO_GRID_TIMEOUT);
    }

    @Override
    protected void onResume() {
        if (!isOpenBrowser) {
            cbChoose.setChecked(
                    checkedItems.contains(picList.get(picIndex).getSrc()));
        }
        setImageTitle();
    }

    private void toogleView() {
        if (rlTitle.getVisibility() == View.VISIBLE) {
            rlTitle.setVisibility(View.INVISIBLE);
            rlTitle.startAnimation(fadeOutAnim);
            rlBottom.setVisibility(View.INVISIBLE);
            rlBottom.startAnimation(fadeOutAnim);
        } else {
            rlTitle.setVisibility(View.VISIBLE);
            rlTitle.startAnimation(fadeInAnim);
            rlBottom.setVisibility(View.VISIBLE);
            rlBottom.startAnimation(fadeInAnim);
        }
    }

    private String getRealImageUrl(String imgUrl) {
        String realImgUrl = null;
        if (imgUrl.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
            String assetFileName = BUtility.F_Widget_RES_path
                    + imgUrl.substring(BUtility.F_Widget_RES_SCHEMA.length());
            realImgUrl = "assets://" + assetFileName;
        } else if (imgUrl.startsWith(BUtility.F_FILE_SCHEMA)) {
            realImgUrl = imgUrl;
        } else if (imgUrl.startsWith(BUtility.F_Widget_RES_path)) {
            realImgUrl = "assets://" + imgUrl;
        } else if (imgUrl.startsWith("/")) {
            realImgUrl = BUtility.F_FILE_SCHEMA + imgUrl;
        } else {
            realImgUrl = imgUrl;
        }
        return realImgUrl;
    }

    private void initAnimation() {
        final int duration = 300;
        LinearInterpolator interpolator = new LinearInterpolator();
        fadeInAnim = new AlphaAnimation(0, 1);
        fadeInAnim.setDuration(duration);
        fadeInAnim.setInterpolator(interpolator);

        fadeOutAnim = new AlphaAnimation(1, 0);
        fadeOutAnim.setDuration(duration);
        fadeOutAnim.setInterpolator(interpolator);
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            picIndex = i;
            if (!isOpenBrowser) {
                cbChoose.setChecked(
                        checkedItems.contains(picList.get(i).getSrc()));
            }
            setImageTitle();
            showProgress(picList.get(i).progress);
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (!isOpenBrowser) {
                if (i == 1) {// 开始滑动
                    cbChoose.setOnCheckedChangeListener(null);
                } else if (i == 0) {// 静止
                    cbChoose.setOnCheckedChangeListener(
                            onCheckedChangeListener);
                    cbChoose.setTag(picList.get(picIndex).getSrc());
                }
            }
        }
    };

    // 仅当在选择图片时才会用得上
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (isChecked) {
                if (!checkedItems.contains(buttonView.getTag())) {
                    if (checkedItems.size() >= EUEXImageConfig.getInstance()
                            .getMaxImageCount()) {
                        Toast.makeText(mContext,
                                String.format(
                                        EUExUtil.getString(
                                                "plugin_uex_image_at_most_choose"),
                                        EUEXImageConfig.getInstance()
                                                .getMaxImageCount()),
                                Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    checkedItems.add((String) (buttonView.getTag()));
                }
            } else {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
                }
            }
            if (checkedItems.size() > 0) {
                btnFinishInTitle.setText(
                        EUExUtil.getString("plugin_uex_image_crop_done") + "("
                                + checkedItems.size() + "/" + EUEXImageConfig
                                        .getInstance().getMaxImageCount()
                                + ")");
                btnFinishInTitle.setEnabled(true);
            } else {
                btnFinishInTitle.setText(
                        EUExUtil.getString("plugin_uex_image_crop_done"));
                btnFinishInTitle.setEnabled(false);
            }
        }
    };
}
