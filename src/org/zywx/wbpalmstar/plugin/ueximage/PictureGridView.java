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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.assist.ImageScaleType;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.ace.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//以九宫格的形式显示某个文件夹下的图片列表
public class PictureGridView extends ImageBaseView {
    public static final String TAG = "PictureGridView";
    /*
     * 当打开系统图库时folderName才会有值，如果是打开图片选择器，此处图片信息将完全从系统中读。并且用户可以做选择图片的操作。
     * 如果是执行打开浏览器操作，则不会有值
     */
    private String folderPath;
    private String folderName;
    private GridView gvPictures;

    private ImageView ivGoBack;
    private TextView tvTitle;
    private UEXImageUtil uexImageUtil;
    private Button btnFinishInTitle;
    private List<PictureInfo> picList;
    private List<String> checkedItems;
    private MyAdapter adapter;
    private boolean isOpenBrowser = false;
    private ImageBaseView mPictureGridActivity = null;
    private OnClickListener finishGridListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            finish(TAG, Constants.OPERATION_CANCELLED);
        }
    };

    public PictureGridView(Context context, EUExImage eUExImage,
                           String folderName, int requestCode, ViewEvent viewEvent) {
        super(context, eUExImage, requestCode, viewEvent, TAG);
        onCreate(context, folderName);
        mPictureGridActivity = this;
    }

    private void onCreate(Context context, String folder) {
        LayoutInflater.from(context)
                .inflate(
                        EUExUtil.getResLayoutID(
                                "plugin_uex_image_activity_picture_grid"),
                        this, true);
        if (Constants.UI_STYLE_NEW == EUEXImageConfig.getInstance()
                .getUIStyle()) {
            View rootView = (View) findViewById(
                    EUExUtil.getResIdID("layout_grid_view"));
            rootView.setBackgroundColor(
                    EUEXImageConfig.getInstance().getViewGridBackground());
        }
        uexImageUtil = UEXImageUtil.getInstance();
        folderPath = folder;
        // 执行浏览操作
        if (TextUtils.isEmpty(folderPath)) {
            isOpenBrowser = true;
            JSONArray imageDataArray = EUEXImageConfig.getInstance()
                    .getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
        } else { // 执行选择操作
            folderName = new File(folderPath).getName();
            picList = CommonUtil.getPictureInfo(mContext, folderPath);
            uexImageUtil.setCurrentPicList(picList);
        }
        uexImageUtil.setCurrentPicList(picList);

        ivGoBack = (ImageView) findViewById(
                EUExUtil.getResIdID("iv_left_on_title"));
        ivGoBack.setOnClickListener(finishGridListener);
        tvTitle = (TextView) findViewById(EUExUtil.getResIdID("tv_title"));
        btnFinishInTitle = (Button) findViewById(
                EUExUtil.getResIdID("btn_finish_title"));
        gvPictures = (GridView) findViewById(
                EUExUtil.getResIdID("gv_pictures"));
        // 拖动下拉条和滑动过程中暂停加载
        // gvPictures.setOnScrollListener(new PauseOnScrollListener(
        // ImageLoader.getInstance(), true, true));
        if (isOpenBrowser) {
            initViewForBrowser(context);
        } else {
            initViewForPicker(context);
        }
        gvPictures.setSelection(picList.size());
    }

    private void initViewForPicker(final Context context) {
        tvTitle.setText(folderName);
        adapter = new MyAdapter(mContext, picList);
        gvPictures.setAdapter(adapter);
        checkedItems = uexImageUtil.getCheckedItems();
        updateBtnFinish();
        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private void initViewForBrowser(Context context) {
        if (Constants.UI_STYLE_OLD == EUEXImageConfig.getInstance()
                .getUIStyle()) {
            ivGoBack.setVisibility(View.INVISIBLE);
        } else {
            tvTitle.setText(
                    EUEXImageConfig.getInstance().getGridBrowserTitle());
            btnFinishInTitle.setVisibility(View.INVISIBLE);
        }
        adapter = new MyAdapter(context, picList);
        gvPictures.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        adapter.notifyDataSetChanged();
    }

    public class MyAdapter extends BaseAdapter {
        DisplayImageOptions options;
        List<PictureInfo> paths;
        Context contextAdapter;

        public MyAdapter(Context context, List<PictureInfo> paths) {
            this.paths = paths;
            contextAdapter = context;
            options = new DisplayImageOptions.Builder().cacheInMemory(false)
                    .cacheOnDisk(true).bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .displayer(new SimpleBitmapDisplayer())
                    .considerExifParams(true)// 考虑Exif旋转
                    .build();
            Collections.sort(paths, new Comparator<PictureInfo>() {
                @Override
                public int compare(PictureInfo lhs, PictureInfo rhs) {
                    if (lhs.getLastModified() > rhs.getLastModified()) {
                        return 1;
                    } else if (lhs.getLastModified() == rhs.getLastModified()) {
                        return 0;
                    } else if (lhs.getLastModified() < rhs.getLastModified()) {
                        return -1;
                    }
                    return 0;
                }
            });
        }

        @Override
        public int getCount() {
            return paths.size();
        }

        @Override
        public PictureInfo getItem(int i) {
            return paths.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View convertView,
                ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater
                        .inflate(
                                EUExUtil.getResLayoutID(
                                        "plugin_uex_image_item_grid_picture"),
                                null);
                viewHolder.imageView = (ImageView) convertView
                        .findViewById(EUExUtil.getResIdID("iv_item"));
                viewHolder.checkBox = (CheckBox) convertView
                        .findViewById(EUExUtil.getResIdID("checkbox"));
                // 如果是浏览图片，则没有选择的checkbox
                if (isOpenBrowser) {
                    viewHolder.checkBox.setVisibility(View.INVISIBLE);
                } else {
                    viewHolder.checkBox.setOnCheckedChangeListener(
                            onCheckedChangeListener);
                }

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final ImageView imageView = viewHolder.imageView;
            PictureInfo pictureInfo = paths.get(i);
            final ViewHolder tempViewHolder = viewHolder;
            if (!isOpenBrowser) {
                ACEImageLoader.getInstance().displayImageWithOptions(
                        pictureInfo.getSrc(), viewHolder.imageView, options,
                        loadingListener);
                viewHolder.checkBox.setTag(pictureInfo.getSrc());
                viewHolder.checkBox.setChecked(
                        checkedItems.contains(pictureInfo.getSrc()));
            } else {// 浏览图片：对于传入的图片的加载
                String url = pictureInfo.getSrc();
                if (pictureInfo.getThumb() != null) {
                    url = pictureInfo.getThumb();
                }
                if (url.substring(0, 4).equalsIgnoreCase(Constants.HTTP)) {
                    ACEImageLoader.getInstance().displayImageWithOptions(url,
                            viewHolder.imageView, options);
                } else {
                    Bitmap bitmap = CommonUtil
                            .getLocalImage(mContext, url);
                    imageView.setImageBitmap(bitmap);
                }
            }
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    picPreview(contextAdapter, i);
                }
            });

            return convertView;
        }

        private class ViewHolder {
            ImageView imageView;
            CheckBox checkBox;
        }
    }

    private void picPreview(Context context, int position) {
        EUEXImageConfig.getInstance().setStartIndex(position);
        View imagePreviewView = new ImagePreviewView(context, mEUExImage,
                folderName, position, Constants.REQUEST_IMAGE_BROWSER_FROM_GRID,
                new ViewEvent() {

                    @Override
                    public void resultCallBack(int requestCode,
                            int resultCode) {
                        if (resultCode == Constants.OPERATION_CONFIRMED) {
                            // 大图点击了完成，应直接完成选择，关闭此页面并给回调
                            finish(TAG, resultCode);
                        } else {
                            // 大图点击返回，小图界面刷新选择状态，继续选择
                            updateBtnFinish();
                            adapter.notifyDataSetChanged();
                            mPictureGridActivity.requestViewFocus();
                        }
                    }
                });
        mEUExImage.addViewToCurrentWindow(imagePreviewView,
                ImagePreviewView.TAG,
                EUEXImageConfig.getInstance().getPicPreviewFrame());
    }

    private void updateBtnFinish() {
        if (!isOpenBrowser) {
            if (checkedItems.size() > 0) {
                btnFinishInTitle.setText(
                        EUExUtil.getString("plugin_uex_image_crop_done") + "("
                                + checkedItems.size() + "/" + EUEXImageConfig
                                        .getInstance().getMaxImageCount()
                                + ")");
                // btnFinishInTitle.setEnabled(true);
            } else {
                btnFinishInTitle.setText(
                        EUExUtil.getString("plugin_uex_image_crop_done"));
                // btnFinishInTitle.setEnabled(false);
            }
        }
    }

    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            if (!isChecked) {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
                }
            } else {
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
                    checkedItems.add((String) buttonView.getTag());
                }
            }
            updateBtnFinish();
        }
    };
    SimpleImageLoadingListener loadingListener = new SimpleImageLoadingListener() {
        @Override
        public void onLoadingComplete(String imageUri, View view,
                final Bitmap bm) {
            if (TextUtils.isEmpty(imageUri)) {
                return;
            }
            // 此处加一个#eeeeee的滤镜，防止checkbox看不清
            try {
                ((ImageView) view).getDrawable().setColorFilter(
                        Color.argb(0xff, 0xee, 0xee, 0xee),
                        PorterDuff.Mode.MULTIPLY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

}
