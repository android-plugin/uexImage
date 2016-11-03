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
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.assist.FailReason;
import com.ace.universalimageloader.core.listener.ImageLoadingListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;
import org.zywx.wbpalmstar.plugin.ueximage.widget.PhotoView;
import org.zywx.wbpalmstar.plugin.ueximage.widget.RowView;
import org.zywx.wbpalmstar.plugin.ueximage.widget.SwipeLayout;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImagePreviewActivity extends Activity {
    private final String TAG = "ImagePreviewActivity";

    private ViewPager viewPager;
    private String folderName;

    private ImageView ivGoBack;
    private TextView tvTitle;
    private Button btnFinishInTitle;
    private CheckBox cbChoose;
    private TextView tvCheckbox;
    private List<String> checkedItems;
    private UEXImageUtil uexImageUtil;

    private ImageView imageView;

    private List<PictureInfo> picList;
    private int picIndex;
    private boolean isOpenBrowser;
    //仅在浏览图片时有用。
    private TextView tvShare;
    private TextView tvToGrid;
    private ResoureFinder finder;
    private RelativeLayout rlTitle;
    private RelativeLayout rlBottom;


    private AlphaAnimation fadeInAnim;
    private AlphaAnimation fadeOutAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = ResoureFinder.getInstance(this);
        setContentView(finder.getLayoutId("plugin_uex_image_activity_image_preview"));
        uexImageUtil = UEXImageUtil.getInstance();
        isOpenBrowser = EUEXImageConfig.getInstance().getIsOpenBrowser();

        rlTitle = (RelativeLayout) findViewById(finder.getId("title_layout"));
        ivGoBack = (ImageView) findViewById(finder.getId("iv_left_on_title"));
        tvTitle = (TextView) findViewById(finder.getId("tv_title"));
        btnFinishInTitle = (Button) findViewById(finder.getId("btn_finish_title"));
        viewPager = (ViewPager) findViewById(finder.getId("vp_picture"));
        cbChoose = (CheckBox) findViewById(finder.getId("checkbox"));
        rlBottom = (RelativeLayout) findViewById(finder.getId("rl_bottom"));
        rlTitle.setAlpha(0.9f);
        rlBottom.setAlpha(0.9f);

        initData();
        if (isOpenBrowser) {
            initViewForBrowser();
        } else {
            initViewForPicker();
        }
        initAnimation();

    }
    private void initData() {
        if (isOpenBrowser) {
            JSONArray imageDataArray  = EUEXImageConfig.getInstance().getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
            picIndex = EUEXImageConfig.getInstance().getStartIndex();
        } else {
            folderName = getIntent().getExtras().getString(Constants.EXTRA_FOLDER_NAME);
            picIndex = getIntent().getExtras().getInt(Constants.EXTRA_PIC_INDEX);
            checkedItems = uexImageUtil.getCheckedItems();
            picList = uexImageUtil.getCurrentPicList();
        }
    }





    private void initViewForPicker() {
        tvTitle.setText(folderName);
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (checkedItems.size() > 0) {
            btnFinishInTitle.setText("完成(" + checkedItems.size() + "/" +  EUEXImageConfig.getInstance().getMaxImageCount() + ")");
            btnFinishInTitle.setEnabled(true);
        }
        cbChoose.setTag(picList.get(picIndex).getSrc());
        cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
        btnFinishInTitle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (checkedItems.size() >=  EUEXImageConfig.getInstance().getMinImageCount()) {
                    setResult(RESULT_OK, null);
                    finish();
                } else {
                    String str = String.format(finder.getString("plugin_uex_image_at_least_choose"),  EUEXImageConfig.getInstance().getMinImageCount());
                    Toast.makeText(ImagePreviewActivity.this, str, Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
    private void initViewForBrowser() {
        tvShare = (TextView) findViewById(finder.getId("tv_share"));
        tvToGrid = (TextView) findViewById(finder.getId("tv_to_grid"));

        ivGoBack.setVisibility(View.INVISIBLE);
        tvCheckbox = (TextView) findViewById(finder.getId("tv_checkbox"));
        cbChoose.setVisibility(View.INVISIBLE);
        tvCheckbox.setVisibility(View.INVISIBLE);
        tvToGrid.setVisibility(View.VISIBLE);
        tvToGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ImagePreviewActivity.this, PictureGridActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                finish();
            }
        });

        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (EUEXImageConfig.getInstance().isDisplayActionButton()) {
            tvShare.setVisibility(View.VISIBLE);
            tvShare.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    final String src = picList.get(picIndex).getSrc();
                    Bitmap bitmap;
                    if (src.substring(0, 4).equalsIgnoreCase(Constants.HTTP)) {
                        bitmap = ImageLoader.getInstance().loadImageSync(src);
                    } else {
                        bitmap = CommonUtil.getLocalImage(ImagePreviewActivity.this, src);
                    }
                    File file = new File(Environment.getExternalStorageDirectory(),
                            File.separator + UEXImageUtil.TEMP_PATH + File.separator + "uex_image_to_share.jpg");
                    if (bitmap == null) {
                        Toast.makeText(ImagePreviewActivity.this, "当前图片尚未加载完毕，请稍后重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (CommonUtil.saveBitmap2File(bitmap, file)) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        shareIntent.setType("image/*");
                        startActivity(Intent.createChooser(shareIntent, "分享到"));
                    } else {
                        Toast.makeText(ImagePreviewActivity.this, "图片操作失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (!EUEXImageConfig.getInstance().isEnableGrid()) {
                ((View)tvShare.getParent()).setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isOpenBrowser) {
            cbChoose.setChecked(checkedItems.contains(picList.get(picIndex)));
        }
        if(1==picList.size()){
        	tvTitle.setText( "1" + "/" + picList.size());
        }else{
        	tvTitle.setText((picIndex + 1) + "/" + picList.size());
        }
    }

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
        public Object instantiateItem(ViewGroup container, int position) {
            View view = getLayoutInflater().inflate(finder.getLayoutId("plugin_uex_image_swipe_layout"), null);
            SwipeLayout swipeLayout = (SwipeLayout) view.findViewById(finder.getId("swipeLayout"));
            swipeLayout.setLeftSwipeEnabled(false);
            swipeLayout.setRightSwipeEnabled(false);
            swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
            LinearLayout ll = (LinearLayout) swipeLayout.findViewById(finder.getId("ll_photoView"));
            final PhotoView imageView = new PhotoView(ImagePreviewActivity.this);
            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(layoutParams);
            imageView.enable();
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //显示图片的配置
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .considerExifParams(true)//考虑Exif旋转
                    .build();
            String src = picList.get(position).getSrc();
            ImageLoader.getInstance().displayImage(getRealImageUrl(src), imageView, options, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {
                    BDebug.i("onLoadingStarted");
                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                    BDebug.i("onLoadingFailed");
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    BDebug.i("onLoadingComplete",s);
                    BDebug.i(imageView.getWidth(),imageView.getHeight());
                }

                @Override
                public void onLoadingCancelled(String s, View view) {
                    BDebug.i("onLoadingCancelled");
                }
            });
            imageView.setOnClickListener(imageClickListener);

            ll.addView(imageView);
            LinearLayout detail = (LinearLayout) swipeLayout.findViewById(finder.getId("ll_image_detail"));
            try {
                String path = getRealImageUrl(src);
                if(path.startsWith("file:///")) {
                    path = path.replace("file://", "");
                }
                ExifInterface exif = new ExifInterface(path);
                if (exif != null) {
                    if (!TextUtils.isEmpty(exif.getAttribute(ExifInterface.TAG_MAKE)) && !TextUtils.isEmpty(exif.getAttribute(ExifInterface.TAG_MODEL))) {
                        RowView deviceRowView = new RowView(ImagePreviewActivity.this, "器材", exif.getAttribute(ExifInterface.TAG_MAKE) + "/" + exif.getAttribute(ExifInterface.TAG_MODEL));
                        detail.addView(deviceRowView);
                    }
                    if (!TextUtils.isEmpty(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)) && TextUtils.isEmpty( exif.getAttribute(ExifInterface.TAG_ISO))) {
                        RowView exposureRowView = new RowView(ImagePreviewActivity.this, "曝光",
                                "曝光时间:" + exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) + ", ISO" + exif.getAttribute(ExifInterface.TAG_ISO));
                        detail.addView(exposureRowView);
                    }

                    if (!TextUtils.isEmpty(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))) {
                        RowView focusLengthRowView = new RowView(ImagePreviewActivity.this, "焦距:",
                                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
                        detail.addView(focusLengthRowView);
                        String whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
                        if (TextUtils.isEmpty(whiteBalance)) {
                            whiteBalance = "Auto";
                        }
                        RowView colorRowView = new RowView(ImagePreviewActivity.this, "色彩:", "白平衡:" + whiteBalance);
                        detail.addView(colorRowView);

                    }
                    if (!TextUtils.isEmpty(exif.getAttribute(ExifInterface.TAG_DATETIME))) {
                        RowView dateRowView = new RowView(ImagePreviewActivity.this, "时间:", exif.getAttribute(ExifInterface.TAG_DATETIME));
                        detail.addView(dateRowView);
                    }
                }
                System.out.println("EXIF:" + new Gson().toJson(exif));
            } catch (IOException e) {
                e.printStackTrace();
            }

            swipeLayout.addDrag(SwipeLayout.DragEdge.Bottom, detail);
            container.addView(view);
            return view;

        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    };
    private View.OnClickListener imageClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            toogleView();
        }
    };
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

    private String getRealImageUrl(String imgUrl){
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
                cbChoose.setChecked(checkedItems.contains(picList.get(i).getSrc()));
            }
            if(1==picList.size()){
            	tvTitle.setText( "1" + "/" + picList.size());
            }else{
            	tvTitle.setText((picIndex + 1) + "/" + picList.size());
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (!isOpenBrowser) {
                if (i == 1) {//开始滑动
                    cbChoose.setOnCheckedChangeListener(null);
                } else if ( i == 0) {//静止
                    cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
                    cbChoose.setTag(picList.get(picIndex).getSrc());
                }
            }
        }
    };
    //仅当在选择图片时才会用得上
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (!checkedItems.contains(buttonView.getTag())) {
                    if(checkedItems.size() >=  EUEXImageConfig.getInstance().getMaxImageCount()){
                        Toast.makeText(ImagePreviewActivity.this, "最多选择" +  EUEXImageConfig.getInstance().getMaxImageCount() + "张图片", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    checkedItems.add((String)(buttonView.getTag()));
                }
            } else {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
                }
            }
            if (checkedItems.size() > 0) {
                btnFinishInTitle.setText("完成(" +checkedItems.size()+ "/"+  EUEXImageConfig.getInstance().getMaxImageCount() + ")");
                btnFinishInTitle.setEnabled(true);
            } else {
                btnFinishInTitle.setText("完成");
                btnFinishInTitle.setEnabled(false);
            }
        }
    };
}
